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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

/**
 * Support output modes that include overall statistics and ROC files.
 */
@TestClass("com.rtg.vcf.eval.SplitEvalSynchronizerTest")
abstract class WithRocsEvalSynchronizer extends InterleavingEvalSynchronizer {

  private final RocContainer mDefaultRoc;
  private final RocContainer mAlleleRoc;
  private final boolean mZip;
  private final boolean mSlope;
  private final File mOutDir;
  protected final int mCallSampleNo;
  protected final int mBaselineSampleNo;
  protected int mCallOutside = 0;
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;

  /**
   * @param variants the set of variants to evaluate
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @param rocCriteria criteria for selecting a favoured ROC point
   */
  WithRocsEvalSynchronizer(VariantSet variants,
                           RocSortValueExtractor extractor,
                           File outdir, boolean zip, boolean slope, boolean dualRocs, Set<RocFilter> rocFilters, RocPointCriteria rocCriteria) {
    super(variants);

    mBaselineSampleNo = variants.baselineSample();
    mCallSampleNo = variants.calledSample();

    final Set<RocFilter> filters = new LinkedHashSet<>(rocFilters);
    if (mCallSampleNo == -1 && extractor.requiresSample()) {
      Diagnostic.warning("During ALT comparison no ROC data will be produced, as a sample is required by the selected ROC score field: " + extractor);
    } else if (mCallSampleNo == -1 || mBaselineSampleNo == -1) {
      filters.removeIf(RocFilter::requiresGt);
      if (extractor != RocSortValueExtractor.NULL_EXTRACTOR && filters.size() != rocFilters.size()) {
        final Set<RocFilter> excluded = new LinkedHashSet<>(rocFilters.stream().filter(RocFilter::requiresGt).collect(Collectors.toList()));
        Diagnostic.warning("During ALT comparison some ROC data files will not be produced: " + excluded + ", producing ROC data for: " + filters);
      }
    }
    if (mCallSampleNo == -1 && extractor.requiresSample()) {
      mDefaultRoc = new RocContainer(RocSortValueExtractor.NULL_EXTRACTOR);
      mDefaultRoc.addFilter(RocFilter.ALL);
      mDefaultRoc.setRocPointCriteria(rocCriteria);
      mAlleleRoc = null;
    } else {
      mDefaultRoc = new RocContainer(extractor);
      mDefaultRoc.addFilters(filters);
      mDefaultRoc.setRocPointCriteria(rocCriteria);
      if (dualRocs) {
        mAlleleRoc = new RocContainer(extractor, "allele_");
        mAlleleRoc.addFilters(filters);
        mAlleleRoc.setRocPointCriteria(rocCriteria);
      } else {
        mAlleleRoc = null;
      }
    }
    // Inform the roc container of the header. (mAlleleRoc, if present, uses same filters and extractor)
    // Use the calls header, may affect some more complex JS expression based filters/scores.
    mDefaultRoc.setHeader(variants.calledHeader());
    mZip = zip;
    mSlope = slope;
    mOutDir = outdir;
  }

  @Override
  protected void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    mMisPhasings += misPhasings;
    mUnphasable += unphasable;
    mCorrectPhasings += correctPhasings;
  }

  // Used to accumulate totals
  protected void incrementBaselineCounts(boolean tp, boolean alleleMatch, boolean fn) {
    if (!(tp || alleleMatch || fn)) {
      throw new IllegalArgumentException();
    }
    mDefaultRoc.incrementBaselineCount(mBrv, mBaselineSampleNo, tp);
    if (mAlleleRoc != null) {
      mAlleleRoc.incrementBaselineCount(mBrv, mBaselineSampleNo, tp || alleleMatch);
    }
  }

  protected void addToROCContainer(double tpWeight, double fpWeight, double tpqWeight, boolean alleleMatch) {
    if (alleleMatch) { // Consider these FP for GT ROCs
      mDefaultRoc.addRocLine(mCrv, mCallSampleNo, 0, 1, 0);
    } else {
      mDefaultRoc.addRocLine(mCrv, mCallSampleNo, tpWeight, fpWeight, tpqWeight);
    }
    if (mAlleleRoc != null) {
      mAlleleRoc.addRocLine(mCrv, mCallSampleNo, tpWeight, fpWeight, tpqWeight);
    }
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
    mDefaultRoc.missingScoreWarning();
    if (mCallOutside > 0) {
      final RocPoint<?> total = mDefaultRoc.getTotal(RocFilter.ALL);
      final int callTotal = (int) Math.round(total.getRawTruePositives() + total.getFalsePositives() + mCallOutside);
      Diagnostic.userLog("Fraction of calls outside evaluation regions: " + Utils.realFormat((double) mCallOutside / callTotal, 4) + " (" + mCallOutside + "/" + callTotal + ")");
    }
    writePhasingInfo();
    if (mAlleleRoc != null) {
      mAlleleRoc.writeRocs(mOutDir, mZip, mSlope);
      // Do we want the allele-level summary too?
      //mAlleleRoc.writeSummary(mOutDir, alleleTp, mFalsePositives, mFalseNegatives);
    }
    if (mDefaultRoc.isRocEnabled()) {
      mDefaultRoc.writeRocs(mOutDir, mZip, mSlope);
    }
    mDefaultRoc.writeSummary(mOutDir);
  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }
}
