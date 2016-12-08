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

package com.rtg.util.diagnostic;

/**
 * Collect counts and have them reported when <code>Spy.report()</code> is called.
 */
public class SpyHistogram {
  private final String mName;
  private long mCount = 0;
  private final long[] mHisto;

  /**
   * @param name used in reporting results.
   * @param length number of counters in histogram.
   */
  public SpyHistogram(final String name, final int length) {
    mName = name;
    mHisto = new long[length];
    Spy.add(this);
  }

  /**
   * Increment the histogram counter as determined by index.
   * If beyond the specified length then increment a separate counter.
   * @param index specifies counter to be incremented assumed to be &ge; 0.
   */
  public void increment(final int index) {
    if (index >= mHisto.length) {
      ++mCount;
      return;
    }
    mHisto[index]++;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(mName).append(" [").append(mHisto.length).append("] ");
    for (long aMHisto : mHisto) {
      sb.append(aMHisto).append(" ");
    }
    sb.append("...").append(mCount);
    return sb.toString();
  }
}
