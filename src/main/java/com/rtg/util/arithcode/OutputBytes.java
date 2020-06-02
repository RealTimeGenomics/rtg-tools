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
import com.rtg.util.array.byteindex.ByteChunks;

/**
 */
@TestClass("com.rtg.util.arithcode.BytesTest")
public final class OutputBytes implements Output {

  static final int BITS_PER_BYTE = 8;

  private final ByteChunks mOut;

  private int mByteBuffer;

  private int mBitsToGo;

  /**
   * @param out where the output bytes are written.
   */
  public OutputBytes(ByteChunks out) {
    mOut = out;
    reset();
  }

  private void flush() {
    mOut.append(mByteBuffer);
    reset();
  }

  private void reset() {
    mBitsToGo = BITS_PER_BYTE;
    mByteBuffer = 0;
  }

  private void writtenBit() {
    --mBitsToGo;
    if (mBitsToGo == 0) {
      flush();
    }
  }

  @Override
  public long endBlock() {
    if (mBitsToGo < BITS_PER_BYTE) {
      mByteBuffer = mByteBuffer << mBitsToGo;
      flush();
    }
    return mOut.length();
  }

  @Override
  public void close() {
    assert mBitsToGo == BITS_PER_BYTE;
    mOut.trim();
  }

  @Override
  public void writeBit(boolean bit) {
    if (bit) {
      writeBitTrue();
    } else {
      writeBitFalse();
    }
  }

  @Override
  public void writeBitTrue() {
    assert mBitsToGo > 0 && mBitsToGo <= BITS_PER_BYTE;
    mByteBuffer = (mByteBuffer << 1) | 1;
    writtenBit();
  }

  @Override
  public void writeBitFalse() {
    assert mBitsToGo > 0 && mBitsToGo <= BITS_PER_BYTE;
    mByteBuffer = mByteBuffer << 1;
    writtenBit();
  }
}
