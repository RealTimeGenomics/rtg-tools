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
package com.rtg.util.arithcode;

import java.util.Arrays;

/**
 * Model where counts for each symbol are provided externally.
 */
public final class StaticModel implements DetailedModel {

  private final int mTotalCount;

  private final int[] mCounts;

  /**
   * Construct a uniform model.
   * @param counts supply frequency information for each alternative
   */
  StaticModel(final int[] counts) {
    mCounts = new int[counts.length + 1];
    int sofar = 0;
    for (int i = 1; i <= counts.length; i++) {
      sofar += counts[i - 1] + 1;
      mCounts[i] = sofar;
    }
    mTotalCount = mCounts[mCounts.length - 1];
  }

  @Override
  public int totalCount() {
    return mTotalCount;
  }

  @Override
  public int pointToSymbol(int midCount) {
    final int bs = Arrays.binarySearch(mCounts, midCount);
    if (bs >= 0) {
      return bs;
    }
    return -bs - 2;
  }

  @Override
  public void interval(int symbol, int[] result) {
    result[0] = mCounts[symbol];
    result[1] = mCounts[symbol + 1];
    result[2] = mTotalCount;
  }

  @Override
  public void encode(ArithEncoder encoder, int symbol) {
    encoder.encode(mCounts[symbol], mCounts[symbol + 1], mTotalCount);
  }

  @Override
  public int decode(ArithDecoder decoder) {
    final int cnt = decoder.getCurrentSymbolCount(mTotalCount);
    final int symbol = pointToSymbol(cnt);
    decoder.removeSymbolFromStream(mCounts[symbol], mCounts[symbol + 1], mTotalCount);
    return symbol;
  }
}
