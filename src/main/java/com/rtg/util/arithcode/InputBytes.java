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
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 */
@TestClass("com.rtg.util.arithcode.BytesTest")
public final class InputBytes extends IntegralAbstract implements Input {

  private final ByteChunks mIn;

  private final long mStart;

  private final long mEnd;

  private long mPosition;

  private int mBitsToGo = 0;

  private int mByteBuffer;

  private boolean mEof = false;

  /**
   * @param in index containing concatenated compressed blocks.
   * @param start index of first byte in current block (0 based).
   * @param end index one past last byte in current block (0 based).
   */
  public InputBytes(ByteChunks in, long start, long end) {
    mIn = in;
    mStart = start;
    mEnd = end;
    mPosition = start;
    fetchInput();
  }

  @Override
  public boolean readBit() {
    if (mEof) {
      return false;
    }
    --mBitsToGo;
    final boolean res = (mByteBuffer & (1 << mBitsToGo)) != 0;
    fetchInput();
    return res;
  }

  private void fetchInput() {
    if (mBitsToGo == 0) {
      if (mPosition == mEnd) {
        mEof = true;
      } else {
        mByteBuffer = mIn.getByte(mPosition);
        mBitsToGo = OutputBytes.BITS_PER_BYTE;
        ++mPosition;
      }
    }
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(toString(), 0 <= mStart && mStart <= mPosition && mPosition <= mEnd && mEnd <= mIn.length());
    Exam.assertTrue(0 <= mBitsToGo && mBitsToGo <= OutputBytes.BITS_PER_BYTE);
    Exam.assertTrue(0 <= mByteBuffer && mByteBuffer < (1 << OutputBytes.BITS_PER_BYTE));
    return true;
  }

}
