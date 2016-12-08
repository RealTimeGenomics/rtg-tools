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
package com.rtg.util.array;

import com.rtg.util.format.FormatInteger;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Common implementation between the array classes.
 * Remembers the length and provides some common methods.
 *
 */
public abstract class AbstractIndex extends IntegralAbstract implements CommonIndex {

  /** Number of elements in index. */
  protected long mLength;


  /**
   * @param length of the array.
   */
  public AbstractIndex(final long length) {
    if (length < 0) {
      throw new NegativeArraySizeException("length=" + length);
    }
    mLength = length;
  }

  @Override
  public void swap(final long index1, final long index2) {
    // Default implementation - can often be made faster in particular
    // implementations
    final long temp = get(index1);
    set(index1, get(index2));
    set(index2, temp);
  }

  @Override
  public final long length() {
    return mLength;
  }

  /**
   * @param offset putative offset into index.
   * @exception IndexOutOfBoundsException if offset negative or &ge; length.
   */
  protected void check(final long offset) {
    if (offset < 0 || offset >= mLength) {
      throw new IndexOutOfBoundsException("Index out of bounds:" + offset + " : " + mLength);
    }
  }

  private static final long STEP = 10L;

  @Override
  public void toString(final StringBuilder sb) {
    sb.append("Index [").append(length()).append("]").append(LS);
    for (long i = 0; i < length(); i += STEP) {
      toString(sb, i, i + STEP);
    }
  }

  protected abstract FormatInteger formatValue();

  void toString(final StringBuilder sbLong, final long start, final long end) {
    final long e = end > length() ? length() : end;
    boolean allZero = true;
    for (long i = start; i < e; ++i) {
      if (get(i) != 0L) {
        allZero = false;
        break;
      }
    }
    if (allZero) {
      return;
    }
    sbLong.append("[");
    sbLong.append(start);
    sbLong.append("] ");

    for (long i = start; i < e; ++i) {
      formatValue().format(sbLong, get(i));
      if (i != (e - 1)) {
        sbLong.append(", ");
      }
    }
    sbLong.append(LS);
  }


  /**
   * String representation on one line of part of the index.
   *
   * @param sbLong StringBuilder where the output is being written
   * @param start first index written
   * @param end one following the last index written.
   */
  public void dumpString(final StringBuilder sbLong, final long start, final long end) {
    sbLong.append("[");
    sbLong.append(start);
    sbLong.append("] ");

    for (long i = start; i < end; ++i) {
      sbLong.append("  ");
      sbLong.append(FormatInteger.toBits(get(i)));
    }
    sbLong.append(LS);
  }


  @Override
  public boolean integrity() {
    assert mLength >= 0;
    return true;
  }

  //The following are exposed in ExtensibleIndex

  /**
   * Trim the length of the array deleting any existing elements &ge; length.
   * @param length for array to be set to. Must be &le; <code>length()</code>.
   */
  public void trim(long length) {
    throw new UnsupportedOperationException();
  }

  /**
   * Reduce the underlying memory used as far as possible to accomodate the current length.
   */
  public void trim() {
    trim(length());
  }

  /**
   * Extend the array to support at least the specified size.
   * @param size new size
   */
  public void extendTo(long size) {
    if (size > length()) {
      extendBy(size - length());
    }
  }

  /**
   * Allocate an additional increment entries.
   * @param increment minimum number of new entries to be allocated.
   * @return initial position of the start of the newly allocated entries.
   */
  public long extendBy(long increment) {
    throw new UnsupportedOperationException();
  }

  /**
   * Extend length by one and set newly created location to
   * value.
   * @param value the value as a long, regardless of the underlying type
   */
  public void append(long value) {
    final long offset = length();
    extendBy(1);
    set(offset, value);
  }

  /**
   * Sets the value at the specified position.
   * Checks that the value is compatible with the underlying storage type.
   * In particular if the underlying storage type is n bits then a positive value
   * must have bit positions (n+1)..64 (inclusive, counting from 1) all 0.
   * If it is negative then it must have positions (n+1)..64 (inclusive, counting from 1) all 1.
   * This ensures that information is not lost.
   * Sign bits are preserved across <code>setsigned</code> and <code>getSigned</code> calls.
   * This is not true of set and get calls where the low order bit pattern is preserved.
   *
   * @param offset the position in this index
   * @param value the value as a long, regardless of the underlying type
   */
  public void setSigned(long offset, long value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Extend length by one and set newly created location to
   * value.
   * Sign bits are preserved across <code>appendSigned</code> and <code>getSigned</code> calls.
   * @param value the value as a long, regardless of the underlying type
   */
  public void appendSigned(long value) {
    final long offset = length();
    extendBy(1);
    setSigned(offset, value);
  }


}
