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
 * Clips sequences based on average quality in a sliding window.
 *
 */
public final class DefaultReadTrimmer implements ReadTrimmer {

  private final int mWindowSize;
  private final int mQualityThreshold;

  /**
   * Construct a read trimmer
   * @param windowSize size of the window to look over
   * @param qualityThreshold the threshold the window must average higher than
   */
  public DefaultReadTrimmer(int windowSize, int qualityThreshold) {
    mWindowSize = windowSize;
    mQualityThreshold = qualityThreshold;
  }

  @Override
  public String toString() {
    return "Default(threshold=" + mQualityThreshold + ", window=" + mWindowSize + ")";
  }

  @Override
  public int trimRead(byte[] read, byte[] qualities, int length) {
    if (qualities == null || qualities.length == 0) {
      return length;
    }
    final int[] quals = new int[mWindowSize];
    int cutoffIndex = length;
    double sum = 0.0;
    for (int i = 0; i < cutoffIndex; ++i) {
      if (i >= quals.length) {
        if (sum / quals.length < mQualityThreshold) {
          cutoffIndex = i;
        }
      }
      final int i2 = i % quals.length;
      sum -= quals[i2];
      quals[i2] = (int) qualities[i];
      sum += quals[i2];
    }
    if (cutoffIndex > mWindowSize) {
      return cutoffIndex;
    }
    return 0;
  }

}
