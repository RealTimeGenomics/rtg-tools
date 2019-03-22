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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rtg.reader.SequencesReader;
import com.rtg.reference.Ploidy;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.util.Pair;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.PerSampleVariantStatistics;
import com.rtg.vcf.VariantStatistics;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
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
  private final boolean mVerbose;
  private final CrossoverSelector mGeneticMaps;
  private final Map<Sex, ReferenceGenome> mSexRef = new HashMap<>();
  protected boolean mAddRunInfo = true;
  protected boolean mDoStatistics = true;
  private ChildsimStatistics mStats = null;
  private boolean mHasWarnedOutOfOrder = false;
  private boolean mSeenVariants = false;

  private static final class ChildsimStatistics extends VariantStatistics {
    private static final class SampleStats extends PerSampleVariantStatistics {
      private int mFatherCrossovers = 0;
      private int mMotherCrossovers = 0;
      @Override
      protected Pair<List<String>, List<String>> getStatistics() {
        final Pair<List<String>, List<String>> stats = super.getStatistics();
        stats.getA().add("Father Crossovers");
        stats.getB().add(Integer.toString(mFatherCrossovers));
        stats.getA().add("Mother Crossovers");
        stats.getB().add(Integer.toString(mMotherCrossovers));
        return stats;
      }
    }

    ChildsimStatistics() {
      super(null);
    }

    @Override
    protected PerSampleVariantStatistics ensurePerSampleStats(String sampleName) {
      if (!mPerSampleStats.containsKey(sampleName)) {
        mPerSampleStats.put(sampleName, new SampleStats());
      }
      return mPerSampleStats.get(sampleName);
    }
  }

  /**
   * @param reference input reference data
   * @param rand random number generator
   * @param ploidy the default ploidy to use if no reference specification is present
   * @param crossovers crossover point selector implementation
   * @param verbose if true output extra information on crossover points
   * @throws IOException if there is an error reading reference genome configuration
   */
  public ChildSampleSimulator(SequencesReader reference, PortableRandom rand, ReferencePloidy ploidy, CrossoverSelector crossovers, boolean verbose) throws IOException {
    mReference = reference;
    mRandom = rand;
    mVerbose = verbose;
    for (Sex sex : EnumSet.allOf(Sex.class)) {
      mSexRef.put(sex, new ReferenceGenome(mReference, sex, ploidy));
    }
    mGeneticMaps = crossovers;
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
    mutateIndividual(vcfPopFile, vcfOutFile, new Trio(father, mother, sample, sex));
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfPopFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param trios trios specifying the relationships for each child sample to generate.
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfPopFile, File vcfOutFile, Trio... trios) throws IOException {
    final VcfHeader header = VcfUtils.getHeader(vcfPopFile);
    boolean foundGt = false;
    for (FormatField ff : header.getFormatLines()) {
      if (VcfUtils.FORMAT_GENOTYPE.equals(ff.getId())) {
        foundGt = true;
        break;
      }
    }
    if (!foundGt) {
      throw new NoTalkbackSlimException("input VCF does not contain GT information");
    }
    final GenomeRelationships ped = VcfPedigreeParser.load(header);

    final String[] children = new String[trios.length];
    for (int i = 0; i < trios.length; i++) {
      final Trio trio = trios[i];
      children[i] = trio.mChildSampleName;
      trio.prepareTrio(header, mSexRef, ped);
      log(trio.toString());
    }

    if (mDoStatistics) {
      mStats = new ChildsimStatistics();
      mStats.onlySamples(children);
    }

    if (mAddRunInfo) {
      header.addMetaInformationLine(VcfHeader.META_STRING + "SEED=" + mRandom.getSeed());
    }

    try (VcfWriter vcfOut = new VcfWriterFactory().zip(FileUtils.isGzipFilename(vcfOutFile)).addRunInfo(mAddRunInfo).make(header, vcfOutFile)) {
      for (long i = 0; i < mReference.numberSequences(); ++i) {
        mutateSequence(vcfPopFile, vcfOut, mReference.name(i), trios);
      }
    }
    if (!mSeenVariants) {
      Diagnostic.warning("No input variants! (is the VCF empty, or against an incorrect reference?)");
    }
    if (mVerbose) {
      Diagnostic.info(""); // Just to separate the statistics
    }
  }

  protected void printStatistics(OutputStream outStream) throws IOException {
    if (mDoStatistics) {
      mStats.printStatistics(outStream);
    }
  }

  private void log(String message) {
    if (mVerbose) {
      Diagnostic.info(message);
    } else {
      Diagnostic.userLog(message);
    }
  }

  private void warnOutOfOrder() {
    if (!mHasWarnedOutOfOrder) {
      Diagnostic.warning("Out of order VCF records encountered, crossover simulation may be affected.");
      mHasWarnedOutOfOrder = true;
    }
  }


  // Get the (sorted) parent recombination points for this (diploid) chromosome. There must be at least one and there may be extra
  private int[] getCrossoverPositions(ReferenceSequence refSeq, Sex sex) throws IOException {
    final int[] crossoverPoints = mGeneticMaps.getCrossoverPositions(mRandom, refSeq, sex);
    log("Chose " + crossoverPoints.length + " recombination points for " + sex + " parent on chromosome " + refSeq.name());
    return crossoverPoints;
  }


  //writes sample to given writer, returns records as list
  private void mutateSequence(File vcfPopFile, VcfWriter vcfOut, String refName, Trio... trios) throws IOException {
    final TrioSequenceState[] trioStates = new TrioSequenceState[trios.length];
    for (int i = 0; i < trios.length; i++) {
      trioStates[i] = new TrioSequenceState(trios[i], refName).invoke();
    }
    int lastPos = 0;
    try (VcfReader reader = VcfReader.openVcfReader(vcfPopFile, new RegionRestriction(refName))) {
      final VcfHeader header = vcfOut.getHeader();
      while (reader.hasNext()) {
        mSeenVariants = true;
        final VcfRecord v = reader.next();
        final int start = v.getStart();
        if (start - lastPos < 0) {
          warnOutOfOrder();
        } else {
          // Update parental haplotype states since the last variant
          for (TrioSequenceState trioState : trioStates) {
            trioState.advanceCrossovers(lastPos, start);
          }
        }

        for (TrioSequenceState trioState : trioStates) {
          final StringBuilder gt = trioState.getChildGt(header, v);
          v.setNumberOfSamples(v.getNumberOfSamples() + 1);
          for (String format : v.getFormats()) {
            final String value = VcfUtils.FORMAT_GENOTYPE.equals(format) ? gt.toString() : VcfRecord.MISSING;
            v.addFormatAndSample(format, value);
          }
        }
        vcfOut.write(v);
        if (mDoStatistics) {
          mStats.tallyVariant(vcfOut.getHeader(), v);
        }
        lastPos = start;
      }
    }
    for (TrioSequenceState trioState : trioStates) {
      trioState.logHaplotypes(lastPos);
    }
  }

  private void appendAllele(final StringBuilder gt, final int aid) {
    if (aid == -1) {
      gt.append(VcfRecord.MISSING);
    } else {
      gt.append(aid);
    }
  }

  static class Trio {
    private final String mFatherSampleName;
    private final String mMotherSampleName;
    private final String mChildSampleName;
    private final Sex mChildSex;
    private ReferenceGenome mFatherRefg;
    private ReferenceGenome mMotherRefg;
    private ReferenceGenome mChildRefg;
    private Sex mFatherSex;
    private Sex mMotherSex;
    private int mFatherSampleNum;
    private int mMotherSampleNum;

    Trio(String father, String mother, String sample, Sex sex) {
      mFatherSampleName = father;
      mMotherSampleName = mother;
      mChildSampleName = sample;
      mChildSex = sex;
    }
    public String child() {
      return mChildSampleName;
    }
    private void prepareTrio(VcfHeader header, Map<Sex, ReferenceGenome> sexRef, GenomeRelationships ped) {
      if (header.getSampleNames().contains(mChildSampleName)) {
        throw new NoTalkbackSlimException("sample '" + mChildSampleName + "' already exists");
      }
      final int fatherSampleNum = header.getSampleNames().indexOf(mFatherSampleName);
      if (fatherSampleNum == -1) {
        throw new NoTalkbackSlimException("father sample '" + mFatherSampleName + "' does not exist");
      }
      final int motherSampleNum = header.getSampleNames().indexOf(mMotherSampleName);
      if (motherSampleNum == -1) {
        throw new NoTalkbackSlimException("mother sample '" + mMotherSampleName + "' does not exist");
      }

      mFatherSex = ped.getSex(mFatherSampleName);
      mMotherSex = ped.getSex(mMotherSampleName);
      header.addSampleName(mChildSampleName);
      if (mChildSex == Sex.FEMALE || mChildSex == Sex.MALE) {
        header.addMetaInformationLine(VcfHeader.SAMPLE_STRING + "=<ID=" + mChildSampleName + ",Sex=" + mChildSex + ">");
      }
      header.addMetaInformationLine(VcfHeader.PEDIGREE_STRING + "=<Child=" + mChildSampleName + ",Mother=" + mMotherSampleName + ",Father=" + mFatherSampleName + ">");
      mFatherRefg = sexRef.get(mFatherSex);
      mMotherRefg = sexRef.get(mMotherSex);
      mChildRefg = sexRef.get(mChildSex);
      mFatherSampleNum = fatherSampleNum;
      mMotherSampleNum = motherSampleNum;
    }
    public String toString() {
      return "Father ID=" + mFatherSampleName + " Sex=" + mFatherSex + "\n"
      + "Mother ID=" + mMotherSampleName + " Sex=" + mMotherSex + "\n"
      + "Child ID=" + mChildSampleName + " Sex=" + mChildSex + "\n";
    }
  }

  private static final int[][] REFERENCE_GENOTYPES = {new int[] {}, new int[] {0}, new int[] {0, 0}};

  private class TrioSequenceState {
    private final Trio mTrio;
    private final String mRefName;
    private final ReferenceSequence mChildRefSeq;
    private final ReferenceSequence mFatherRefSeq;
    private final ReferenceSequence mMotherRefSeq;
    private final Ploidy mChildPloidy;
    private final int mFatherCount;
    private final int mMotherCount;
    private int[] mMotherCrossovers;
    private int[] mFatherCrossovers;
    private int mMotherCurrentCrossover;
    private int mFatherCurrentCrossover;
    private int mFatherHap;
    private int mMotherHap;
    private int mRegionStart = 0;
    private String mRegionType = "N";

    TrioSequenceState(Trio trio, String refName) {
      mTrio = trio;
      mRefName = refName;
      mChildRefSeq = trio.mChildRefg.sequence(refName);
      mFatherRefSeq = trio.mFatherRefg.sequence(refName);
      mMotherRefSeq = trio.mMotherRefg.sequence(refName);
      final Ploidy fatherPloidy = mFatherRefSeq.ploidy();
      final Ploidy motherPloidy = mMotherRefSeq.ploidy();
      mChildPloidy = mChildRefSeq.ploidy();
      mFatherCount = getEffectivePloidy(fatherPloidy.count());
      mMotherCount = getEffectivePloidy(motherPloidy.count());

      final String desc = "Father=" + fatherPloidy + " + Mother=" + motherPloidy + " -> Child=" + mChildPloidy;
      final int childCount = mChildPloidy.count();
      if (childCount == 1 && mFatherCount == 0 && mMotherCount == 0) {
        throw new NoTalkbackSlimException("Sequence " + mRefName + ": Illegal ploidy combination " + desc);
      }
      if (childCount == 2 && (mFatherCount == 0 || mMotherCount == 0)) {
        throw new NoTalkbackSlimException("Sequence " + mRefName + ": Illegal ploidy combination " + desc);
      }
      if (childCount > 2 || mFatherCount > 2 || mMotherCount > 2) {
        throw new NoTalkbackSlimException("Sequence " + mRefName + ": Unsupported ploidy combination" + desc);
      }
    }

    public TrioSequenceState invoke() throws IOException {
      mMotherCrossovers = mMotherCount > 1 ? getCrossoverPositions(mChildRefSeq, Sex.FEMALE) : new int[]{};
      mFatherCrossovers = mFatherCount > 1 ? getCrossoverPositions(mChildRefSeq, Sex.MALE) : new int[]{};
      mFatherHap = -1;
      mMotherHap = -1;
      if (mChildPloidy.count() > 0) {
        if (mMotherCount > 0) {
          mMotherHap = mRandom.nextInt(mMotherCount);
          log("Sequence " + mRefName + " chose initial mother haplotype " + mMotherHap);
        }
        if (mFatherCount > 0) {
          mFatherHap = mRandom.nextInt(mFatherCount);
          log("Sequence " + mRefName + " chose initial father haplotype " + mFatherHap);
        }
      }
      mMotherCurrentCrossover = 0;
      mFatherCurrentCrossover = 0;
      return this;
    }

    private void advanceCrossovers(int lastPos, int start) {
      boolean logged = false;
      while (mFatherCount > 1 && (mFatherCurrentCrossover < mFatherCrossovers.length) && between(mFatherCrossovers[mFatherCurrentCrossover], lastPos, start)) {
        if (!logged) {
          logHaplotypes(lastPos);
          logged = true;
        }
        mFatherHap = (mFatherHap + 1) % mFatherCount;
        if (mDoStatistics) {
          ((ChildsimStatistics.SampleStats) mStats.ensurePerSampleStats(mTrio.mChildSampleName)).mFatherCrossovers++;
        }
        mFatherCurrentCrossover++;
        log("Crossover on father in " + mRefName + "[" + (lastPos + 1) + "-" + (start + 1) + "], now haplotype " + mFatherHap);
        mRegionStart = start;
        mRegionType = "X";
      }
      while (mMotherCount > 1 && (mMotherCurrentCrossover < mMotherCrossovers.length) && between(mMotherCrossovers[mMotherCurrentCrossover], lastPos, start)) {
        if (!logged) {
          logHaplotypes(lastPos);
          logged = true;
        }
        mMotherHap = (mMotherHap + 1) % mMotherCount;
        if (mDoStatistics) {
          ((ChildsimStatistics.SampleStats) mStats.ensurePerSampleStats(mTrio.mChildSampleName)).mMotherCrossovers++;
        }
        mMotherCurrentCrossover++;
        log("Crossover on mother in " + mRefName + "[" + (lastPos + 1) + "-" + (start + 1) + "], now haplotype " + mMotherHap);
        mRegionStart = start;
        mRegionType = "X";
      }
    }
    
    private StringBuilder getChildGt(VcfHeader header, VcfRecord v) {
      final StringBuilder gt = new StringBuilder();
      if (mChildPloidy == Ploidy.NONE) {
        gt.append(VcfRecord.MISSING);

      } else if (mChildPloidy == Ploidy.POLYPLOID) {
        // Just copy from mother for polyploid (i.e. MT)
        gt.append(v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(mTrio.mMotherSampleNum));

      } else {

        final int[] fatherGtInt = getGt(header, v, mTrio.mFatherSampleNum, mFatherCount);
        final int[] motherGtInt = getGt(header, v, mTrio.mMotherSampleNum, mMotherCount);

        // Create child GT, choosing from each parent haplotype appropriately
        if (mChildPloidy == Ploidy.HAPLOID) { // Choose from whichever parent has most
          if (mFatherCount > mMotherCount) {
            appendAllele(gt, fatherGtInt[mFatherHap]);
          } else {
            appendAllele(gt, motherGtInt[mMotherHap]);
          }

        } else if (mChildPloidy == Ploidy.DIPLOID) { // Choose one from each parent
          appendAllele(gt, fatherGtInt[mFatherHap]);
          gt.append(VcfUtils.PHASED_SEPARATOR);
          appendAllele(gt, motherGtInt[mMotherHap]);

        } else {
          throw new UnsupportedOperationException("Unsupported ploidy: " + mChildPloidy);
        }
      }
      return gt;
    }

    // Treat polyploid as haploid
    private int getEffectivePloidy(int ploidy) {
      return ploidy == -1 ? 1 : ploidy;
    }

    private boolean between(int point, int min, int max) {
      return point > min && point <= max;
    }

    private int[] getGt(VcfHeader header, VcfRecord v, int sampleNum, int ploidy) {
      final String gtStr = v.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sampleNum);
      final int[] gtInt;
      if (VcfRecord.MISSING.equals(gtStr)) {
        gtInt = REFERENCE_GENOTYPES[ploidy];
      } else {
        gtInt = VcfUtils.splitGt(gtStr);
      }
      if (gtInt.length != ploidy) {
        throw new NoTalkbackSlimException("Genotype with incorrect ploidy for sample: " + header.getSampleNames().get(sampleNum) + " at " + v.getSequenceName() + ":" + v.getOneBasedStart() + " exp: " + ploidy + " was : " + gtInt.length);
      }
      return gtInt;
    }

    private void logHaplotypes(int lastPos) {
      Diagnostic.userLog("ChildSim\t" + mRegionType + "\t" + mTrio.mChildSampleName + "\t"
        + mRefName + "\t" + mRegionStart + "\t" + lastPos
        + "\t" + (mFatherHap == -1 ? "." : mFatherHap) + "\t" + (mMotherHap == -1 ? "." : mMotherHap));
    }
  }
}
