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
package com.rtg.sam;

import com.rtg.util.IntegerOrPercentage;

import htsjdk.samtools.SAMRecord;

/**
 * Filtering based on a filter parameters.
 */
public class DefaultSamFilter implements SamFilter {

  // Number of bits used for quantizing subsampling fraction
  private static final int SUBSAMPLE_RESOLUTION = 16;
  private static final int SUBSAMPLE_MAX = 1 << SUBSAMPLE_RESOLUTION;
  private static final int SUBSAMPLE_MASK = SUBSAMPLE_MAX - 1;

  private final SamFilterParams mFilterParams;
  private final boolean mInvert;

  /**
   * Filter for SAM records.
   *
   * @param params filter parameters
   */
  public DefaultSamFilter(final SamFilterParams params) {
    mFilterParams = params;
    mInvert = params.invertFilters();
  }

  /**
   * Filter a SAM record based on specified filtering parameters.
   * Does not consider position based filters, or inversion status.
   *
   * @param params parameters
   * @param rec record
   * @return true if record should be accepted for processing
   */
  private static boolean checkRecordProperties(final SamFilterParams params, final SAMRecord rec) {
    final int flags = rec.getFlags();
    if ((flags & params.requireUnsetFlags()) != 0) {
      return false;
    }
    if ((flags & params.requireSetFlags()) != params.requireSetFlags()) {
      return false;
    }
    if (rec.getAlignmentStart() == 0 && params.excludeUnplaced()) {
      return false;
    }

    final boolean mated = (flags & SamBamConstants.SAM_READ_IS_MAPPED_IN_PROPER_PAIR) != 0;
    final boolean unmapped = rec.getReadUnmappedFlag();
    if (!mated && !unmapped && params.excludeUnmated()) {
      return false;
    }

    final int minMapQ = params.minMapQ();
    if (minMapQ >= 0 && rec.getMappingQuality() < minMapQ) {
      return false;
    }

    final Integer nh = SamUtils.getNHOrIH(rec);
    final int maxNH = params.maxAlignmentCount();
    if (maxNH >= 0 && nh != null && nh > maxNH) {
        return false;
    }
    if (params.excludeVariantInvalid()) {
      if (nh != null && nh == 0) {
        return false;
      }
      if (!rec.getReadUnmappedFlag() && rec.getAlignmentStart() <= 0) {
        return false;
      }
    }

    final IntegerOrPercentage maxAS = mated ? params.maxMatedAlignmentScore() : params.maxUnmatedAlignmentScore();
    if (maxAS != null) {
      final Integer as = rec.getIntegerAttribute(SamUtils.ATTRIBUTE_ALIGNMENT_SCORE);
      if (as != null && as > maxAS.getValue(rec.getReadLength())) {
        return false;
      }
    }

    if (params.subsampleFraction() != null) {
      final double sFrac;
      if (params.subsampleRampFraction() != null) { // Use subsample ramping
        if (rec.getAlignmentStart() == 0) {
          sFrac = (params.subsampleFraction() + params.subsampleRampFraction()) / 2;
        } else {
          final int pos = rec.getAlignmentStart();
          final int refLength = rec.getHeader().getSequence(rec.getReferenceIndex()).getSequenceLength();
          final double lengthFrac = Math.max(0, Math.min(1.0, (double) pos / refLength));
          sFrac = params.subsampleFraction() + lengthFrac * (params.subsampleRampFraction() - params.subsampleFraction());
        }
      } else {
        sFrac = params.subsampleFraction();
      }
      // Subsample using hash of read name, ensures pairs are kept together
      return (internalHash(rec.getReadName(), params.subsampleSeed()) & SUBSAMPLE_MASK) < sFrac * SUBSAMPLE_MAX;
    }

    return true;
  }

  private static long internalHash(final String data, final long seed) {
    long hash = 13L;
    final char[] charArray = data.toCharArray();
    for (final char c : charArray) {
      hash = hash * 31L + c;
    }
    return hash + seed;
  }

  @Override
  public boolean acceptRecord(final SAMRecord rec) {
    return mInvert ^ checkRecordProperties(mFilterParams, rec);
  }
}
