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
package com.rtg.util.array.intindex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.IndexType;
import com.rtg.util.integrity.Exam;

/**
 * Break array into chunks to fit within java convention that indices must
 * be ints.
 *
 */
public final class IntChunks extends IntIndex implements ExtensibleIndex {

  private final int mChunkBits;

  private final int mChunkSize;

  private final int mChunkMask;

  private int[][] mArray;

  private long mTotalLength;


  /**
   * Constructs an index by splitting into array chunks.
   *
   * @param length of the index being created.
   */
  public IntChunks(final long length) {
    this(length, CHUNK_BITS);
  }

  /**
   * Constructs an index by splitting into array chunks.
   * @param numberChunks number of chunks to be created (must be enough to support length).
   * @param length of the index being created (actual length is guaranteed to be &ge; length).
   * @param chunkBits number of bits used for an entry in a chunk.
   */
  public IntChunks(final int numberChunks, final long length, final int chunkBits) {
    super(length);
    assert chunkBits > 0 && chunkBits <= 30;
    mChunkBits = chunkBits;
    mChunkSize = 1 << mChunkBits;
    mChunkMask = mChunkSize - 1;

    mArray = new int[numberChunks][];
    long soFar = 0;
    for (int i = 0; i < numberChunks && soFar < length; i++) {
      mArray[i] =  new int[mChunkSize];
      soFar += mChunkSize;
    }
    mTotalLength = soFar;
    if (length > mTotalLength) {
      throw new RuntimeException("too few chunks for length=" + length + " chunks=" + numberChunks + " chunkBits=" + chunkBits);
    }
    mLength = length;
    assert integrity();
  }

  /**
   * Constructs an index by splitting into array chunks.
   * This version sets the size of the chunks - it should only be used directly for testing.
   * @param length of the index being created.
   * @param chunkBits number of bits used for an entry in a chunk.
   */
  IntChunks(final long length, final int chunkBits) {
    super(length);
    assert chunkBits >= 0 && chunkBits <= 31;
    mChunkBits = chunkBits;
    mChunkSize = 1 << mChunkBits;
    mChunkMask = mChunkSize - 1;

    final long ch = (length + mChunkSize - 1) / mChunkSize;
    if (ch > Integer.MAX_VALUE) {
      throw new RuntimeException("length requested too long length=" + length + " mChunkSize=" + mChunkSize);
    }
    final int chunks = (int) ch;
    mArray = new int[chunks][];
    long left = mLength;
    long total = 0;
    for (int i = 0; i < chunks; i++) {
      final int assignedLength = left <= mChunkSize ? (int) left :  mChunkSize;
      assert assignedLength > 0;
      mArray[i] =  new int[assignedLength];
      left -= assignedLength;
      total += assignedLength;
    }
    assert left == 0;
    mTotalLength = total;
    assert integrity();
  }

  private IntChunks(long length, int chunkBits, int chunkSize, int chunkMask, int[][] array, long totalLength) {
    super(length);
    mChunkBits = chunkBits;
    mChunkSize = chunkSize;
    mChunkMask = chunkMask;
    mArray = array;
    mTotalLength = totalLength;
  }


  @Override
  public long extendBy(final long length) {
    if (length < 0) {
      throw new IllegalArgumentException("" + length);
    }
    final long res = mLength;
    final long target = mLength + length;
    while (mTotalLength < target) {
      final long i = mTotalLength >>> mChunkBits;
    if (i >= mArray.length) {
      final long newSize = (mArray.length + 1) * 2;
      if (newSize > Integer.MAX_VALUE) {
        //I wonder what will be the earliest date it will be possible to get to here
        //throw new RuntimeException("Attempting to allocate too large a chunk array. newSize=" + newSize);
        throw new RuntimeException();
      }
      mArray = Arrays.copyOf(mArray, (int) newSize);
    }
    final int ii = (int) i;
    if (mArray[ii] == null) {
      final int size = (int) Math.min(mChunkSize, target);
      mArray[ii] = new int[size];
      mTotalLength += size;
    } else {
      //short subarray
      final int[] newSubArray;
      final int newLength;
      if (mTotalLength < mChunkSize) {
        //use standard 3 / 2 size increase to avoid giant increase in small array
        final long threeovertwo = mArray[ii].length * 3L / 2;
        final long atLeast = Math.max(threeovertwo, target);
        newLength = (int) Math.min(mChunkSize, atLeast); //chunk size is int -> result is int
        assert newLength == mChunkSize || newLength == atLeast;
      } else {
        newLength = mChunkSize;
      }
      newSubArray = new int[newLength];
      final int[] arr = mArray[ii];
      final int lenArr = arr.length;
      System.arraycopy(arr, 0, newSubArray, 0, lenArr);
      mTotalLength += newLength - lenArr;
      mArray[ii] = newSubArray;
    }
    }
    mLength = target;
    assert integrity();
    return res;
  }

