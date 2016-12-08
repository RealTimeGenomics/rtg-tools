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

import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.io.FileUtils;

/**
 *
 */
public class FileCompressedOutputStream extends OutputStream {


  /** The total number of values stored. */

  /** How many values we compress into each bit-field (via multiplication). */
  private final int mPerBitfield;

  /** How many bit-fields we can squash into each long (via bit shifting). */
  private final int mNumBitfields;

  /** The number of values packed into each long. */
  private final int mPerLong;

  /** Each bit-field of this width contains <code>mPerByte</code> values. */
  private final int mBits;

  /** The allowable values are <code>0 .. mRange-1</code>. */
  private final int mRange;

  /** Powers of <code>mRange</code>. */
  private final int[] mRangePowers;

  private final DataOutputStream mStream;
  private final long[] mBuffer = new long[1024 * 1024 / 8];
  private long mBufferStartPos;
  private int mBufferInUse;

  private long mPos;

  /**
   * Constructor
   * @param file file to write long values to
   * @param range number of values compressed data can take
   * @throws IOException if an IO error occurs
   */
  public FileCompressedOutputStream(File file, int range) throws IOException {
    mStream = new DataOutputStream(FileUtils.createOutputStream(file, false));
    //stuff from CompressedByteArray
    mRange = range;
    mPerBitfield = range == 5 ? 3 : range == 22 ? 2 : 1;
    mBits = range == 5 ? 7 : range == 22 ? 9 : CompressedByteArray.minBits(range);
    mNumBitfields = 64 / mBits;
    mPerLong = mPerBitfield * mNumBitfields;
    // precalculate our lookup tables
    mRangePowers = new int[mPerBitfield];
    for (int withinBitfield = 0; withinBitfield < mPerBitfield; ++withinBitfield) {
      mRangePowers[withinBitfield] = (int) Math.pow(mRange, withinBitfield);
    }
  }

  private boolean inRange(long pos) {
    return pos - mBufferStartPos < mBuffer.length;
  }

  private void flushCurrent() throws IOException {
    final int toWrite = mBufferInUse - mBits; //can only be sure those 1 byte behind are ready to be written
    if (toWrite > 0) {
      for (int i = 0; i < toWrite; ++i) {
        mStream.writeLong(mBuffer[i]);
      }
      final int newInUse = mBufferInUse - toWrite;
      System.arraycopy(mBuffer, toWrite, mBuffer, 0, newInUse);
      Arrays.fill(mBuffer, newInUse, mBufferInUse, 0);
      mBufferStartPos += toWrite;
      mBufferInUse = newInUse;
    }
  }

  private long data(long pos) throws IOException {
    while (!inRange(pos)) {
      flushCurrent();
    }
    return mBuffer[(int) (pos - mBufferStartPos)];
  }
  private void dataSet(long pos, long value) throws IOException {
    while (!inRange(pos)) {
      flushCurrent();
    }
    final int index = (int) (pos - mBufferStartPos);
    mBuffer[index] = value;
    if (index >= mBufferInUse) {
      mBufferInUse = index + 1;
    }
  }
  @Override
  public void close() throws IOException {
    try (final DataOutputStream out = mStream) {
      for (int i = 0; i < mBufferInUse; ++i) {
        out.writeLong(mBuffer[i]);
      }
      out.writeLong(0L); //reader is slightly dumb

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

  private void set(long offset, byte[] data, int bOffset, int length) throws IOException {
    // we must read the first long, in case it is already partially filled.
    long whichLong = offset / mPerLong;
    long longValue = data(whichLong);
    int whichBitfield = (int) (offset % mPerLong) / mPerBitfield;
    int withinBitfield = (int) (offset % mPerLong) % mPerBitfield;
    for (int i = bOffset; i < bOffset + length; ++i) {
      final int mult = mRangePowers[withinBitfield];
      // final int mult = (int) Math.pow(mRange, withinBitfield);
      final byte val = data[i];
      assert 0 <= val && val < mRange : "value: " + val + " i: " + i;
      longValue += (long) (val * mult) << (whichBitfield * mBits);
      ++withinBitfield;
      if (withinBitfield == mPerBitfield) {
        withinBitfield = 0;
        ++whichBitfield;
        if (whichBitfield == mNumBitfields) {
          dataSet(whichLong, longValue);
          whichBitfield = 0;
          ++whichLong;
          longValue = 0L;
        }
      }
    }
    // store the last partial long.
    if (longValue != 0) {
      dataSet(whichLong, longValue);
    }
  }


  /**
   * @return number of values written
   */
  public long values() {
    return mPos;
  }


}
