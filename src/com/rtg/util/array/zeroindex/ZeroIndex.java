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
package com.rtg.util.array.zeroindex;

import com.rtg.util.array.AbstractIndex;
import com.rtg.util.array.ExtensibleIndex;
import com.rtg.util.format.FormatInteger;

/**
 * All positions in the index return the same value. That is this uses zero bits per entry.
 *
 */
public final class ZeroIndex extends AbstractIndex implements ExtensibleIndex {

  private static final FormatInteger FORMAT_INTEGER = new FormatInteger(5);

  private final long mConstant;

  /**
   * @param length number of items.
   * @param constant value stored at each location.
   * @exception NegativeArraySizeException if length less than 0
   */
  public ZeroIndex(final long length, final long constant) {
    super(length);
    mConstant = constant;
    assert globalIntegrity();
  }

  @Override
  public long bytes() {
    return 0L;
  }

  @Override
  public long get(final long index) {
    check(index);
    return mConstant;
  }

  @Override
  public void set(final long index, final long value) {
    assert value == mConstant; // : value + ":" + mConstant;
  }

  @Override
  protected FormatInteger formatValue() {
    return FORMAT_INTEGER;
  }

  @Override
  public long extendBy(long increment) {
    assert increment >= 0;
    final long res = mLength;
    mLength += increment;
    return res;
  }

  @Override
  public void extendTo(long size) {
    mLength = Math.max(size, mLength);
  }

  @Override
  public void trim(long length) {
    assert length >= 0;
    mLength = length;
  }

  @Override
  public long getSigned(long offset) {
    check(offset);
    return mConstant;
  }

  @Override
  public void setSigned(long offset, long value) {
    assert value == mConstant; // : value + ":" + mConstant;
  }

  @Override
  public void toString(StringBuilder sb) {
    sb.append("Index [").append(length()).append("]").append(LS);
    if (mConstant != 0) {
      sb.append(mConstant).append(" constant").append(LS);
    }
  }

  @Override
  public boolean safeFromWordTearing() {
    return true;
  }
}
