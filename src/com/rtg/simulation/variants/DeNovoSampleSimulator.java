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
import java.util.LinkedList;
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
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.variant.GenomePriorParams;
import com.rtg.vcf.VariantStatistics;
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
 * Creates a genotyped sample as the same as an input sample with the addition of de novo mutations.
 *
 */
public class DeNovoSampleSimulator {

  private final SequencesReader mReference;
  private final PortableRandom mRandom;
  private final ReferencePloidy mDefaultPloidy;
  private final PriorPopulationVariantGenerator mGenerator;
  private VariantStatistics mStats = null;
  private int mOriginalSampleNum = -1;
  private Sex[] mOriginalSexes = null;
  private ReferenceGenome mOriginalRefg = null;
  private ReferenceGenome mMaleGenome = null;
  private ReferenceGenome mFemaleGenome = null;
  private boolean mVerbose = true;
  private boolean mSeenVariants = false;

  /**
   * @param reference input reference data
   * @param params genome params
   * @param rand random number generator
   * @param ploidy the default ploidy to use if no reference specification is present
   * @param targetMutations expected number of mutations per genome
   * @param verbose if true output extra information on crossover points
   * @throws IOException if an I/O error occurs.
   */
  public DeNovoSampleSimulator(SequencesReader reference, GenomePriorParams params, PortableRandom rand, ReferencePloidy ploidy, final int targetMutations, boolean verbose) throws IOException {
    mReference = reference;
    mRandom = rand;
    mDefaultPloidy = ploidy;
    mVerbose = verbose;
    mGenerator = new PriorPopulationVariantGenerator(reference, new PopulationMutatorPriors(params), rand, new PriorPopulationVariantGenerator.FixedAlleleFrequencyChooser(1.0), targetMutations);
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfInFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param origSample name of the father sample
   * @param sample name to give the generated sample
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfInFile, File vcfOutFile, String origSample, String sample) throws IOException {
    final VcfHeader header = VcfUtils.getHeader(vcfInFile);
    if (header.getSampleNames().contains(sample)) {
      throw new NoTalkbackSlimException("sample '" + sample + "' already exists");
    }
    mOriginalSampleNum = header.getSampleNames().indexOf(origSample);
    if (mOriginalSampleNum == -1) {
      throw new NoTalkbackSlimException("original sample '" + origSample + "' does not exist");
    }

    final GenomeRelationships ped = VcfPedigreeParser.load(header);
    mOriginalSexes = new Sex[ped.genomes().length];
    for (String genome : header.getSampleNames()) {
      mOriginalSexes[header.getSampleNames().indexOf(genome)] = ped.getSex(genome);
    }
    final Sex originalSex = ped.getSex(origSample);
    mOriginalRefg = new ReferenceGenome(mReference, originalSex, mDefaultPloidy);
    mMaleGenome = new ReferenceGenome(mReference, Sex.MALE, mDefaultPloidy);
    mFemaleGenome = new ReferenceGenome(mReference, Sex.FEMALE, mDefaultPloidy);

    boolean foundGt = false;
    boolean foundDeNovo = false;
    for (FormatField ff : header.getFormatLines()) {
      if (VcfUtils.FORMAT_GENOTYPE.equals(ff.getId())) {
        foundGt = true;
      } else if (VcfUtils.FORMAT_DENOVO.equals(ff.getId())) {
        foundDeNovo = true;
      }
    }
    if (!foundGt) {
      throw new NoTalkbackSlimException("input VCF does not contain GT information");
    }
    if (!foundDeNovo) {
      header.addFormatField(VcfUtils.FORMAT_DENOVO, MetaType.STRING, VcfNumber.ONE, "De novo allele");
    }
    if (mVerbose) {
      Diagnostic.info("Original ID=" + origSample + " Sex=" + originalSex);
    }

    header.addSampleName(sample);
    if (originalSex == Sex.FEMALE || originalSex == Sex.MALE) {
      header.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + sample + ",Sex=" + originalSex.toString() + ">");
    }
    header.addLine(VcfHeader.PEDIGREE_STRING + "=<Derived=" + sample + ",Original=" + origSample + ">");

    header.addRunInfo();
    header.addLine(VcfHeader.META_STRING + "SEED=" + mRandom.getSeed());

