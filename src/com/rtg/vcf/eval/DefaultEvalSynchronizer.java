/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;

/**
 * Creates typical vcfeval output files with separate VCF files and ROC files.
 */
class DefaultEvalSynchronizer extends MergingEvalSynchronizer {

  private static final String FN_FILE_NAME = "fn.vcf";
  private static final String FP_FILE_NAME = "fp.vcf";
  private static final String TP_FILE_NAME = "tp.vcf";
  private static final String TPBASE_FILE_NAME = "tp-baseline.vcf";

  private final VcfWriter mTpCalls;
  private final VcfWriter mTpBase;
  private final VcfWriter mFp;
  private final VcfWriter mFn;
  private final RocContainer mRoc;
  private final RocSortValueExtractor mRocExtractor;
  private final int mCallSampleNo;
  private final boolean mZip;
  private final boolean mSlope;
  private final File mOutDir;
  private int mBaselineTruePositives = 0;
  int mCallTruePositives = 0;
  int mFalseNegatives = 0;
  int mFalsePositives = 0;
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param callsSampleName the name of the sample used in the calls
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param outputTpBase true if the baseline true positive file should be written
   * @param slope true to output ROC slope files
   * @param rtgStats true to output additional ROC curves for RTG specific attributes
   * @throws IOException if there is a problem opening output files
   */
  DefaultEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                          String callsSampleName, RocSortValueExtractor extractor,
                          File outdir, boolean zip, boolean outputTpBase, boolean slope, boolean rtgStats) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);
    final RocContainer roc = new RocContainer(extractor.getSortOrder(), extractor.toString());
    roc.addStandardFilters();
    if (rtgStats) {
      roc.addExtraFilters();
    }
    mRoc = roc;
    mRocExtractor = extractor;
    mZip = zip;
    mSlope = slope;
    mOutDir = outdir;
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mTpCalls = new VcfWriter(variants.calledHeader(), new File(outdir, TP_FILE_NAME + zipExt), null, zip, true);
    mTpBase = outputTpBase ? new VcfWriter(variants.baseLineHeader(), new File(outdir, TPBASE_FILE_NAME + zipExt), null, zip, true) : null;
    mFp = new VcfWriter(variants.calledHeader(), new File(outdir, FP_FILE_NAME + zipExt), null, zip, true);
    mFn = new VcfWriter(variants.baseLineHeader(), new File(outdir, FN_FILE_NAME + zipExt), null, zip, true);
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");
  }

  @Override
  protected void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    mMisPhasings += misPhasings;
    mUnphasable += unphasable;
    mCorrectPhasings += correctPhasings;
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
    // No-op
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
    // No-op
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    // No-op
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    // No-op
  }

  @Override
  protected void handleKnownCall() throws IOException {
    if (mCv instanceof OrientedVariant) { // Included (but the baseline was at a different position. This is interesting)
      //mCrv.addInfo("STATUS", "C-TP-BDiff=" + mCv.toString());
      mCallTruePositives++;
      mTpCalls.write(mCrv);
      addToROCContainer(((OrientedVariant) mCv).getWeight());
    } else if (mCv instanceof SkippedVariant) { // Too-hard
      // No-op
    } else { // Excluded (FP or self-inconsistent)
      mFalsePositives++;
      mFp.write(mCrv);
      addToROCContainer(0);
    }
  }

  private void addToROCContainer(double weight) {
    final EnumSet<RocFilter> filters = EnumSet.noneOf(RocFilter.class);
    for (final RocFilter filter : RocFilter.values()) {
      if (filter.accept(mCrv, mCallSampleNo)) {
        filters.add(filter);
      }
    }
    double score = Double.NaN;
    try {
      score = mRocExtractor.getSortValue(mCrv, mCallSampleNo);
    } catch (IndexOutOfBoundsException ignored) {
    }
    mRoc.addRocLine(score, weight, filters);
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    if (mBv instanceof OrientedVariant) { // Included but the baseline was at a different position. This is interesting
      //mBrv.addInfo("STATUS", "C-TP-BDiff=" + mCv.toString());
      mBaselineTruePositives++;
      if (mTpBase != null) {
        mTpBase.write(mBrv);
      }
    } else if (mCv instanceof SkippedVariant) { // Too-hard, output this variant with just the used alleles
      // No-op
    } else { // Excluded (novel or self-inconsistent)
      mFalseNegatives++;
      mFn.write(mBrv);
    }
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    // Just deal with the call side first, and let the baseline call take care of itself
    handleKnownCall();
  }

  int getUnphasable() {
    return mUnphasable;
  }

  int getMisPhasings() {
    return mMisPhasings;
  }

  int getCorrectPhasings() {
    return mCorrectPhasings;
  }


  @Override
  void finish() throws IOException {
    super.finish();
    mRoc.writeRocs(mOutDir, mBaselineTruePositives, mFalsePositives, mFalseNegatives, mZip, mSlope);
    writePhasingInfo();
    mRoc.writeSummary(new File(mOutDir, CommonFlags.SUMMARY_FILE), mBaselineTruePositives, mFalsePositives, mFalseNegatives);
  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mTpBase;
         VcfWriter ignored2 = mTpCalls;
         VcfWriter ignored3 = mFn;
         VcfWriter ignored4 = mFp) {
      // done for nice closing side effects
    }
  }
}
