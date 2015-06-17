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

import com.reeltwo.jumble.annotations.TestClass;


/**
 * <P>Performs arithmetic decoding, converting bit input into
 * cumulative probability interval output.  Returns probabilities as
 * integer counts <code>low</code>, <code>high</code> and
 * <code>total</code>, with the range being
 * <code>[low/total,high/total)</code>.
 *
 * <P>For more details, see <a href="../../../tutorial.html">The Arithmetic Coding Tutorial</a>.
 *
 * @version 1.1
 * @see ArithEncoder
 * @see Input
 */
@TestClass("com.rtg.util.arithcode.ArithTest")
public final class ArithDecoder extends ArithCoder {

  /**
   * Construct an arithmetic decoder that reads from the given
   * bit input.
   * @param in Bit input from which to read bits.
   */
  public ArithDecoder(Input in) {
    mIn = in;
    for (int i = 1; i <= CODE_VALUE_BITS; ++i) {
      bufferBit();
    }
  }

  /**
   * Returns a count for the current symbol that will be between the
   * low and high counts for the symbol in the model given the total count.
   * Once symbol is retrieved, the model is used to compute the actual low,
   * high and total counts and {@link #removeSymbolFromStream} is called.
   * @param totalCount The current total count for the model.
   * @return A count that is in the range above or equal to the low count and less than the high count of the next symbol decoded.
   */
  public int getCurrentSymbolCount(int totalCount) {
    return (int) (((mValue - mLow + 1) * totalCount - 1) / (mHigh - mLow + 1));
  }

  /**
   * Removes a symbol from the input stream.  Called after {@link #getCurrentSymbolCount}.
   * @param lowCount Cumulative count for symbols indexed below symbol to be removed.
   * @param highCount <code>lowCount</code> plus count for this symbol.
   * @param totalCount Total count for all symbols seen.
   */
  public void removeSymbolFromStream(long lowCount, long highCount, long totalCount) {
    final long range = mHigh - mLow + 1;
    mHigh = mLow + (range * highCount) / totalCount - 1;
    mLow = mLow + (range * lowCount) / totalCount;
    while (true) {
      if (mHigh < HALF) {
        // no effect
      } else if (mLow >= HALF) {
        mValue -= HALF;
        mLow -= HALF;
        mHigh -= HALF;
      } else if (mLow >= FIRST_QUARTER && mHigh <= THIRD_QUARTER) {
        mValue -= FIRST_QUARTER;
        mLow -= FIRST_QUARTER;
        mHigh -= FIRST_QUARTER;
      } else {
        return;
      }
      mLow <<= 1; // = 2 * mLow;
      mHigh = (mHigh << 1) + 1; // 2 * mHigh + 1;
      bufferBit();
    }
  }

  /**
   * Input stream from which to read bits.
   */
  private final Input mIn;

  /**
   * Current bits for decoding.
   */
  private long mValue; // implied = 0;

  /**
   * Reads a bit from the underlying bit input stream and buffers it.
   */
  private void bufferBit() {
    mValue = mValue << 1;
    if (mIn.readBit()) {
      ++mValue;
    }
  }

}
