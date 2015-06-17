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

import com.rtg.util.Utils;

/**
 * Represents a line of ROC output (i.e. a point on a ROC curve)
 */
class RocLine implements Comparable<RocLine> {
  final String mSequence;
  final int mPos;
  final double mPrimarySortValue;
  final double mWeight;
  final boolean mCorrect;

  RocLine(String sequence, int pos, double primarySortValue, double weight, boolean correct) {
    super();
    mSequence = sequence;
    mPos = pos;
    mPrimarySortValue = primarySortValue;
    mWeight = weight;
    mCorrect = correct;
  }

  @Override
  public int compareTo(RocLine other) {
    final int order = Double.compare(other.mPrimarySortValue, this.mPrimarySortValue);
    if (order != 0) {
      return order;
    }
    if (!this.mCorrect && other.mCorrect) {
      return -1;
    } else if (this.mCorrect && !other.mCorrect) {
      return 1;
    }
    final int sequence = this.mSequence.compareTo(other.mSequence);
    if (sequence != 0) {
      return sequence;
    }
    return this.mPos - other.mPos;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!other.getClass().equals(getClass())) {
      return false;
    }
    final RocLine o = (RocLine) other;
    return compareTo(o) == 0;
  }

  @Override
  public int hashCode() {
    return Utils.hash(new Object[] {mSequence, mPos, mPrimarySortValue, mCorrect});
  }

}
