/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.util;

import static com.rtg.util.StringUtils.LS;

import java.util.Arrays;

/**
 * Class for holding an expandable histogram of both positive and negative values..
 */
public class HistogramWithNegatives {

  private long[] mHistogram = new long[0];

  private int mMin = Integer.MAX_VALUE;

  /**
   * Increment the value at position by 1.
   * @param position the zero based position to increment
   */
  public void increment(int position) {
    increment(position, 1);
  }

  /**
   * Increment the value at position by value.
   * @param position the zero based position to increment
   * @param value the value to increment by
   */
  public void increment(int position, long value) {
    assert value > 0;

    if (mMin == Integer.MAX_VALUE) {
      mHistogram = new long[1];
      mMin = position;
    } else {
      final int index = position - mMin;
      if (index >= mHistogram.length) {
        mHistogram = Arrays.copyOf(mHistogram, index + 1);
      } else if (index < 0) {
        final long[] newa = new long[mHistogram.length - index];
        System.arraycopy(mHistogram, 0, newa, -index, mHistogram.length);
        mHistogram = newa;
        mMin = position;
      }
    }
    mHistogram[position - mMin] += value;
  }

  /**
   * @return the number of entries stored in the histogram.
   */
  public int length() {
    return mHistogram.length;
  }

  /**
   * @return one greater than the maximum position with a non-zero entry in the histogram
   */
  public int max() {
    return length() + min();
  }

  /**
   * @return the minimum position with a non-zero entry in the histogram.
   */
  public int min() {
    return mMin;
  }

  /**
   * Get the value at the given position.
   * @param position the zero based position
   * @return the value at the given position
   */
  public long getValue(int position) {
    return mHistogram[position - mMin];
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (mMin == Integer.MAX_VALUE) {
      sb.append("[]").append(LS);
    } else {
      sb.append("[").append(mMin).append("..").append(mMin + mHistogram.length - 1).append("]").append(LS);
      for (final long i : mHistogram) {
        sb.append(i).append(" ");
      }
      sb.append(LS);
    }
    return sb.toString();
  }
}
