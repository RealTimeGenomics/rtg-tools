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

package com.rtg.reader;

/**
 * Computes a position to trim a read at via an algorithm very similar to:
 *
 * <code>argmax_x{\sum_{i=x+1}^l(INT-q_i)}</code> if <code>q_l&lt;INT</code> where l is the original read length.
 *
 */
public final class BestSumReadTrimmer implements ReadTrimmer {

  private final int mQualityThreshold;
  private final boolean mStart;

  /**
   * Construct a best sum read trimmer
   * @param qualityThreshold the threshold the sum the remainder of the read must be higher than
   */
  public BestSumReadTrimmer(int qualityThreshold) {
    this(qualityThreshold, false);
  }

  /**
   * Construct a best sum read trimmer for a specific direction
   * @param qualityThreshold the threshold the sum the remainder of the read must be higher than
   * @param start if true, trim from the start of the read, else trim from the end
   */
  public BestSumReadTrimmer(int qualityThreshold, boolean start) {
    mQualityThreshold = qualityThreshold;
    mStart = start;
  }

  @Override
  public String toString() {
    return "BestSum(threshold=" + mQualityThreshold + ",side=" + (mStart ? "start" : "end") + ")";
  }

  @Override
  public int trimRead(byte[] read, byte[] qualities, int length) {
    if (qualities == null || qualities.length == 0) {
      return length;
    }
    if (length > qualities.length) {
      throw new IllegalArgumentException("Declared read length is longer than supplied qualities array");
    }
    return mStart ? trimStart(read, qualities, length) : trimEnd(qualities, length);
  }

  private int trimStart(byte[] read, byte[] qualities, int length) {
    if (length == 0 || qualities[0] >= mQualityThreshold) {
      return length;
    }
    int bestPos = 0;
    int bestSum = 0;
    int sum = 0;
    for (int i = 0; i < length; ++i) {
      sum += mQualityThreshold - qualities[i];
      if (sum >= bestSum) {
        bestSum = sum;
        bestPos = i + 1;
      }
    }
    final int newLength = length - bestPos;
    if (newLength > 0) {
      System.arraycopy(read, bestPos, read, 0, newLength);
      System.arraycopy(qualities, bestPos, qualities, 0, newLength);
    }

    return newLength;
  }

  private int trimEnd(byte[] qualities, int length) {
    int bestPos = length;
    if (length > 0 && qualities[length - 1] < mQualityThreshold) {
      int bestSum = 0;
      int sum = 0;
      for (int i = length - 1; i >= 0; --i) {
        sum += mQualityThreshold - qualities[i];
        if (sum >= bestSum) {
          bestSum = sum;
          bestPos = i;
        }
      }
    }
    return bestPos;
  }
}
