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

/**
 * Creates only ROC files, no VCF output.
 */
class RocOnlyEvalSynchronizer extends WithRocsEvalSynchronizer {

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
  RocOnlyEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                          String callsSampleName, RocSortValueExtractor extractor,
                          File outdir, boolean zip, boolean slope, boolean twoPass, EnumSet<RocFilter> rocFilters) throws IOException {
    super(baseLineFile, callsFile, variants, ranges, callsSampleName, extractor, outdir, zip, slope, twoPass, rocFilters);
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
    final byte s = mCv.getStatus();
    switch (s) {
      case VariantId.STATUS_SKIPPED:
        break;
      case VariantId.STATUS_GT_MATCH:
        mCallTruePositives++;
        addToROCContainer(((OrientedVariant) mCv).getWeight(), s);
        break;
      case VariantId.STATUS_ALLELE_MATCH:
        mFalsePositivesCommonAllele++;
        addToROCContainer(((OrientedVariant) mCv).getWeight(), s);
        break;
      case VariantId.STATUS_NO_MATCH:
        mFalsePositives++;
        addToROCContainer(0, s);
        break;
      default:
        throw new RuntimeException("Unhandled variant status: " + mCv.getStatus());
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    switch (mBv.getStatus()) {
      case VariantId.STATUS_SKIPPED:
        break;
      case VariantId.STATUS_GT_MATCH:
        mBaselineTruePositives++;
        break;
      case VariantId.STATUS_ALLELE_MATCH:
        mFalseNegativesCommonAllele++;
        break;
      case VariantId.STATUS_NO_MATCH:
        mFalseNegatives++;
        break;
      default:
        throw new RuntimeException("Unhandled variant status: " + mBv.getStatus());
    }
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    // Just deal with the call side first, and let the baseline call take care of itself
    handleKnownCall();
  }

  @Override
  public void close() throws IOException {
  }
}
