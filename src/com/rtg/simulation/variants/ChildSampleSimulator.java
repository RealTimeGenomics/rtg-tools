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
import java.util.ArrayList;
import java.util.List;

import com.rtg.reader.SequencesReader;
import com.rtg.reference.Ploidy;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.DefaultVcfWriter;
import com.rtg.vcf.VariantStatistics;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Creates a genotyped sample as a child of two parents defined in a VCF file.
 * Some information regarding crossover from Francisco De La Vega:

  <pre>

   - Crossovers are obligatory for proper segregation of chromosomes during
   meiosis. Almost always at least one crossover happens for each chromosome.
   When this doesn't happen this creates aneuplodies which can be seen in rare
   genetic diseases or some not seen because they lead to miscarriages.

   - Both sister chromatids exchange with their homologous chromosomes.
   Again, this is needed for proper segregation, but the 2-D diagrams simplify
   this fact. Please look at Figure 3 of the review of Cromie and Smith for a
   more realistic representation.

   - Based on data I have seen usually you observe one cross-over per
   chromosome in humans. Occasionally you may have two. The rate of this is
   not very well documented. The crossover machinery exerts a zone of
   interference preventing other crossovers to occur nearby. In yeast you can
   observe other crossovers but they are far apart of each other. I would just
   put a very low probability for more than one crossover per chromosome (may
   be happening one in a hundred chromosomes).

   - Rarely there is a non-reciprocal donation of a piece of DNA from one
   homologous chromosome to the other. This means one chromosome gains a
   little piece of DNA (may be from a few kb to a few tens of kb) and the
   other losses this. This is very rare but can be observed - may be once in
   1,000-10,000 meiosis. The rate of this is not documented in humans and an
   area of research so this is my best guess, but you may not need to model
   this in your simulations.

  </pre>

  This code does not currently model crossover in PAR regions of sex chromosomes,
  although strictly speaking, it should.  Regarding sex chromosomes and crossover
  in PAR regions, Wikipedia says:

  <pre>

  Pairing (synapsis) of the X and Y chromosomes and crossing over (recombination)
  between their pseudoautosomal regions appear to be necessary for the normal
  progression of male meiosis. Thus, those cells in which X-Y recombination
  does not occur will fail to complete meiosis. Structural and/or genetic
  dissimilarity (due to hybridization or mutation) between the pseudoautosomal
  regions of the X and Y chromosomes can disrupt pairing and recombination, and
  consequently cause male infertility.

  </pre>
  This implies that the sex chromosomes must have 2 (maybe rarely 4) crossovers,
  within the PAR region, to ensure that non PAR regions do not get exchanged.

 *
 */
public class ChildSampleSimulator {

  private final SequencesReader mReference;
  private final PortableRandom mRandom;
  private final ReferencePloidy mDefaultPloidy;
  private ChildStatistics mStats = null;
  private int mFatherSampleNum = -1;
  private int mMotherSampleNum = -1;
  private ReferenceGenome mMotherRefg = null;
  private ReferenceGenome mFatherRefg = null;
  private boolean mHasWarnedOutOfOrder = false;
  private double mExtraCrossoverFreq = 0;
  private boolean mVerbose = true;
  private boolean mSeenVariants = false;

  private static final class ChildStatistics extends VariantStatistics {

    private int mFatherCrossovers = 0;
    private int mMotherCrossovers = 0;

    ChildStatistics() {
      super(null);
    }

    @Override
    public String getStatistics() {
      final StringBuilder out = new StringBuilder();
      out.append(super.getStatistics());
      if (mFatherCrossovers > 0) {
        out.append("Father Crossovers            : ").append(mFatherCrossovers).append(StringUtils.LS);
      }
      if (mMotherCrossovers > 0) {
        out.append("Mother Crossovers            : ").append(mMotherCrossovers).append(StringUtils.LS);
      }
      return out.toString();
    }
  }

