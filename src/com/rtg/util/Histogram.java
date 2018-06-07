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

package com.rtg.util;

import static com.rtg.util.StringUtils.LS;

import java.util.Arrays;

/**
 * Class for holding an expandable histogram.
 */
public class Histogram {

  private long[] mHistogram = new long[100];
  private int mLength = 0;

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
    assert value >= 0;
    mLength = Math.max(mLength, position + 1);
    if (position >= mHistogram.length) {
      mHistogram = Arrays.copyOf(mHistogram, position + position / 3);
    }
    mHistogram[position] += value;
  }

  /**
   * Get the length of the histogram.
   * @return the length of the histogram
   */
  public int getLength() {
    return mLength;
  }

  /**
   * Get the value at the given position.
   * @param position the zero based position
   * @return the value at the given position
   */
  public long getValue(int position) {
    return mHistogram[position];
  }

  /**
   * @return one greater than the maximum position with a non-zero entry in the histogram
   */
  public int max() {
    return mLength;
  }

  /**
   * @return the minimum position with a non-zero entry in the histogram.
   */
  public int min() {
    return 0;
  }

  /**
   * @return the sum of all values
   */
  public long sum() {
    long tot = 0;
    for (int i = min(); i < max(); ++i) {
      tot += getValue(i);
    }
    return tot;
  }

  /**
   * Get the value at the given position, returning
   * 0 for values greater than the length of the histogram.
   * @param position the zero based position
   * @return the value at the given position
   */
  public long getValueUnbounded(int position) {
    return position >= max() ? 0 : position < min() ? 0 : getValue(position);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < getLength(); ++i) {
      final long count = mHistogram[i];
      sb.append(count).append("\t");
    }
    return sb.toString().trim();
  }

  /**
   * Output the table as basic tab separated values, one line per entry.
   * @param includeZeros if true, include rows containing zero counts.
   * @param columnTitles optional titles for each column. If omitted, no header line is produced.
   * @return the <code>TSV</code> representation
   */
  public String getAsTsv(boolean includeZeros, String... columnTitles) {
    assert columnTitles.length <= 2;
    final StringBuilder sb = new StringBuilder();
    if (columnTitles.length > 0) {
      sb.append("#").append(columnTitles[0]).append('\t').append(columnTitles.length > 1 ? columnTitles[1] : "count").append(LS);
    }
    for (int i = 0; i < getLength(); ++i) {
      final long count = mHistogram[i];
      if (includeZeros || count > 0) {
        sb.append(i).append('\t').append(count).append(LS);
      }
    }
    return sb.toString();
  }

  /**
   * Parse a zero based histogram from tab separated single-line string.
   * @param histStr string to parse and add
   */
  public void addHistogram(String histStr) {
    if (histStr.length() > 0) {
      final String[] values = histStr.split("\t");
      for (int i = values.length - 1; i >= 0; --i) {
        final long val = Long.parseLong(values[i]);
        if (val > 0) {
          increment(i, val);
        }
      }
    }
  }

  /**
   * Merges the contents of another histogram into this one.
   * @param other the other histogram
   */
  public void addHistogram(Histogram other) {
    for (int i = 0; i < other.getLength(); ++i) {
      increment(i, other.getValue(i));
    }
  }

  /**
   * Create a distribution from current histogram
   * @return the distribution
   */
  public double[] toDistribution() {
    final double[] ret = new double[getLength()];
    final long tot = sum();
    for (int i = min(); i < max(); ++i) {
      ret[i] = (double) getValue(i) / tot;
    }
    return ret;
  }

}
