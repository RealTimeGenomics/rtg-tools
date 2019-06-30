/*
 * Copyright (c) 2016. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rtg.reader.SequencesReader;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.simulation.SimulationUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.StatisticsVcfWriter;
import com.rtg.vcf.VariantStatistics;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfFormatException;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates a genotyped sample using population variants defined in a VCF file.
 */
public class SampleSimulator {

  private final SequencesReader mReference;
  private final PortableRandom mRandom;
  private final boolean mAllowMissingAf;
  private VariantStatistics mStats = null;
  private boolean mSeenVariants = false;
  private int mMissingAfCount;
  private int mWithAfCount;
  private final Map<Sex, ReferenceGenome> mSexRef = new HashMap<>();
  protected boolean mAddRunInfo = true;
  protected boolean mDoStatistics = true;

  /**
   * @param reference input reference data
   * @param rand random number generator
   * @param ploidy the default ploidy to use if no reference specification is present
   * @param allowMissingAf if set, treat variants without frequency annotation as having equally likely alleles
   * @throws IOException if there is an error reading reference genome configuration
   */
  SampleSimulator(SequencesReader reference, PortableRandom rand, ReferencePloidy ploidy, boolean allowMissingAf) throws IOException {
    mReference = reference;
    mRandom = rand;
    mAllowMissingAf = allowMissingAf;
    for (Sex sex : EnumSet.allOf(Sex.class)) {
      mSexRef.put(sex, new ReferenceGenome(mReference, sex, ploidy));
    }
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfPopFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param sample name to give the generated sample
   * @param sex sex of the generated sample
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfPopFile, File vcfOutFile, String sample, Sex sex) throws IOException {
    mutateIndividual(vcfPopFile, vcfOutFile, new String[] {sample}, new Sex[] {sex});
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfPopFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param samples names to give each generated sample
   * @param sexes sex of each generated sample
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfPopFile, File vcfOutFile, String[] samples, Sex[] sexes) throws IOException {
    if (samples.length != sexes.length) {
      throw new IllegalArgumentException();
    }
    final VcfHeader header = VcfUtils.getHeader(vcfPopFile);
    for (final String sample : samples) {
      if (header.getSampleIndex(sample) != -1) {
        throw new NoTalkbackSlimException("sample '" + sample + "' already exists");
      }
      header.addSampleName(sample);
    }
    mSeenVariants = false;
    mMissingAfCount = 0;
    mWithAfCount = 0;
    if (mDoStatistics) {
      mStats = new VariantStatistics(null);
      mStats.onlySamples(samples);
    }
    boolean foundGt = false;
    for (FormatField ff : header.getFormatLines()) {
      if (VcfUtils.FORMAT_GENOTYPE.equals(ff.getId())) {
        foundGt = true;
        break;
      }
    }
    if (!foundGt) {
      header.addFormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, VcfNumber.ONE, "Genotype");
    }
    for (int i = 0; i < samples.length; i++) {
      final String sample = samples[i];
      final Sex sex = sexes[i];
      if (sex == Sex.FEMALE || sex == Sex.MALE) {
        header.addMetaInformationLine(VcfHeader.SAMPLE_STRING + "=<ID=" + sample + ",Sex=" + sex + ">");
      }
    }
    if (mAddRunInfo) {
      header.addMetaInformationLine(VcfHeader.META_STRING + "SEED=" + mRandom.getSeed());
    }

