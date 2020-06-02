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

/**
 * A singleton uniform distribution byte model.  Provides a single
 * static member that is a non-adaptive model assigning equal
 * likelihood to all 256 bytes.
 */
public final class UniformModel implements DetailedModel {

  /**
   * A re-usable uniform model.
   */
  public static final UniformModel MODEL = new UniformModel(256);

  /**
   * Construct a uniform model.
   * @param numOutcomes the number of alternatives to encode
   */
  UniformModel(final int numOutcomes) {
    mNumOutcomes = numOutcomes;
  }

  private final int mNumOutcomes;

  @Override
  public int totalCount() {
    return mNumOutcomes;
  }

  @Override
  public void encode(ArithEncoder encoder, int symbol) {
    encoder.encode(symbol, symbol + 1, mNumOutcomes);
  }

  @Override
  public int decode(ArithDecoder decoder) {
    final int symbol = decoder.getCurrentSymbolCount(mNumOutcomes);
    decoder.removeSymbolFromStream(symbol, symbol + 1, mNumOutcomes);
    return symbol;
  }

  @Override
  public int pointToSymbol(int midCount) {
    return midCount;
  }

  @Override
  public void interval(int symbol, int[] result) {
    result[0] = symbol;
    result[1] = symbol + 1;
    result[2] = mNumOutcomes;
  }
}