  /**
   * @param reference input reference data
   * @param rand random number generator
   * @param ploidy the default ploidy to use if no reference specification is present
   * @param extraCrossovers expected number of extra crossovers per chromosome
   * @param verbose if true output extra information on crossover points
   */
  public ChildSampleSimulator(SequencesReader reference, PortableRandom rand, ReferencePloidy ploidy, double extraCrossovers, boolean verbose) {
    mReference = reference;
    mRandom = rand;
    mDefaultPloidy = ploidy;
    mExtraCrossoverFreq = extraCrossovers;
    mVerbose = verbose;
  }

  private static VcfHeader getVcfHeader(File vcfFile) throws IOException {
    try (VcfReader reader = VcfReader.openVcfReader(vcfFile)) {
      return reader.getHeader();
    }
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfPopFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param sample name to give the generated sample
   * @param sex sex of the generated sample
   * @param father name of the father sample
   * @param mother name of the mother sample
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfPopFile, File vcfOutFile, String sample, Sex sex, String father, String mother) throws IOException {
    final VcfHeader header = getVcfHeader(vcfPopFile);
    if (header.getSampleNames().contains(sample)) {
      throw new NoTalkbackSlimException("sample '" + sample + "' already exists");
    }
    mFatherSampleNum = header.getSampleNames().indexOf(father);
    if (mFatherSampleNum == -1) {
      throw new NoTalkbackSlimException("father sample '" + father + "' does not exist");
    }
    mMotherSampleNum = header.getSampleNames().indexOf(mother);
    if (mMotherSampleNum == -1) {
      throw new NoTalkbackSlimException("mother sample '" + mother + "' does not exist");
    }

    final GenomeRelationships ped = VcfPedigreeParser.load(header);
    final Sex fatherSex = ped.getSex(father);
    mFatherRefg = new ReferenceGenome(mReference, fatherSex, mDefaultPloidy);
    final Sex motherSex = ped.getSex(mother);
    mMotherRefg = new ReferenceGenome(mReference, motherSex, mDefaultPloidy);

    boolean foundGt = false;
    for (FormatField ff : header.getFormatLines()) {
      if (ff.getId().equals(VcfUtils.FORMAT_GENOTYPE)) {
        foundGt = true;
        break;
      }
    }
    if (!foundGt) {
      throw new NoTalkbackSlimException("input VCF does not contain GT information");
    }

    if (mVerbose) {
      Diagnostic.info("Father ID=" + father + " Sex=" + fatherSex);
      Diagnostic.info("Mother ID=" + mother + " Sex=" + motherSex);
    }

    header.addSampleName(sample);
    if (sex == Sex.FEMALE || sex == Sex.MALE) {
      header.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + sample + ",Sex=" + sex.toString() + ">");
    }
    header.addLine(VcfHeader.PEDIGREE_STRING + "=<Child=" + sample + ",Mother=" + mother + ",Father=" + father + ">");

    header.addRunInfo();
    header.addLine(VcfHeader.META_STRING + "SEED=" + mRandom.getSeed());

