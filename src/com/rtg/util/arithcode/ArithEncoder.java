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
 * <P>Performs arithmetic encoding, converting cumulative probability
 * interval input into bit output.  Cumulative probability intervals
 * are given as integer counts <code>low</code>, <code>high</code> and
 * <code>total</code>, with the range being
 * <code>[low/total,high/total)</code>.
 *
 * <P>For more details, see <a href="../../../tutorial.html">The Arithmetic Coding Tutorial</a>.
 * </P>
 * <P>
 * The public methods below can be called in the following order:
 * <br>
 * <code>(endBlock* encode* endBlock+)* close endBlock*</code>
 * </P>
 * @version 1.1
 * @see ArithDecoder
 * @see Output
 */
@TestClass("com.rtg.util.arithcode.ArithTest")
public final class ArithEncoder extends ArithCoder {

  /**
   * Construct an arithmetic coder from a bit output.
   * @param out Underlying bit output.
   */
  public ArithEncoder(Output out) {
    mOut = out;
  }

  /**
   * Get a key which is passed into Input to retrieve a particular block.
   * @return the key associated with the next block.
   */
  public long endBlock() {
    if (mState == State.ENCODING) {
      ++mBitsToFollow; // need a final bit (not sure why)
      if (mLow < FIRST_QUARTER) {
        bitPlusFollowFalse();
      } else {
        bitPlusFollowTrue();
      }
      mBitsToFollow = 0;
      mLow = 0;
      mHigh = TOP_VALUE;
      mState = State.END_BLOCK;
    }
    return mOut.endBlock();
  }

  /**
   * Close the arithmetic encoder, writing all bits that are
   * buffered and closing the underlying output streams.
   */
  public void close() {
    assert mState == State.END_BLOCK;
    mState = State.CLOSED;
    mOut.close();
  }

  /**
   * Encodes an interval expressed as a low count, high count and total count.
   * The high count is taken to be exclusive, and the resulting range is
   * <code>highCount - lowCount + 1</code>.
   * @param lowCount Cumulative count of symbols below current one.
   * @param highCount Cumulative count of symbols below current one plus current one.
   * @param totalCount Cumulative count of all symbols.
   */
  public void encode(int lowCount, int highCount, int totalCount) {
    assert mState == State.ENCODING || mState == State.END_BLOCK;
    mState = State.ENCODING;
    final long range = mHigh - mLow + 1;
    mHigh = mLow + (range * highCount) / totalCount - 1;
    mLow  = mLow + (range * lowCount) / totalCount;
    while (true) {
      if (mHigh < HALF) {
        bitPlusFollowFalse();
      } else if (mLow >= HALF) {
        bitPlusFollowTrue();
        mLow -= HALF;
        mHigh -= HALF;
      } else if (mLow >= FIRST_QUARTER && mHigh < THIRD_QUARTER) {
        ++mBitsToFollow;
        mLow -= FIRST_QUARTER;
        mHigh -= FIRST_QUARTER;
      } else {
        return;
      }
      mLow <<= 1;
      mHigh = (mHigh << 1) + 1;
    }
  }

  /**
   * Bit output stream for writing encoding bits.
   */
  private final Output mOut;

  /**
   * Number of bits beyond first bit that were normalized.
   */
  private int mBitsToFollow; // implied = 0;

  private static enum State {
    CLOSED,
    END_BLOCK,
    ENCODING
  }

  private State mState = State.END_BLOCK;

  /**
   * Write a <code>true</code> bit, and then a number of <code>false</code> bits
   * equal to the number of bits to follow.
   */
  private void bitPlusFollowTrue() {
    for (mOut.writeBitTrue(); mBitsToFollow > 0; --mBitsToFollow) {
      mOut.writeBitFalse();
    }
  }

  /**
   * Write a <code>false</code> bit, and then a number of <code>true</code> bits
   * equal to the number of bits to follow.
   */
  private void bitPlusFollowFalse() {
    for (mOut.writeBitFalse(); mBitsToFollow > 0; --mBitsToFollow) {
      mOut.writeBitTrue();
    }
  }

}
