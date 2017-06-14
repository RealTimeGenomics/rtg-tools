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

import java.util.ArrayList;

import com.rtg.vcf.VcfFilterStatistics.Stat;

/**
 */
public abstract class VcfInfoFilter extends AbstractVcfFilter {

  VcfInfoFilter(VcfFilterStatistics stats, Stat stat) {
    super(stats, stat);
  }

  /**
   * Filter on the range of an integer field
   */
  public static class MinMaxIntFilter extends VcfInfoFilter {
    private final String mField;
    private final int mMin;
    private final int mMax;

    /**
     * Create an filter on integer INFO field values.
     * The value must be missing or within range to be accepted.
     * @param stats collects statistics on number of items filtered. May be null.
     * @param stat the statistic indicator.
     * @param min the minimum value that will be accepted
     * @param max the maximum value that will be accepted
     * @param field the INFO field name
     */
    public MinMaxIntFilter(VcfFilterStatistics stats, Stat stat, int min, int max, String field) {
      super(stats, stat);
      mMin = min;
      mMax = max;
      mField = field;
    }

    @Override
    boolean acceptCondition(VcfRecord record) {
      final ArrayList<String> values = record.getInfo().get(mField);
      if (values != null && values.size() == 1) {
        try {
          final Integer value = Integer.valueOf(values.get(0));
          return !(value < mMin || value > mMax);
        } catch (NumberFormatException e) {
          throw new VcfFormatException("Expected numeric value in INFO " + mField + " at record: " + record);
        }
      }
      return true;
    }
  }
}
