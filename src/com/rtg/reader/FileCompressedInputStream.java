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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FalseSeekableStream;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.RandomAccessFileStream;
import com.rtg.util.io.SeekableStream;

/**
 * Class for read longs from file.
 */
public class FileCompressedInputStream extends SeekableStream {

  /** The total number of values stored. */
  private final long mSize;

  /** How many values we compress into each bit-field (via multiplication). */
  private final int mPerBitfield;

  /** How many bit-fields we can squash into each long (via bit shifting). */
  private final int mNumBitfields;

  /** The number of values packed into each long. */
  private final int mPerLong;

  /** Each bit-field of this width contains <code>mPerByte</code> values. */
  private final int mBits;

  /** Equals <code>Math.pow(2, mBits) - 1</code> */
  private final int mMask;

  /**
   * A lookup table: given a bit-field with value <code>val</code>,
   * <code>mValue[i][v]</code> is the value of the <code>i'th</code> field in that bit-field.
   */
  private final byte[][] mValue;





  private final SeekableStream mStream;
  private final byte[] mRawBuffer = new byte[1024 * 1024];
  private final long[] mBuffer = new long[1024 * 1024 / 8];
  private long mBufferStartPos;
  private int mBufferInUse;

  private long mPos;

  /**
   * Constructor
   * @param file file containing long values
   * @param range number of values compressed data can take
   * @param length number of values in compressed data
   * @param seekable whether we desire random access or sequential access only
   * @throws IOException if an IO error occurs
   */
  public FileCompressedInputStream(File file, int range, long length, boolean seekable) throws IOException {
    if (seekable) {
      final RandomAccessFile raf = new RandomAccessFile(file, "r");
      mStream = new RandomAccessFileStream(raf);
    } else {
      final InputStream is = FileUtils.createFileInputStream(file, false);
      mStream = new FalseSeekableStream(is);
    }
    //stuff from CompressedByteArray
    /* The allowable values are <code>0 .. mRange-1</code>. */
    mPerBitfield = range == 5 ? 3 : range == 22 ? 2 : 1;
    mBits = range == 5 ? 7 : range == 22 ? 9 : CompressedByteArray.minBits(range);
    mMask = (int) Math.pow(2, mBits) - 1;
    mNumBitfields = 64 / mBits;
    mPerLong = mPerBitfield * mNumBitfields;
    mSize = length;
    //mData = LongCreate.createIndex(size / mPerLong + 1);

    // precalculate our lookup tables
    mValue = new byte[mPerBitfield][];
    /* Powers of <code>mRange</code>. */
    for (int withinBitfield = 0; withinBitfield < mPerBitfield; withinBitfield++) {
      mValue[withinBitfield] = new byte[mMask + 1];
      final int divisor = (int) Math.pow(range, withinBitfield);
      for (int i = 0; i <= mMask; i++) {
        mValue[withinBitfield][i] = (byte) (i / divisor % range);
      }
    }
  }

  private boolean inRange(long pos) {
    return pos >= mBufferStartPos && pos < mBufferStartPos + mBufferInUse;
  }

  /* Reads from inner stream a multiple of 8 bytes, if it cannot it is an error */
  private int readInternal(byte[] buff, int offset, int length) throws IOException {
    int len = mStream.read(buff, offset, length);
    //read a multiple of 8 bytes to prevent futzing around
    final int rem = len & 7;
    if (rem > 0) {
      IOUtils.readFully(mStream, mRawBuffer, offset + len, rem);
      len += rem;
    }
    return len;
  }

  private void seekRange(long pos) throws IOException {
    mBufferStartPos = pos < 0 ? 0L : pos;
    final long seekPos = mBufferStartPos * 8;
    mStream.seek(seekPos);
    final int len = readInternal(mRawBuffer, 0, mRawBuffer.length);
    mBufferInUse = len / 8;
    ByteArrayIOUtils.convertToLongArray(mRawBuffer, 0, len, mBuffer, 0, mBufferInUse);
  }

  private void readNext() throws IOException {
    final int len = readInternal(mRawBuffer, 0, mRawBuffer.length);
    final int bLen = len / 8;
    ByteArrayIOUtils.convertToLongArray(mRawBuffer, 0, len, mBuffer, 0, bLen);
    mBufferStartPos = mBufferStartPos + mBufferInUse;
    mBufferInUse = bLen;
  }

  private long data(long offset) throws IOException {
    while (!inRange(offset)) {
      if (offset < mBufferStartPos || offset > mBufferStartPos + 2 * mBufferInUse) {
        seekRange(offset);
      } else {
        readNext();
      }
    }
    return mBuffer[(int) (offset - mBufferStartPos)];
  }

  private byte get(long offset) throws IOException {
    final long whichLong = offset / mPerLong;
    long longValue = data(whichLong);
    final int whichBitfield = (int) (offset % mPerLong) / mPerBitfield;
    longValue = longValue >>> (whichBitfield * mBits);
    final int bitField = (int) (longValue & mMask);
    final int withinBitfield = (int) (offset % mPerLong) % mPerBitfield;
    return mValue[withinBitfield][bitField];
  }

  private void get(byte[] dest, long offset, int bOffset, int length) throws IOException {
    long whichLong = offset / mPerLong;
    long longValue = data(whichLong);
    int whichBitfield = (int) (offset % mPerLong) / mPerBitfield;
    longValue = longValue >>> (whichBitfield * mBits);
    int bitField = (int) (longValue & mMask);
    int withinBitfield = (int) (offset % mPerLong) % mPerBitfield;
    for (int i = 0; i < length; i++) {
      // this line is a faster version of the following two, using a lookup table.
      dest[i + bOffset] = mValue[withinBitfield][bitField];
      // the slow version...
      //final int divisor = (int) Math.pow(mRange, withinBitfield);
      //dest[i] = (byte) (bitField / divisor % mRange);
      withinBitfield++;
      if (withinBitfield == mPerBitfield) {
        // move along to next bitfield
        withinBitfield = 0;
        whichBitfield++;
        longValue = longValue >>> mBits;
        bitField = (int) (longValue & mMask);
        if (whichBitfield == mNumBitfields) {
          whichBitfield = 0;
          whichLong++;
          longValue = data(whichLong);
          bitField = (int) (longValue & mMask);
        }
      }
    }
  }

  @Override
  public long getPosition() {
    return mPos;
  }

  @Override
  public long length() {
    return mSize;
  }

  @Override
  public void seek(long pos) {
    mPos = pos;
  }

  @Override
  public int read() throws IOException {
    if (mPos == mSize) {
      return -1;
    }
    return get(mPos++) & 0xff;
  }
  @Override
  public void close() throws IOException {
    mStream.close();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int lenUse = len;
    if (mSize - mPos < len) {
      lenUse = (int) (mSize - mPos);
    }
    if (lenUse == 0) {
      return -1;
    }
    get(b, mPos, off, lenUse);
    mPos += lenUse;
    return lenUse;
  }

}