    mStats = new VariantStatistics(null);
    mStats.onlySamples(sample);
    try (VcfWriter vcfOut = new VcfWriterFactory().zip(FileUtils.isGzipFilename(vcfOutFile)).make(header, vcfOutFile)) {
      final ReferenceGenome refG = new ReferenceGenome(mReference, originalSex, mDefaultPloidy);

      // Generate de novo variants (oblivious of any pre-existing variants)
      final List<PopulationVariantGenerator.PopulationVariant> deNovo = mGenerator.generatePopulation();

      for (long i = 0; i < mReference.numberSequences(); ++i) {
        final ReferenceSequence refSeq = refG.sequence(mReference.name(i));

        final List<PopulationVariantGenerator.PopulationVariant> seqDeNovo = new LinkedList<>();
        for (PopulationVariantGenerator.PopulationVariant v : deNovo) {
          if (v.getSequenceId() == i) {
            seqDeNovo.add(v);
          }
        }

        outputSequence(vcfInFile, vcfOut, refSeq, seqDeNovo);
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

  // Treat polyploid as haploid
  private int getEffectivePloidy(int ploidy) {
    return ploidy == -1 ? 1 : ploidy;
  }

  //writes sample to given writer, returns records as list
  private List<VcfRecord> outputSequence(File vcfPopFile, VcfWriter vcfOut, ReferenceSequence refSeq, List<PopulationVariantGenerator.PopulationVariant> deNovo) throws IOException {
    final Ploidy ploidy = mOriginalRefg.sequence(refSeq.name()).ploidy();
    final int ploidyCount = getEffectivePloidy(ploidy.count());
    final int sampleId = vcfOut.getHeader().getNumberOfSamples() - 1;

    final String desc = "Original=" + ploidy + " -> Derived=" + ploidy;
    if (ploidyCount > 2) {
      throw new NoTalkbackSlimException("Sequence " + refSeq.name() + ": Unsupported ploidy" + desc);
    }
    if ((ploidyCount == 0) && (deNovo.size() > 0)) {
      Diagnostic.developerLog("Ignoring " + deNovo.size() + " deNovo variants generated for chromosome " + refSeq.name() + " with ploidy NONE");
      deNovo.clear();
    }
    //System.err.println("Sequence " + refSeq.name() + " has ploidy " + desc);
    final ArrayList<VcfRecord> sequenceVariants = new ArrayList<>();
    try (VcfReader reader = VcfReader.openVcfReader(vcfPopFile, new RegionRestriction(refSeq.name()))) {
      while (reader.hasNext()) {
        mSeenVariants = true;
        final VcfRecord v = reader.next();
        final int nextVariantPos = v.getStart();

        outputDeNovo(refSeq, deNovo, nextVariantPos, ploidyCount, sampleId, vcfOut, sequenceVariants);

        // For non de-novo, append sample genotype from original genotype
        v.setNumberOfSamples(sampleId + 1);
        final String gt = v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(mOriginalSampleNum);
        for (String format : v.getFormats()) {
          final String value = VcfUtils.FORMAT_GENOTYPE.equals(format) ? gt : VcfRecord.MISSING;
          v.addFormatAndSample(format, value);
        }

        sequenceVariants.add(v);
        vcfOut.write(v);
        mStats.tallyVariant(vcfOut.getHeader(), v);
      }

      // Output any remaining de novo variants
      outputDeNovo(refSeq, deNovo, Integer.MAX_VALUE, ploidyCount, sampleId, vcfOut, sequenceVariants);

    }
    return sequenceVariants;
  }

  private void outputDeNovo(ReferenceSequence refSeq, List<PopulationVariantGenerator.PopulationVariant> deNovo, final int endPos, final int ploidyCount, final int sampleId, VcfWriter vcfOut, final ArrayList<VcfRecord> sequenceVariants) throws IOException {
    // Merge input and de novo, requires records to be sorted
    while (!deNovo.isEmpty() && (deNovo.get(0).getStart() <= endPos)) {
      final PopulationVariantGenerator.PopulationVariant pv = deNovo.remove(0);
      final VcfRecord dv = pv.toVcfRecord(mReference);
      if (dv.getStart() == endPos) {
        // We could be smarter and merge the records, but this will be so infrequent (assuming we're not re-using the same seed as a previous run) let's just warn.
        Diagnostic.warning("Skipping De Novo mutation at " + refSeq.name() + ":" + dv.getOneBasedStart() + " to avoid collision with existing variant (consider a new RNG seed)");
        continue;
      }
      dv.getInfo().clear(); // To remove AF chosen by the population variant generator
      if (mVerbose) {
        Diagnostic.info("De Novo mutation at " + refSeq.name() + ":" + dv.getOneBasedStart());
      }
      addSamplesForDeNovo(dv, sampleId, ploidyCount, refSeq.name());

      sequenceVariants.add(dv);
      vcfOut.write(dv);
      mStats.tallyVariant(vcfOut.getHeader(), dv);
    }
  }


  private String addSamplesForDeNovo(final VcfRecord v, final int sampleId, final int ploidyCount, String refName) {
    final String sampleGt = ploidyCount == 1 ? "1" : mRandom.nextBoolean() ? "0|1" : "1|0";
    v.setNumberOfSamples(sampleId + 1);
    for (int id = 0; id <= sampleId; ++id) {
      final String gt;
      if (id != sampleId) {
        final ReferenceGenome sampleGenome = mOriginalSexes[id] == Sex.MALE ? mMaleGenome : mFemaleGenome;
        final Ploidy ploidy = sampleGenome.sequence(refName).ploidy();
        switch (ploidy) {
          case HAPLOID:
            gt = "0";
            break;
          case DIPLOID:
            gt = "0|0";
            break;
          default:
            gt = ".";
            break;
        }
      } else {
        gt = sampleGt;
      }

      v.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gt);
      v.addFormatAndSample(VcfUtils.FORMAT_DENOVO, (id == sampleId) ? "Y" : "N");
    }
    return sampleGt;
  }

}