    try (VcfWriter vcfOut = new StatisticsVcfWriter<>(new VcfWriterFactory().zip(FileUtils.isGzipFilename(vcfOutFile)).addRunInfo(mAddRunInfo).make(header, vcfOutFile), mStats)) {
      for (long i = 0; i < mReference.numberSequences(); ++i) {
        mutateSequence(vcfPopFile, vcfOut, mReference.name(i), sexes);
      }
    }
    if (!mSeenVariants) {
      Diagnostic.warning("No input variants (is the VCF empty, or against an incorrect reference?)");
    } else {
      if (mWithAfCount == 0 && !mAllowMissingAf) {
        Diagnostic.warning("No input variants contained allele frequency information.");
      }
      if (mMissingAfCount > 0) {
        Diagnostic.userLog(mMissingAfCount + " input records had no allele frequency information.");
      }
    }
  }

  protected void printStatistics(OutputStream outStream) throws IOException {
    if (mDoStatistics) {
      mStats.printStatistics(outStream);
    }
  }

  //writes sample to given writer, returns records as list
  private void mutateSequence(File vcfPopFile, VcfWriter vcfOut, String refName, Sex[] sexes) throws IOException {
    Diagnostic.userLog("Selecting genotypes on sequence: " + refName);
    try (final VcfReader reader = VcfReader.openVcfReader(vcfPopFile, new RegionRestriction(refName))) {
      final int[] lastVariantEnd = new int[sexes.length];
      Arrays.fill(lastVariantEnd, -1);
      while (reader.hasNext()) {
        mSeenVariants = true;
        final VcfRecord v = reader.next();
        v.addFormat(VcfUtils.FORMAT_GENOTYPE); // Ensure the record has a notion of genotype if it doesn't already - values will be filled in below
        double[] dist = null;
        for (int sample = 0; sample < sexes.length; sample++) {
          final ReferenceSequence refSeq = mSexRef.get(sexes[sample]).sequence(refName);
          final int ploidyCount = refSeq.ploidy().count() >= 0 ? refSeq.ploidy().count() : 1; //effectively treats polyploid as haploid
          final StringBuilder gt = new StringBuilder();
          if (refSeq.ploidy().count() != 0) {
            if (dist == null) {
              dist = getAlleleDistribution(v);
            }
            int variantEnd = lastVariantEnd[sample]; // Need to use separate variable while going through the ploidy loop, otherwise we won't make hom variants.
            for (int i = 0; i < ploidyCount; ++i) {
              if (gt.length() != 0) {
                gt.append(VcfUtils.PHASED_SEPARATOR);
              }
              final int alleleId;
              if (v.getStart() < lastVariantEnd[sample]) { // Do not generate overlapping variants
                alleleId = 0;
              } else {
                final double d = mRandom.nextDouble();
                alleleId = chooseAllele(dist, d);
              }
              final VariantType svType = alleleId == 0 ? null : VariantType.getSymbolicAlleleType(v.getAltCalls().get(alleleId - 1));
              if (svType != null) {
                Diagnostic.warning("Symbolic variant ignored: " + v);
                variantEnd = Math.max(variantEnd, v.getEnd());
                gt.append(0); // For now, skip symbolic alleles.
              } else {
                // Updating variantEnd even for alleleId == 0 prevents VCF representational issues with overlapping variants.
                //if (alleleId > 0) {
                variantEnd = Math.max(variantEnd, v.getEnd());
                //}
                gt.append(alleleId);
              }
            }
            lastVariantEnd[sample] = variantEnd;
          } else { //ploidy count == 0
            gt.append('.');
          }
          v.setNumberOfSamples(v.getNumberOfSamples() + 1);
          for (String format : v.getFormats()) {
            final String value = VcfUtils.FORMAT_GENOTYPE.equals(format) ? gt.toString() : VcfRecord.MISSING;
            v.addFormatAndSample(format, value);
          }
        }
        vcfOut.write(v);
      }
    }
  }

  // Get a cumulative allele distribution, ref allele is last position
  double[] getAlleleDistribution(VcfRecord v) {
    final double[] dist;
    final List<String> allFreqStr = v.getInfo().get(VcfUtils.INFO_ALLELE_FREQ);
    if (allFreqStr == null) {
      ++mMissingAfCount;
      if (mAllowMissingAf) {
        // Uniform probability for each allele
        final double[] defaultDist = new double[v.getAltCalls().size() + 1];
        Arrays.fill(defaultDist, 1.0); // All alleles are equally likely
        dist = SimulationUtils.cumulativeDistribution(defaultDist);
      } else {
        // Zero probability for the alt alleles
        dist = new double[v.getAltCalls().size() + 1];
      }
    } else {
      ++mWithAfCount;
      if (allFreqStr.size() != v.getAltCalls().size()) {
        throw new VcfFormatException("Incorrect number of AF entries for record " + v);
      }
      dist = new double[allFreqStr.size() + 1];
      double ac = 0.0;
      for (int i = 0; i < allFreqStr.size(); ++i) {
        ac += Double.parseDouble(allFreqStr.get(i));
        dist[i] = ac;
      }
      if (ac > 1.0) {
        throw new NoTalkbackSlimException("Sum of AF probabilities exceeds 1.0 for record " + v);
      }
    }
    dist[dist.length - 1] = 1.0; //the reference consumes the rest of the distribution
    return dist;
  }

  // Choose from cumulative distribution where ref is last position
  private static int chooseAllele(double[] dist, double d) {
    final int a = SimulationUtils.chooseFromCumulative(dist, d);
    return a == dist.length - 1 ? 0 : (a + 1);
  }
}
