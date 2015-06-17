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
package com.rtg.util.bytecompression;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.rtg.util.diagnostic.Diagnostic;

/**
 * Distributes values across multiple arrays.
 */
public class MultiByteArray extends ByteArray {
  private final int mBits;
  private final int mChunkSize;
  private final long mMask;
  private byte[][] mData;
  private long mTotalSize;
  private long mCurrentLength;

  /**
   * Constructor
   * @param size the number of values that can be stored
   */
  public MultiByteArray(final long size) {
    this(size, 27);
  }

  /**
   * Constructor
   * @param size the number of values that can be stored
   * @param bits the number of bits of addressing used per array
   */
  public MultiByteArray(final long size, int bits) {
    assert 1 < bits;
    assert bits <= 30;
    mBits = bits;
    mChunkSize = 1 << mBits;
    mMask = mChunkSize - 1;
    mData = new byte[(int) (size >> mBits) + ((size & mMask) == 0 ? 0 : 1)][];
    int i = 0;
    long x = size;
    mTotalSize = 0;
    while (x > 0) {
      final int chunkSize = x > mChunkSize ? mChunkSize : (int) x;
      Diagnostic.developerLog("MultiByteArray allocating " + chunkSize
          + " bytes (block " + (i + 1) + " of " + mData.length + ")");
      mData[i++] = new byte[chunkSize];
      x -= chunkSize;
      mTotalSize += chunkSize;
    }
    mCurrentLength = size;
  }

  /**
   * Ensure there is enough space to store at least given number of entries.
   * Internally there may be room for more.
   * @param newMax new length of array.
   */
  public void extendTo(long newMax) {

    if (newMax < mCurrentLength) {
      throw new IllegalArgumentException("" + newMax + " is less than current length of: " + mCurrentLength);
    }
    while (mTotalSize < newMax) {
      final long i = mTotalSize >>> mBits;
      if (i >= mData.length) {
        final long newSize = (mData.length + 1) * 2;
        if (newSize > Integer.MAX_VALUE) {
          //I wonder what will be the earliest date it will be possible to get to here
          throw new RuntimeException("Attempting to allocate too large a chunk array. newSize=" + newSize);
        }
        mData = Arrays.copyOf(mData, (int) newSize);
      }
      final int ii = (int) i;
      if (mData[ii] == null) {
        mData[ii] = new byte[mChunkSize];
        mTotalSize += mChunkSize;
      } else {
        //short subarray
        final byte[] newSubArray = new byte[mChunkSize];
        final byte[] arr = mData[ii];
        final int lenArr = arr.length;
        System.arraycopy(arr, 0, newSubArray, 0, lenArr);
        mTotalSize += mChunkSize - lenArr;
        mData[ii] = newSubArray;
      }
    }
    mCurrentLength = newMax;
  }

  @Override
  public byte get(long offset) {
    final int block = (int) (offset >> mBits);
    final int blockpos = (int) (offset & mMask);
    return mData[block][blockpos];
  }

  @Override
  public void get(byte[] dest, long offset, int count) {
    int block = (int) (offset >> mBits);
    int blockpos = (int) (offset & mMask);
    int destpos = 0;
    int todo = count;

    while (todo > 0) {
      final int amountToCopy = Math.min(todo, mChunkSize - blockpos);
      System.arraycopy(mData[block], blockpos, dest, destpos, amountToCopy);
      destpos += amountToCopy;
      todo -= amountToCopy;
      block++;
      blockpos = 0;
    }
  }

  @Override
  public void set(long offset, byte value) {
    final int block = (int) (offset >> mBits);
    final int blockpos = (int) (offset & mMask);
    mData[block][blockpos] = value;
  }

  @Override
  public void set(long offset, byte[] buffer, int count) {
    set(offset, buffer, 0, count);
  }

  @Override
  public void set(long offset, byte[] src, int bOffset, int count) {
    int block = (int) (offset >> mBits);
    int blockpos = (int) (offset & mMask);
    int srcpos = bOffset;
    int todo = count;

    while (todo > 0) {
      final int amountToCopy = Math.min(todo, mChunkSize - blockpos);
      System.arraycopy(src, srcpos, mData[block], blockpos, amountToCopy);
      srcpos += amountToCopy;
      todo -= amountToCopy;
      block++;
      blockpos = 0;
    }
  }

  void load(final InputStream stream, final long offset, final int count) throws IOException {
    int read = 0;
    int i = (int) (offset >> mBits);
    int j = (int) (offset & mMask);
    while (read < count) {
      read += stream.read(mData[i], j, Math.min(mChunkSize - j, count - read));
      i++;
      j = 0;
    }
  }

  @Override
  public long length() {
    return mCurrentLength;
  }
}
