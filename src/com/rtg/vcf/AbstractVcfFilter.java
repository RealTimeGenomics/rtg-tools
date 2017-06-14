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

package com.rtg.vcf;

import static com.rtg.vcf.VcfFilterStatistics.Stat;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.vcf.header.VcfHeader;

/**
 * Abstract out process of checking a condition and recording stats about it
 */
@TestClass("com.rtg.vcf.VcfFilterCliTest")
public abstract class AbstractVcfFilter implements VcfFilter {
  final VcfFilterStatistics mStatistics;
  final Stat mStat;

  AbstractVcfFilter(VcfFilterStatistics stats, Stat stat) {
    mStatistics = stats;
    mStat = stat;
  }

  @Override
  public boolean accept(VcfRecord record) {
    if (!acceptCondition(record)) {
      if (mStatistics != null) {
        mStatistics.increment(mStat);
      }
      return false;
    }
    return true;
  }

  @Override
  public void setHeader(VcfHeader header) {
  }

  /**
   * Condition to filter on
   * @param record the VCF record to filter
   * @return false if the record is unacceptable and should be filtered
   */
  abstract boolean acceptCondition(VcfRecord record);

  /**
   * Filter on QUAL field
   */
  public static class QualFilter extends AbstractVcfFilter {
    final double mMinQuality;
    final double mMaxQuality;
    QualFilter(VcfFilterStatistics stats, double minQuality, double maxQuality) {
      super(stats, Stat.QUALITY_FILTERED_COUNT);
      mMinQuality = minQuality;
      mMaxQuality = maxQuality;
    }
    @Override
    boolean acceptCondition(VcfRecord record) {
      // QUAL filtering
      final String qualityStr = record.getQuality();
      if (!VcfRecord.MISSING.equals(qualityStr)) {
        final double quality = Double.parseDouble(qualityStr);
        if (quality < mMinQuality || quality > mMaxQuality) {
          return false;
        }
      }
      return true;
    }
  }

}
