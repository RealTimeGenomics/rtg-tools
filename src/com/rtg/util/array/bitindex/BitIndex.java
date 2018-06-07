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
package com.rtg.util.array.bitindex;

import com.rtg.util.MathUtils;
import com.rtg.util.array.AbstractIndex;
import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.array.longindex.LongChunks;
import com.rtg.util.format.FormatInteger;
import com.rtg.util.integrity.Exam;


/**
 * This implements an array of small unsigned integer values,
 * packing several values into an array of longs.
 * The number of bits used for each value is always a power of two bits,
 * so this implementation is more wasteful on memory than PackedIndex.
 *
 */
public final class BitIndex extends AbstractIndex implements ExtensibleIndex {

  private static final FormatInteger FORMAT_INTEGER = new FormatInteger(5);

  /** The main array, in which all values are stored. */
  private final LongChunks mArray;

  /** Width of each bit-field */
  private final int mBits;

  /** Shifting by this is equivalent to multiplying by <code>mBits</code>. */
  private final int mBitsShift;

  /** Equals 2 to the power of <code>mBits</code> minus 1. */
  private final long mMask;

  /** Where the sign bits will be */
  private final long mUpperMask;

  /** actual sign bit */
  private final long mSignBit;

  /** Number of bit-fields packed into each long */
  private final int mNumFields;

  /** The number of bits used to access the fields within each long. */
  private final int mFieldIndexBits;

  /**
   * The type of array to use inside the index
   */
  public enum IndexType {
    /** Choose chunked or single based on the length */
    DEFAULT,
    /** Use an array of arrays */
    CHUNKED,
    /** Use a single array */
    SINGLE
  }

  /**
   * @param length number of items.
   * @param bits number of bits to be stored in each item.
   * @exception NegativeArraySizeException if length less than 0
   */
  public BitIndex(final long length, final int bits) {
    super(length);
    if (bits < 1) {
      throw new IllegalArgumentException("Illegal range value=" + bits);
    }
    mBits = roundUpBits(bits);
    mBitsShift = MathUtils.ceilPowerOf2Bits(mBits - 1);
    assert mBits == (1 << mBitsShift);
    assert 5 * mBits == (5 << mBitsShift);

    mMask = MathUtils.round(Math.pow(2, mBits)) - 1;
    // because we have rounded up mBits to a power of 2, mNumFields will be a power of two.
    mNumFields = 64 / mBits;
    assert mNumFields == roundUpBits(mNumFields);
    mFieldIndexBits = MathUtils.ceilPowerOf2Bits(mNumFields - 1);
    //System.err.println("fieldIndexBits=" + mFieldIndexBits);
    assert mNumFields == (1 << mFieldIndexBits);
    final long llen = length / mNumFields + 1;

    // always use LongChunks, otherwise the JIT gets confused.
    mArray = new com.rtg.util.array.longindex.LongChunks(llen);
    // since we always use long chunks, we may as well be extensible
    mSignBit = (mMask + 1) >>> 1;
    mUpperMask = ~mMask;
    assert globalIntegrity();
  }

  @Override
  public long extendBy(long length) {
    final long targetLength = mLength + length;
    final long llen = targetLength / mNumFields + 1;
    final long innerExtend = llen - mArray.length();
    mArray.extendBy(innerExtend);
    final long res = mLength;
    mLength = targetLength;
    return res;
  }

  @Override
  public void trim(long length) {
    final long llen = length / mNumFields + 1;
    mArray.trim(llen);
    mLength = length;
  }

  @Override
  public long getSigned(long offset) {
    final long val = get(offset);
    if ((val & mSignBit) != 0) {
      return val | mUpperMask;
    }
    return val;
  }

  @Override
  public void setSigned(long offset, long value) {
    assert ((value >= 0 ? value : -value) & mUpperMask) == 0L : value;
    set(offset, value & mMask);
  }



  /**
   * Round up a number to the next power of 2.
   *
   * @param bits 1..64
   * @return the smallest power of 2 that is greater than or equal to <code>bits</code>.
   */
  public static int roundUpBits(final int bits) {
    return 1 << MathUtils.ceilPowerOf2Bits(bits - 1);
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
    super.integrity();
    Exam.assertTrue(1 <= mBits && mBits <= 64);
    Exam.assertTrue("mBits=" + mBits, mBits == 1 || mBits == 2 || mBits == 4 || mBits == 8 || mBits == 16 || mBits == 32 || mBits == 64);
    return true;
  }

  /**
   * Get the <code>long</code> at the specified index
   *
   * @param index the index
   * @return long value
   * @throws UnsupportedOperationException if the underlying type
   * is not a <code>long</code>.
   */
  @Override
  public long get(final long index) {
    final long chunk = index >> mFieldIndexBits;
    final long value = mArray.get(chunk);
    final int  shift = ((int) index) << mBitsShift;
    return (value >>> shift) & mMask;
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
    assert 0 <= value && value < (1L << mBits) : value;
    final long chunk = index >> mFieldIndexBits;
    final long oldValue = mArray.get(chunk);
    final int shift = ((int) index) << mBitsShift;
    final long newValue = (oldValue & ~(mMask << shift)) | (value << shift);
    mArray.set(chunk, newValue);
  }

  @Override
  protected FormatInteger formatValue() {
    return FORMAT_INTEGER;
  }

  @Override
  public boolean safeFromWordTearing() {
    return false;
  }
}
