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
package com.rtg.util.intervals;

import java.util.Arrays;

/**
 * Finite width interval containing status values. This is backed by an array so memory issue may be a concern for large intervals.
 */
public class StatusInterval {

  /** The byte used to represent absence from the interval */
  public static final byte EMPTY = 0; // If you change this, the c'tor will need an explicit fill.

  private final byte[] mInterval;
  private final int mStart;

  /**
   * Construct a new interval spanning a given region.
   * @param start left edge of interval
   * @param end exclusive right edge of interval
   */
  public StatusInterval(final int start, final int end) {
    if (end <= start) {
      throw new IllegalArgumentException();
    }
    mStart = start;
    mInterval = new byte[end - start];
  }

  /**
   * Add points to the interval. Regions outside the overall interval are ignored.
   * @param start start position
   * @param end end position, exclusive
   * @param status the status to associate with points in the interval.
   */
  public void add(final int start, final int end, final byte status) {
    final int s = start - mStart;
    if (s < mInterval.length && end > mStart) {
      Arrays.fill(mInterval, Math.max(s, 0), Math.min(end - mStart, mInterval.length), status);
    }
  }

  /**
   * Test if given point is has a non-empty status in this interval.
   * @param pos position to test
   * @return true if point is in the interval
   */
  public boolean contains(final int pos) {
    final int index = pos - mStart;
    return mInterval[index] != EMPTY;
  }

  /**
   * Get the status value at a given point is contained in this interval.
   * @param pos position to test
   * @return the value stored in the interval. EMPTY is used for positions not present.
   */
  public byte get(final int pos) {
    final int index = pos - mStart;
    return mInterval[index];
  }
}
