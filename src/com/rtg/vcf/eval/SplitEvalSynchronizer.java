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

import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfWriter;

/**
 * Creates typical vcfeval output files with separate VCF files and ROC files.
 */
class SplitEvalSynchronizer extends WithRocsEvalSynchronizer {

  private static final String FN_FILE_NAME = "fn.vcf";
  private static final String FP_FILE_NAME = "fp.vcf";
  private static final String TP_FILE_NAME = "tp.vcf";
  private static final String TPBASE_FILE_NAME = "tp-baseline.vcf";
  private static final String FN_CA_FILE_NAME = "fn-ca.vcf";
  private static final String FP_CA_FILE_NAME = "fp-ca.vcf";

  private final VcfWriter mTpCalls;
  private final VcfWriter mTpBase;
  private final VcfWriter mFp;
  private final VcfWriter mFn;
  private final VcfWriter mFpCa;
  private final VcfWriter mFnCa;

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
   * @param twoPass true to output additional ROC curves for allele-matches
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  SplitEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                        String callsSampleName, RocSortValueExtractor extractor,
                        File outdir, boolean zip, boolean slope, boolean twoPass, EnumSet<RocFilter> rocFilters) throws IOException {
    super(baseLineFile, callsFile, variants, ranges, callsSampleName, extractor, outdir, zip, slope, twoPass, rocFilters);
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mTpCalls = makeVcfWriter(variants.calledHeader(), new File(outdir, TP_FILE_NAME + zipExt), zip);
    mTpBase = makeVcfWriter(variants.baseLineHeader(), new File(outdir, TPBASE_FILE_NAME + zipExt), zip);
    mFp = makeVcfWriter(variants.calledHeader(), new File(outdir, FP_FILE_NAME + zipExt), zip);
    mFn = makeVcfWriter(variants.baseLineHeader(), new File(outdir, FN_FILE_NAME + zipExt), zip);
    if (twoPass) {
      mFpCa = makeVcfWriter(variants.calledHeader(), new File(outdir, FP_CA_FILE_NAME + zipExt), zip);
      mFnCa = makeVcfWriter(variants.baseLineHeader(), new File(outdir, FN_CA_FILE_NAME + zipExt), zip);
    } else {
      mFpCa = null;
      mFnCa = null;
    }
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
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    // Drop both the unknown records
    if (unknownBaseline) {
      mBrv = null;
    }
    if (unknownCall) {
      mCrv = null;
    }
  }

  @Override
  protected void handleKnownCall() throws IOException {
    if (mCv.hasStatus(VariantId.STATUS_GT_MATCH)) {
      final double tpWeight = ((OrientedVariant) mCv).getWeight();
      if (tpWeight > 0) {
        mCallTruePositives++;
        mTpCalls.write(mCrv);
        addToROCContainer(tpWeight, 0, false);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
      final double tpWeight = ((OrientedVariant) mCv).getWeight();
      if (tpWeight > 0) {
        mFalsePositivesCommonAllele++;
        (mFpCa != null ? mFpCa : mFp).write(mCrv);
        addToROCContainer(tpWeight, 0, true);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
      if (!mCv.hasStatus(VariantId.STATUS_LOW_CONF)) {
        mFalsePositives++;
        mFp.write(mCrv);
        addToROCContainer(0, 1, false);
      }
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    if (!mBv.hasStatus(VariantId.STATUS_LOW_CONF)) {
      if (mBv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        mBaselineTruePositives++;
        mTpBase.write(mBrv);
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        mFalseNegativesCommonAllele++;
        (mFnCa != null ? mFnCa : mFn).write(mBrv);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        mFalseNegatives++;
        mFn.write(mBrv);
      }
    }
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    // Just deal with the call side first, and let the baseline call take care of itself
    handleKnownCall();
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mTpBase;
         VcfWriter ignored2 = mTpCalls;
         VcfWriter ignored3 = mFn;
         VcfWriter ignored4 = mFp;
         VcfWriter ignored5 = mFnCa;
         VcfWriter ignored6 = mFpCa) {
      // done for nice closing side effects
    }
  }
}
