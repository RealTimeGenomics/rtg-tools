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
package com.rtg.util.array.objectindex;

import com.rtg.util.format.FormatInteger;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Common code used in implementing all the <code>int</code> index variants. Holds some handy
 * constants as well as the length of the index.
 *
 * @param <A> type of objects stored in array.
 */
public abstract class ObjectIndex<A> extends IntegralAbstract {

  /** Local new line convention */
  private static final String LS = System.lineSeparator();

  /**
   * Maximum number of bits that can be used when allocating an <code>int</code> array.
   */
  protected static final int MAX_BITS = 28;

  /**
   * Length of largest allocatable <code>int</code> array.
   */
  static final long MAX_LENGTH = 1L << MAX_BITS;

  /**
   * Information used in creating "chunks" in some of the implementations. Be
   * wary of changing CHUNK_BITS.
   */
  protected static final int CHUNK_BITS = 28;

  /**
   * Number of bytes in an <code>int</code>.
   */
  protected static final int INT_SIZE = 4;

  /** Number of a bits in an int. */
  private static final int INT_BITS = 32;

  /** The low order bits of a long corresponding to an int. */
  static final long INT_MASK = (1L << INT_BITS) - 1L;

  /** The bits above those used by an int. */
  static final long HIGH_MASK = ~INT_MASK;

  /** The bits from the signed bit for a int up. */
  static final long HIGH_SIGNED_MASK = ~((1L << (INT_BITS - 1)) - 1L);

  /** Number of elements in index. */
  protected long mLength;

  /**
   * @param length of the array.
   * @exception NegativeArraySizeException if length less than 0
   */
  protected ObjectIndex(final long length) {
    if (length < 0) {
      throw new NegativeArraySizeException("length=" + length);
    }
    mLength = length;
  }

  /**
   * Used when creating versions that will be remapped from disk.
   */
  protected ObjectIndex() {
  }

  /**
   * Swap the values at the two specified locations.
   *
   * @param index1 the first index to be swapped
   * @param index2 the second index to be swapped
   */
  public void swap(final long index1, final long index2) {
    // Default implementation - can often be made faster in particular
    // implementations
    final A temp = get(index1);
    set(index1, get(index2));
    set(index2, temp);
  }

  /**
   * @return the number of bytes consumed.
   */
  public long bytes() {
    return INT_SIZE * mLength;
  }

  /**
   * @return the length of the array
   */
  public long length() {
    return mLength;
  }

  private static final long STEP = 10L;

  static final FormatInteger FORMAT_INDEX = new FormatInteger(10);

  @Override
  public void toString(final StringBuilder sbInt) {
    sbInt.append("Index [").append(length()).append("]").append(LS);
    for (long i = 0; i < length(); i += STEP) {
      toString(sbInt, i, i + STEP);
    }
  }


  /**
   * String representation on one line of part of the index.
   *
   * @param sbInt StringBuilder where the output is being written
   * @param start first index written
   * @param end one following the last index written.
   */
  public void toString(final StringBuilder sbInt, final long start, final long end) {
    final long e = end > length() ? length() : end;
    boolean allNull = true;
    for (long i = start; i < e; ++i) {
      if (get(i) != null) {
        allNull = false;
        break;
      }
    }
    if (allNull) {
      return;
    }
    sbInt.append("[");
    FORMAT_INDEX.format(sbInt, start);
    sbInt.append("] ");

    for (long i = start; i < e; ++i) {
      sbInt.append(get(i));
      if (i != (e - 1)) {
        sbInt.append(", ");
      }
    }
    sbInt.append(LS);
  }

  @Override
  public boolean integrity() {
    assert mLength >= 0;
    return true;
  }

  /**
   * Get the <code>int</code> at the specified index
   *
   * @param index the index
   * @return long value
   * @throws UnsupportedOperationException if the underlying type
   * is not a <code>long</code>.
   */
  public abstract A get(final long index);

  /**
   * Set the <code>value</code> at the specified index
   *
   * @param index the index
   * @param value the value
   */
  public abstract void set(final long index, final A value);

  /**
   * For indexes backed by a file closes the file and flushes any modifications.
   * Any further attempts to access the index will throw an exception..
   * In implementations with no backing file this has no effect.
   */
  public abstract void close();
}

