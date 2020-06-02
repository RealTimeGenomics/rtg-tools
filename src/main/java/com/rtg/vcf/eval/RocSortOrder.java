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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Enumeration of possible ROC sort orders
 */
public enum RocSortOrder {

  /**
   * ROC is ordered assuming lowest sort value (best) to highest value (worst)
   */
  ASCENDING(new AscendingDoubleComparator()),

  /**
   * ROC is ordered assuming highest sort value (best) to lowest value (worst)
   */
  DESCENDING(new DescendingDoubleComparator());

  private final Comparator<Double> mComparator;

  RocSortOrder(Comparator<Double> comparator) {
    mComparator = comparator;
  }

  /**
   * @return a comparator appropriate for testing values for this sort order
   */
  public Comparator<Double> comparator() {
    return mComparator;
  }

  // Need to manually ensure that Double.NaN looks smaller than every other number, so that it comes last on the ROC
  private static class DescendingDoubleComparator implements Comparator<Double>, Serializable {
    @Override
    public int compare(Double o1, Double o2) {
      if (Double.isNaN(o1)) {
        return Double.isNaN(o2) ? 0 : 1;
      } else if (Double.isNaN(o2)) {
        return -1;
      }
      return o2.compareTo(o1);
    }

    public String toString() {
      return ">";
    }
  }

  private static class AscendingDoubleComparator implements Comparator<Double>, Serializable {
    @Override
    public int compare(Double o1, Double o2) {
      return o1.compareTo(o2);
    }

    public String toString() {
      return "<";
    }
  }
}
