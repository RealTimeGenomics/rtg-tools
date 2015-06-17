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

/**
 * A class representing a range of non-negative longs as a zero-based half-open interval.
 */
public class LongRange {

  /** Indicates either the start or end is missing (usually meaning the range should be extended as far as possible) */
  public static final long MISSING = -1; // This should probably actually be MIN or MAX, as it interferes with the ability to use LongRange for negative intervals, but currently too much depends on hardcoded -1s.

  /** A singleton that treats all locations as in range. */
  public static final LongRange NONE = new LongRange(MISSING, MISSING) {  // Long.MIN_VALUE, Long.MAX_VALUE) {
    @Override
    public boolean isInRange(final long value) {
      return true;
    }
    @Override
    public String toString() {
      return "[All inclusive]";
    }
  };

  private final long mStart;
  private final long mEnd;

  /**
   * @param start 0 based start position
   * @param end 0 based end position exclusive
   */
  public LongRange(long start, long end) {
    if (end != MISSING && start != MISSING && end < start) {
      throw new IllegalArgumentException("Locus start must be less than or equal to end. start=" + start + ", end=" + end);
      // When start == end, this denotes an insertion point between two bases
    }
    mStart = start;
    mEnd = end;
  }

  /**
   * Nonnegative length of this interval.
   *
   * @return length
   */
  public long getLength() {
    return getEnd() - getStart();
  }

  /**
   * Zero-based start of the range.
   * @return start of the range
   */
  public long getStart() {
    return mStart;
  }

  /**
   * Zero-based end of the range (exclusive).
   * @return end of the range
   */
  public long getEnd() {
    return mEnd;
  }

  /**
   * @param value the query position
   * @return true if the query position is within the range region.
   */
  public boolean isInRange(final long value) {
    return value >= mStart && value < mEnd;
  }

  @Override
  public String toString() {
    return (getStart() + 1) + "-" + getEnd(); // Output as 1-based with end inclusive, like RegionRestriction
  }

}
