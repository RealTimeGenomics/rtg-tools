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
import java.util.Set;

import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;

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
   * @param variants the set of variants to evaluate
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param twoPass true to output additional ROC curves for allele-matches
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  SplitEvalSynchronizer(VariantSet variants,
                        RocSortValueExtractor extractor,
                        File outdir, boolean zip, boolean slope, boolean twoPass, Set<RocFilter> rocFilters) throws IOException {
    super(variants, extractor, outdir, zip, slope, twoPass, rocFilters);
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfWriterFactory vf = new VcfWriterFactory().zip(zip).addRunInfo(true);
    mTpCalls = vf.make(variants.calledHeader(), new File(outdir, TP_FILE_NAME + zipExt));
    mTpBase = vf.make(variants.baselineHeader(), new File(outdir, TPBASE_FILE_NAME + zipExt));
    mFp = vf.make(variants.calledHeader(), new File(outdir, FP_FILE_NAME + zipExt));
    mFn = vf.make(variants.baselineHeader(), new File(outdir, FN_FILE_NAME + zipExt));
    if (twoPass) {
      mFpCa = vf.make(variants.calledHeader(), new File(outdir, FP_CA_FILE_NAME + zipExt));
      mFnCa = vf.make(variants.baselineHeader(), new File(outdir, FN_CA_FILE_NAME + zipExt));
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
        mTpCalls.write(mCrv);
        addToROCContainer(tpWeight, 0, 1, false);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
      final double tpWeight = ((OrientedVariant) mCv).getWeight();
      if (tpWeight > 0) {
        (mFpCa != null ? mFpCa : mFp).write(mCrv);
        addToROCContainer(tpWeight, 0, 1, true);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
      if (!mCv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        mFp.write(mCrv);
        addToROCContainer(0, 1, 0, false);
      } else {
        ++mCallOutside;
      }
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    if (!mBv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
      if (mBv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        incrementBaselineCounts(true, false, false);
        mTpBase.write(mBrv);
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        incrementBaselineCounts(false, true, false);
        (mFnCa != null ? mFnCa : mFn).write(mBrv);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        incrementBaselineCounts(false, false, true);
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
    // Try-with-resources for nice closing side effects
    try (VcfWriter ignored = mTpBase;
         VcfWriter ignored2 = mTpCalls;
         VcfWriter ignored3 = mFn;
         VcfWriter ignored4 = mFp;
         VcfWriter ignored5 = mFnCa;
         VcfWriter ignored6 = mFpCa) {
      super.close();
    }
  }
}
