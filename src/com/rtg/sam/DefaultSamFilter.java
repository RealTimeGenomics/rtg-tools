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

  private final SamFilterParams mFilterParams;

  /**
   * Filter for SAM records.
   *
   * @param params filter parameters
   */
  public DefaultSamFilter(final SamFilterParams params) {
    mFilterParams = params;
  }

  /**
   * Filter a SAM record based on specified filtering parameters. Does not consider position based filters.
   *
   * @param params parameters
   * @param rec record
   * @return true if record should be accepted for processing
   */
  public static boolean acceptRecord(final SamFilterParams params, final SAMRecord rec) {
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
    if (params.excludeUnmated() && !mated && !unmapped) {
      return false;
    }

    final int minMapQ = params.minMapQ();
    if (minMapQ >= 0 && rec.getMappingQuality() < minMapQ) {
      return false;
    }

    final int maxNH = params.maxAlignmentCount();
    if (maxNH >= 0) {
      final Integer nh = SamUtils.getNHOrIH(rec);
      if (nh != null && nh > maxNH) {
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
    return true;
  }


  @Override
  public boolean acceptRecord(final SAMRecord rec) {
    return acceptRecord(mFilterParams, rec);
  }
}
