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
package com.rtg.util.array.packedindex;

import com.rtg.util.MathUtils;
import com.rtg.util.array.AbstractIndex;
import com.rtg.util.array.bitindex.BitIndex.IndexType;
import com.rtg.util.array.longindex.LongIndex;
import com.rtg.util.format.FormatInteger;
import com.rtg.util.integrity.Exam;


/**
 * This implements an array of small unsigned integer values,
 * packing as many values as possible into an array of longs.
 * It uses every bit, so a few values will be split across two longs.
 * This is done so that no divisions or modulo operations are needed,
 * and it also minimizes the space used.
 *
 */
public final class PackedIndex extends AbstractIndex {

  /** The main array, in which all values are stored. */
  private final LongIndex mArray;

  /** Number of bits in a long. */
  private static final int LONG_BITS = 64;

  /** The number of bits you need to shift by to divide by 64. */
  private static final int DIVIDE_BY_64 = 6;

  private static final int MODULO_64 = 63;

  /** Number of values that can be stored in each bit-field. */
  private final long mRange;

  /** Width of each bit-fields */
  private final int mBits;

  /** Equals 2 to the power of <code>mBits</code> minus 1. */
  private final long mMask;

  /**
   * @param length number of items.
   * @param range number of values to be stored in each item.
   * @param type the type of array to use inside the index
   * @exception NegativeArraySizeException if length less than 0
   */
  public PackedIndex(final long length, final long range, final IndexType type) {
    super(length);
    if (range < 2) {
      throw new IllegalArgumentException("Illegal range value=" + range);
    }
    mRange = range;
    mBits = MathUtils.ceilPowerOf2Bits(range - 1);
    mMask = (1L << mBits) - 1;
    final long llen = length * mBits / LONG_BITS + 1;
    // always use LongChunks, otherwise the JIT gets confused.
    mArray = new com.rtg.util.array.longindex.LongChunks(llen);
//    mArray = type == IndexType.DEFAULT ? LongCreate.createIndex(llen)
//        : type == IndexType.CHUNKED ? new com.rtg.util.array.longindex.LongChunks(llen)
//        : new com.rtg.util.array.longindex.Array(llen);
  }

  /**
   * @return the number of bytes consumed.
   */
  @Override
  public long bytes() {
    return mArray.bytes();
  }

  /**
   * Get the number of bits that each value is packed into.
   * @return an integer from 1 to 31.
   */
  public int getBits() {
    return mBits;
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mRange <= MathUtils.round(Math.pow(2, mBits)));
    assert mLength >= 0;
    return true;
  }

  @Override
  public long get(final long index) {
    final long bitPos = index * mBits;
    final int  shift = (int) bitPos & MODULO_64;
    final long firstLong = bitPos >> DIVIDE_BY_64;
    final long secondLong = (bitPos + mBits) >> DIVIDE_BY_64;
    final long long1 = mArray.get(firstLong);
    if (firstLong == secondLong) {
      // only need to read one long
      return (long1 >>> shift) & mMask;
    } else {
      final long long2 = mArray.get(secondLong); // might be the same long, but we don't care.
      // this is tricky: a good example of how it works is mBits=7 and shift=60.
      return ((long2 << (64 - shift)) | (long1 >>> shift)) & mMask;
    }
  }

  /**
   * Set the <code>long</code> at the specified index
   *
   * @param index the index
   * @param value the value
   * @throws UnsupportedOperationException if the underlying type
   * is not a <code>long</code>.
   */
  @Override
  public void set(final long index, final long value) {
    assert 0 <= value && value < mRange;
    final long bitPos = index * mBits;
    final int  shift = (int) bitPos & MODULO_64;
    final long firstLong = bitPos >> DIVIDE_BY_64;
    final long secondLong = (bitPos + mBits) >> DIVIDE_BY_64;
    final long long1 = mArray.get(firstLong);
    if (firstLong == secondLong) {
      // it all fits into one long
      final long newLong1 = (long1 & ~(mMask << shift)) | (value << shift);
      mArray.set(firstLong, newLong1);
    } else {
      // it spans two different longs
      final long long2 = mArray.get(secondLong);
      final int  shift2 = 64 - shift;
      final long newLong1 = (long1 & ~(mMask << shift)) | (value << shift);
      final long newLong2 = (long2 & ~(mMask >> shift2)) | (value >> shift2);
      mArray.set(firstLong, newLong1);
      mArray.set(secondLong, newLong2);
    }
  }

  @Override
  protected FormatInteger formatValue() {
    if (mBits <= 16) {
      return new FormatInteger(5);
    }
    if (mBits <= 32) {
      return new FormatInteger(10);
    }
    return new FormatInteger(20);
  }

  @Override
  public boolean safeFromWordTearing() {
    return false;
  }
}