    mStats = new ChildStatistics();
    mStats.onlySamples(sample);
    try (VcfWriter vcfOut = new DefaultVcfWriter(header, vcfOutFile, null, FileUtils.isGzipFilename(vcfOutFile), true)) {
      final ReferenceGenome refG = new ReferenceGenome(mReference, sex, mDefaultPloidy);
      for (long i = 0; i < mReference.numberSequences(); ++i) {
        final ReferenceSequence refSeq = refG.sequence(mReference.name(i));
        mutateSequence(vcfPopFile, vcfOut, refSeq, mReference.length(i));
      }
    }
    if (!mSeenVariants) {
      Diagnostic.warning("No input variants! (is the VCF empty, or against an incorrect reference?)");
    }
    if (mVerbose) {
      Diagnostic.info(""); // Just to separate the statistics
    }
    Diagnostic.info(mStats.getStatistics());
  }

  private void warnOutOfOrder() {
    if (!mHasWarnedOutOfOrder) {
      Diagnostic.warning("Out of order VCF records encountered, crossover simulation may be affected.");
      mHasWarnedOutOfOrder = true;
    }
  }

  // Treat polyploid as haploid
  private int getEffectivePloidy(int ploidy) {
    return ploidy == -1 ? 1 : ploidy;
  }

  private static final int[][] REFERENCE_GENOTYPES = {new int[] {}, new int[] {0}, new int[] {0, 0}};

  // Convert string genotype to allele ids, promoting empty value to reference of expected ploidy
  private int[] getGt(String gtStr, int ploidy) {
    if (gtStr.equals(VcfRecord.MISSING)) {
      return REFERENCE_GENOTYPES[ploidy];
    } else {
      return VcfUtils.splitGt(gtStr);
    }
  }

  private boolean between(int point, int min, int max) {
    return point > min && point <= max;
  }

  //writes sample to given writer, returns records as list
  private List<VcfRecord> mutateSequence(File vcfPopFile, VcfWriter vcfOut, ReferenceSequence refSeq, int seqLength) throws IOException {
    final Ploidy fatherPloidy = mFatherRefg.sequence(refSeq.name()).ploidy();
    final Ploidy motherPloidy = mMotherRefg.sequence(refSeq.name()).ploidy();
    final Ploidy childPloidy = refSeq.ploidy();
    final int fatherCount = getEffectivePloidy(fatherPloidy.count());
    final int motherCount = getEffectivePloidy(motherPloidy.count());
    final int childCount = childPloidy.count();
    final int motherRequiredCrossover = mRandom.nextInt(seqLength);  // Position of the one required crossover per (diploid) chromosome. There may be extra
    final int fatherRequiredCrossover = mRandom.nextInt(seqLength);  // Position of the one required crossover per (diploid) chromosome. There may be extra
    final double crossoverPerBase = mExtraCrossoverFreq / seqLength; // Chance of getting an additional crossover per base

    final String desc = "Father=" + fatherPloidy + " + Mother=" + motherPloidy + " -> Child=" + childPloidy;
    if (childCount == 1 && fatherCount == 0 && motherCount == 0) {
      throw new NoTalkbackSlimException("Sequence " + refSeq.name() + ": Illegal ploidy combination " + desc);
    }
    if (childCount == 2 && (fatherCount == 0 || motherCount == 0)) {
      throw new NoTalkbackSlimException("Sequence " + refSeq.name() + ": Illegal ploidy combination " + desc);
    }
    if (childCount > 2 || fatherCount > 2 || motherCount > 2) {
      throw new NoTalkbackSlimException("Sequence " + refSeq.name() + ": Unsupported ploidy combination" + desc);
    }
    //System.err.println("Sequence " + refSeq.name() + " has ploidy " + desc);
    int fatherHap = 0;
    int motherHap = 0;
    if (childCount > 0) {
      if (motherCount > 1) {
        motherHap = mRandom.nextInt(motherCount);
        if (mVerbose) {
          Diagnostic.info("Sequence " + refSeq.name() + " chose initial mother haplotype " + motherHap);
        }
      }
      if (fatherCount > 1) {
        fatherHap = mRandom.nextInt(fatherCount);
        if (mVerbose) {
          Diagnostic.info("Sequence " + refSeq.name() + " chose initial father haplotype " + fatherHap);
        }
      }
    }
    final ArrayList<VcfRecord> sequenceMutations = new ArrayList<>();
    int lastPos = 0;
    try (VcfReader reader = VcfReader.openVcfReader(vcfPopFile, new RegionRestriction(refSeq.name()))) {
      while (reader.hasNext()) {
        mSeenVariants = true;
        final VcfRecord v = reader.next();
        // Simulate crossover, assume records are sorted
        final int baseDelta = v.getStart() - lastPos;
        if (baseDelta < 0) {
          warnOutOfOrder();
        } else {
          if (fatherCount > 1 && (between(fatherRequiredCrossover, lastPos, v.getStart()) || mRandom.nextDouble() <= baseDelta * crossoverPerBase)) {
            fatherHap = (fatherHap + 1) % fatherCount;
            mStats.mFatherCrossovers++;
            if (mVerbose) {
              Diagnostic.info("Crossover on father in " + refSeq.name() + "[" + (lastPos + 1) + "-" + v.getOneBasedStart() + "], now haplotype " + fatherHap);
            }
          }
          if (motherCount > 1 && (between(motherRequiredCrossover, lastPos, v.getStart()) || mRandom.nextDouble() <= baseDelta * crossoverPerBase)) {
            motherHap = (motherHap + 1) % motherCount;
            mStats.mMotherCrossovers++;
            if (mVerbose) {
              Diagnostic.info("Crossover on mother in " + refSeq.name() + "[" + (lastPos + 1) + "-" + v.getOneBasedStart() + "], now haplotype " + motherHap);
            }
          }
        }
        lastPos = v.getStart();

        final StringBuilder gt = new StringBuilder();
        if (childPloidy == Ploidy.NONE) {
          gt.append(VcfRecord.MISSING);

        } else if (childPloidy == Ploidy.POLYPLOID) {
          // Just copy from mother for polyploid (i.e. MT)
          gt.append(v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(mMotherSampleNum));

        } else {
          // Get father GT
          final int[] fatherGtInt = getGt(v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(mFatherSampleNum), fatherCount);
          if (fatherGtInt.length != fatherCount) {
            throw new NoTalkbackSlimException("Genotype with incorrect ploidy for sample: " + vcfOut.getHeader().getSampleNames().get(mFatherSampleNum) + " at " + refSeq.name() + ":" + v.getOneBasedStart() + " exp: " + fatherCount + " was : " + fatherGtInt.length);
          }
          // Get mother GT
          final int[] motherGtInt = getGt(v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(mMotherSampleNum), motherCount);
          if (motherGtInt.length != motherCount) {
            throw new NoTalkbackSlimException("Genotype with incorrect ploidy for sample: " + vcfOut.getHeader().getSampleNames().get(mMotherSampleNum) + " at " + refSeq.name() + ":" + v.getOneBasedStart() + " exp: " + motherCount + " was : " + motherGtInt.length);
          }

          // Create child GT, choosing from each parent haplotype appropriately
          if (childPloidy == Ploidy.HAPLOID) { // Choose from whichever parent has most
            if (fatherCount > motherCount) {
              appendAllele(gt, fatherGtInt[fatherHap]);
            } else {
              appendAllele(gt, motherGtInt[motherHap]);
            }

          } else if (childPloidy == Ploidy.DIPLOID) { // Choose one from each parent
            appendAllele(gt, fatherGtInt[fatherHap]);
            gt.append(VcfUtils.PHASED_SEPARATOR);
            appendAllele(gt, motherGtInt[motherHap]);

          } else {
            throw new UnsupportedOperationException("Unsupported ploidy: " + childPloidy);
          }
        }
        v.setNumberOfSamples(v.getNumberOfSamples() + 1);
        for (String format : v.getFormats()) {
          final String value = format.equals(VcfUtils.FORMAT_GENOTYPE) ? gt.toString() : VcfRecord.MISSING;
          v.addFormatAndSample(format, value);
        }
        sequenceMutations.add(v);
        vcfOut.write(v);
        mStats.tallyVariant(vcfOut.getHeader(), v);

      }
    }
    return sequenceMutations;
  }

  private void appendAllele(final StringBuilder gt, final int aid) {
    if (aid == -1) {
      gt.append(VcfRecord.MISSING);
    } else {
      gt.append(aid);
    }
  }

}
