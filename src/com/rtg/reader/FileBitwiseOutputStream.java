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

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.rtg.util.io.FileUtils;

/**
 * Output stream for writing compressed files like {@link com.rtg.util.bytecompression.BitwiseByteArray}
 */
public class FileBitwiseOutputStream extends OutputStream {

  private static final int BITS_PER_LONG = 64;

  /** The right shift that corresponds to a division by 64. */
  private static final int WHICH_LONG = 6;

  /** The mask that gets the bit position with the long. */
  private static final long WITHIN_LONG = (1L << WHICH_LONG) - 1;

  private final int mBits;
  private final DataOutputStream mStream;
  private long mPos;

  //private final byte[] mRawBuffer;
  private final long[] mBuffer;
  private long mBufferStartPos;
  private int mBufferInUse;
  //private final long mSize;

  /**
   * Constructor
   * @param outputFile compressed file to write
   * @param bits number of bits used per entry
   * @throws IOException If an IO error occurs
   */
  public FileBitwiseOutputStream(File outputFile, int bits) throws IOException {
    mStream = new DataOutputStream(FileUtils.createOutputStream(outputFile, false));
    mBits = bits;
    //mRawBuffer = new byte[1024 * 1024];
    mBuffer = new long[1024 * 1024 / 8];
  }

  /**
   * @return number of values written
   */
  public long values() {
    return mPos;
  }

  private boolean inRange(long pos) {
    return pos - mBufferStartPos < mBuffer.length;
  }

  private void flushCurrent() throws IOException {
    final int toWrite = mBufferInUse - mBits; //can only be sure those mBits behind are ready to be written
    if (toWrite > 0) {
      for (int i = 0; i < toWrite; i++) {
        mStream.writeLong(mBuffer[i]);
      }
      final int newInUse = mBufferInUse - toWrite;
      System.arraycopy(mBuffer, toWrite, mBuffer, 0, newInUse);
      Arrays.fill(mBuffer, newInUse, mBufferInUse, 0);
      mBufferStartPos += toWrite;
      mBufferInUse = newInUse;
    }
  }

  private void dataBitwiseOrSet(long pos, long value) throws IOException {
    while (!inRange(pos)) {
      flushCurrent();
    }
    final int index = (int) (pos - mBufferStartPos);
    mBuffer[index] |= value;
    if (index >= mBufferInUse) {
      mBufferInUse = index + 1;
    }
  }
  @Override
  public void close() throws IOException {
    try (DataOutputStream out = mStream) {
      for (int i = 0; i < mBufferInUse; i++) {
        out.writeLong(mBuffer[i]);
      }
      mBufferInUse = 0;
    }
  }
  @Override
  public void flush() throws IOException {
    flushCurrent();
  }

  @Override
  public void write(int b) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    set(mPos, b, off, len);
    mPos += len;
  }

  private void set(final long offset, final byte[] data, final int bOffset, final int length) throws IOException {
    long whichLong = (offset >>> WHICH_LONG) * mBits;
    int whichBit = (int) (offset & WITHIN_LONG);
    for (int pos = bOffset; pos < bOffset + length; pos++) {
      int value = data[pos];
      for (int b = mBits - 1; b >= 0; b--) {
        //final long old = mData[b].get(whichLong);
        //mData[b].set(whichLong, old | ((value & 1L) << whichBit));
        dataBitwiseOrSet(whichLong + b, (value & 1L) << whichBit);
        value = value >>> 1;
      }
      // now move along to the next bit
      whichBit++;
      if (whichBit == BITS_PER_LONG) {
        whichBit = 0;
        whichLong += mBits;
      }
    }
  }
}
