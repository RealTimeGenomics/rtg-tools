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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfUtils;

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
  protected int mBaselineTruePositives = 0;
  protected int mCallTruePositives = 0;
  protected int mFalseNegatives = 0;
  protected int mFalsePositives = 0;
  protected int mFalseNegativesCommonAllele = 0;
  protected int mFalsePositivesCommonAllele = 0;
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
   * @param slope true to output ROC slope files
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  WithRocsEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                           String callsSampleName, RocSortValueExtractor extractor,
                           File outdir, boolean zip, boolean slope, boolean dualRocs, Set<RocFilter> rocFilters) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);

    int callSampleNo;
    Set<RocFilter> filters;
    try {
      callSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");
      filters = rocFilters;
    } catch (NoTalkbackSlimException e) {
      filters = new HashSet<>(rocFilters);
      if (extractor.requiresSample()) {
        Diagnostic.info("During ALT comparison no ROC data will be produced, as a sample is required by the selected ROC score field: " + extractor);
      } else {
        filters.removeIf(RocFilter::requiresGt);
        if (filters.size() != rocFilters.size()) {
          final Set<RocFilter> excluded = new HashSet<>();
          excluded.addAll(rocFilters.stream().filter(RocFilter::requiresGt).collect(Collectors.toList()));
          Diagnostic.info("During ALT comparison some ROC data files will not be produced: " + excluded + ", producing ROC data for: " + filters);
        }
      }
      callSampleNo = -1;
    }
    mCallSampleNo = callSampleNo;
    if (mCallSampleNo >= 0 || !extractor.requiresSample()) {
      mDefaultRoc = new RocContainer(extractor);
      mDefaultRoc.addFilters(filters);
      if (dualRocs) {
        mAlleleRoc = new RocContainer(extractor, "allele_");
        mAlleleRoc.addFilters(filters);
      } else {
        mAlleleRoc = null;
      }
    } else {
      mDefaultRoc = null;
      mAlleleRoc = null;
    }
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

  protected void addToROCContainer(double tpWeight, double fpWeight, double tpqWeight, boolean alleleMatch) {
    if (mDefaultRoc != null) {
      if (alleleMatch) { // Consider these FP for GT ROCs
        mDefaultRoc.addRocLine(mCrv, mCallSampleNo, fpWeight, tpWeight, 0); // XXXLen 0 vs ???
      } else {
        mDefaultRoc.addRocLine(mCrv, mCallSampleNo, tpWeight, fpWeight, tpqWeight);
      }
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
    if (mDefaultRoc != null) {
      mDefaultRoc.missingScoreWarning();
    }
    writePhasingInfo();
    if (mAlleleRoc != null) {
      final int alleleTp = mBaselineTruePositives + mFalseNegativesCommonAllele;
      final int alleleTpq = mCallTruePositives + mFalsePositivesCommonAllele;
      mAlleleRoc.writeRocs(mOutDir, alleleTp, mFalsePositives, mFalseNegatives, alleleTpq, mZip, mSlope);
      // Do we want the allele-level summary too?
      //mAlleleRoc.writeSummary(mOutDir, alleleTp, mFalsePositives, mFalseNegatives);
    }
    if (mDefaultRoc != null) {
      final int strictFp = mFalsePositives + mFalsePositivesCommonAllele;
      final int strictFn = mFalseNegatives + mFalseNegativesCommonAllele;
      mDefaultRoc.writeRocs(mOutDir, mBaselineTruePositives, strictFp, strictFn, mCallTruePositives, mZip, mSlope);
      mDefaultRoc.writeSummary(mOutDir, mBaselineTruePositives, strictFn, mCallTruePositives, strictFp);
    }
  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }
}
