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

import com.rtg.util.array.IndexType;
import com.rtg.util.integrity.Exam;

/**
 * Index is implemented using a straight forward long array.
 * This is so that short instances of IntIndex can be as efficient as possible.
 *
 */
public final class IntArray extends IntIndex {

  private final int[] mArray;

  /**
   * This should be called directly only in tests.
   *
   * @param length number of items to be stored.
   */
  public IntArray(final long length) {
    super(length);
    //assert length <= MAX_LENGTH;
    mArray = new int[(int) length];
  }

  private IntArray(int[] data, long length) {
    super(length);
    mArray = data;
  }


  @Override
  public int getInt(final long index) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    return mArray[ii];
  }

  @Override
  public void setInt(final long index, final int value) {
    final int ii = (int) index;
    if (ii != index) {
      throw new IndexOutOfBoundsException(String.valueOf(index));
    }
    mArray[ii] = value;
  }

  @Override
  public void swap(long index1, long index2) {
    final int ii1 = (int) index1;
    final int ii2 = (int) index2;
    final int tmp = mArray[ii1];
    mArray[ii1] = mArray[ii2];
    mArray[ii2] = tmp;
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertEquals(mArray.length, mLength);
    return true;
  }

  @Override
  public boolean safeFromWordTearing() {
    return true;
  }

  @Override
  public void save(ObjectOutputStream dos) throws IOException {
    dos.writeInt(IndexType.ARRAY.ordinal());
    dos.writeLong(mLength);
    dos.writeObject(mArray);
  }

  /**
   * Should only be called from {@link IntCreate#loadIndex(java.io.ObjectInputStream)}
   * @param ois stream to load from
   * @return index loaded from stream
   * @throws IOException if an IO error occurs
   */
  public static IntArray loadIndex(ObjectInputStream ois) throws IOException {
    final long length = ois.readLong();
    final int[] data;
    try {
      data = (int[]) ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException("Unrecognized index type: " + e.getMessage());
    }
    return new IntArray(data, length);
  }
}