  @Override
  public void trim(long length) {
    if (length > mLength) {
      throw new IllegalArgumentException("" + length);
    }
    final long newArraySize = (length + mChunkMask) >>> mChunkBits;
    if (newArraySize < mArray.length) {
      mArray = Arrays.copyOf(mArray, (int) newArraySize);
    }
    final int offset = (int) ((length - 1) & mChunkMask) + 1;
    if (offset < mChunkSize) {
      final int ns = (int) (newArraySize - 1);
      mArray[ns] = Arrays.copyOf(mArray[ns], offset);
    }
    mTotalLength = length;
    mLength = length;
  }

  /**
   * Get the array length. Should only be used for testing.
   *
   * @return the array length.
   */
  int arrayLength() {
    return mArray.length;
  }

  /**
   * Get the total length. Should only be used for testing.
   *
   * @return the total length.
   */
  long totalLength() {
    return mTotalLength;
  }

  @Override
  public int getInt(final long index) {
    final int chunk = (int) (index >> mChunkBits);
    final int offset = (int) (index & mChunkMask);
    return mArray[chunk][offset];
  }

  @Override
  public long getSigned(final long offset) {
    return getInt(offset);
  }

  @Override
  public void setSigned(final long index, final long value) {
    //High order bits must be zero
    assert  ((value >= 0 ? value : -value) & HIGH_MASK) == 0L : value;
    setInt(index, (int) value);
  }

  @Override
  public void swap(final long index1, final long index2) {
    final int chunk1 = (int) (index1 >> mChunkBits);
    final int offset1 = (int) (index1 & mChunkMask);

    final int chunk2 = (int) (index2 >> mChunkBits);
    final int offset2 = (int) (index2 & mChunkMask);
    final int temp = mArray[chunk1][offset1];
    mArray[chunk1][offset1] = mArray[chunk2][offset2];
    mArray[chunk2][offset2] = temp;
  }

  @Override
  public void setInt(final long index, final int value) {
    final int chunk = (int) (index >> mChunkBits);
    final int offset = (int) (index & mChunkMask);
    mArray[chunk][offset] = value;
  }

  /**
   * Get the chunk size. Should only be used for testing.
   *
   * @return the chunk size.
   */
  int chunkSize() {
    return mChunkSize;
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertTrue(mChunkBits > 0 && mChunkBits <= 31);
    Exam.assertTrue((mChunkSize & mChunkMask) == 0);
    Exam.assertTrue(mChunkSize > 0);
    Exam.assertTrue(mChunkMask > 0);
    Exam.assertTrue(mChunkMask + 1 == mChunkSize);
    if (mArray == null) { //close call has been made.
      return true;
    }
    Exam.assertTrue("" + mLength + ":" + mTotalLength, 0 <= mLength && mLength <= mTotalLength);
    final long il = mTotalLength == 0 ? 0 : ((mTotalLength - 1) >>> mChunkBits) + 1;
    long total = 0;
    for (int i = 0; i < il; i++) {
      final int[] arr = mArray[i];
      Exam.assertNotNull(arr);
      Exam.assertTrue(mChunkSize >= arr.length);
      total += arr.length;
    }
    Exam.assertEquals(total, mTotalLength);
    for (int i = (int) il; i < mArray.length; i++) {
      Exam.assertTrue(mArray[i] == null);
    }
    return true;
  }

  @Override
  public boolean safeFromWordTearing() {
    return true;
  }


  @Override
  public void save(ObjectOutputStream dos) throws IOException {
    dos.writeInt(IndexType.CHUNKS.ordinal());
    dos.writeLong(mLength);
    dos.writeLong(mTotalLength);
    dos.writeInt(mChunkBits);
    dos.writeInt(mChunkSize);
    dos.writeInt(mChunkMask);
    dos.writeObject(mArray);
  }

  /**
   * Should only be called from {@link IntCreate#loadIndex(java.io.ObjectInputStream)}
   * @param ois stream to load from
   * @return index loaded from stream
   * @throws IOException if an IO error occurs
   */
  public static IntChunks loadIndex(ObjectInputStream ois) throws IOException {
    final long length = ois.readLong();
    final long totalLength = ois.readLong();
    final int bits = ois.readInt();
    final int size = ois.readInt();
    final int mask = ois.readInt();
    final int[][] data;
    try {
      data = (int[][]) ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("Unrecognized index type: " + e.getMessage());
    }
    return new IntChunks(length, bits, size, mask, data, totalLength);
  }
}


