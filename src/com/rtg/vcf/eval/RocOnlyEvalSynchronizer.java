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

import com.rtg.util.intervals.ReferenceRanges;

/**
 * Creates only ROC files, no VCF output.
 */
class RocOnlyEvalSynchronizer extends WithRocsEvalSynchronizer {

  /**
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param twoPass true to output additional ROC curves for allele-matches
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  RocOnlyEvalSynchronizer(VariantSet variants, ReferenceRanges<String> ranges,
                          RocSortValueExtractor extractor,
                          File outdir, boolean zip, boolean slope, boolean twoPass, Set<RocFilter> rocFilters) throws IOException {
    super(variants, ranges, extractor, outdir, zip, slope, twoPass, rocFilters);
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
        addToROCContainer(tpWeight, 0, 1, false);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
      final double tpWeight = ((OrientedVariant) mCv).getWeight();
      if (tpWeight > 0) {
        addToROCContainer(tpWeight, 0, 1, true);
      }
    } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
      if (!mCv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
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
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        incrementBaselineCounts(false, true, false);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        incrementBaselineCounts(false, false, true);
      }
    }
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    // Just deal with the call side first, and let the baseline call take care of itself
    handleKnownCall();
  }

  @Override
  public void close() throws IOException {
    super.close();
  }
}
