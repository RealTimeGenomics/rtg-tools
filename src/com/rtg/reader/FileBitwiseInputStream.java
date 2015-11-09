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

import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FalseSeekableStream;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOUtils;
import com.rtg.util.io.RandomAccessFileStream;
import com.rtg.util.io.SeekableStream;

/**
 * Files created by <code>BitwiseByteArray.dumpCompressedValues()</code>
 */
public class FileBitwiseInputStream extends SeekableStream {
  private static final int BITS_PER_LONG = 64;

  /** The right shift that corresponds to a division by 64. */
  private static final int WHICH_LONG = 6;

  /** The mask that gets the bit position with the long. */
  private static final long WITHIN_LONG = (1L << WHICH_LONG) - 1;

  private final int mBits;
  private final SeekableStream mStream;
  private long mPos;

  private final byte[] mRawBuffer;
  private final long[] mBuffer;
  private final long[] mBitBuffer;
  private long mBufferStartPos;
  private int mBufferInUse;
  private final long mSize;

  /**
   * Constructor
   * @param inputFile compressed file to read
   * @param bits number of bits used per entry
   * @param numberValues number of values in file
   * @param seekable whether stream should be possible to seek, if set to false then attempts to seek will result in <code>UnsupportedOperationException</code>
   * @throws IOException If an IO error occurs
   */
  public FileBitwiseInputStream(File inputFile, int bits, long numberValues, boolean seekable) throws IOException {
    if (seekable) {
      final RandomAccessFile raf = new RandomAccessFile(inputFile, "r");
      mStream = new RandomAccessFileStream(raf);
    } else {
      final InputStream is = FileUtils.createFileInputStream(inputFile, false);
      mStream =  new FalseSeekableStream(is);
    }
    mBits = bits;
    mSize = numberValues;
    mRawBuffer = new byte[1024 * 1024];
    mBuffer = new long[1024 * 1024 / 8];
    mBitBuffer = new long[bits];
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
    //often get values +- mBits from pos
    mBufferStartPos = pos - mBits < 0 ? 0L : pos - mBits;
    final long seekPos = mBufferStartPos * 8;
    mStream.seek(seekPos);
    final int len = readInternal(mRawBuffer, 0, mRawBuffer.length);
    mBufferInUse = len / 8;
    ByteArrayIOUtils.convertToLongArray(mRawBuffer, 0, len, mBuffer, 0, mBufferInUse);
  }

  private void readNext() throws IOException {
    //often get values +- mBits from pos
    final int shift = mBufferInUse > mBits ? mBits : mBufferInUse;
    final int len = readInternal(mRawBuffer, 0, mRawBuffer.length - 8 * shift);
    System.arraycopy(mBuffer, mBufferInUse - shift, mBuffer, 0, shift);
    final int bLen = len / 8;
    ByteArrayIOUtils.convertToLongArray(mRawBuffer, 0, len, mBuffer, shift, shift + bLen);
    mBufferStartPos = mBufferStartPos + mBufferInUse - shift;
    mBufferInUse = bLen + shift;
  }

  private long data(long pos) throws IOException {
    while (!inRange(pos)) {
      if (pos < mBufferStartPos || pos > mBufferStartPos + 2 * mBufferInUse) {
        seekRange(pos);
      } else {
        readNext();
      }
    }
    return mBuffer[(int) (pos - mBufferStartPos)];
  }

  private byte get(final long offset) throws IOException {
    final long whichLong = (offset >>> WHICH_LONG) * mBits;
    final int whichBit = (int) (offset & WITHIN_LONG);
    int result = 0;
    for (int b = 0; b < mBits; b++) {
      //final long longValue = mData[b].get(whichLong);
      final long longValue = data(whichLong + b);
      result = (result << 1) | (int) ((longValue >> whichBit) & 1);
    }
    return (byte) result;
  }

  private void get(final byte[] dest, final long offset, int bOffset, final int length) throws IOException {
    long whichLong = (offset >>> WHICH_LONG) * mBits;
    int whichBit = (int) (offset & WITHIN_LONG);
    if (mBits == 3) {
      // special in-lined version of the code for the DNA case.
      long bits0 = data(whichLong);
      long bits1 = data(whichLong + 1);
      long bits2 = data(whichLong + 2);
      for (int pos = 0; pos < length; pos++) {
        final long bit0 = (bits0 >>> whichBit) & 1;
        final long bit1 = (bits1 >>> whichBit) & 1;
        final long bit2 = (bits2 >>> whichBit) & 1;
        dest[pos + bOffset] = (byte) ((bit0 << 2) | (bit1 << 1) | bit2);
        // now move along to the next bit
        whichBit++;
        if (whichBit == BITS_PER_LONG && pos < length - 1) {
          whichBit = 0;
          whichLong += mBits;
          bits0 = data(whichLong);
          bits1 = data(whichLong + 1);
          bits2 = data(whichLong + 2);
        }
      }
    } else {
      for (int b = 0; b < mBits; b++) {
        mBitBuffer[b] = data(whichLong + b);
      }
      for (int pos = 0; pos < length; pos++) {
        int value = 0;
        for (int b = 0; b < mBits; b++) {
          value = (value << 1) | (int) ((mBitBuffer[b] >>> whichBit) & 1);
        }
        dest[pos + bOffset] = (byte) value;
        // now move along to the next bit
        whichBit++;
        if (whichBit == BITS_PER_LONG && pos < length - 1) {
          whichBit = 0;
          whichLong += mBits;
          for (int b = 0; b < mBits; b++) {
            mBitBuffer[b] = data(whichLong + b);
          }
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
