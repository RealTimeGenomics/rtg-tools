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

import java.util.Arrays;

import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Wraps an integer array so that it is immutable.
 */
public class WrappedIntArray extends IntegralAbstract implements ImmutableIntArray {

  private final int[] mArray;

  /**
   * @param array to be wrapped.
   */
  public WrappedIntArray(final int[] array) {
    mArray = array.clone();
  }

  /**
   * @param array to be wrapped.
   */
  public WrappedIntArray(final long[] array) {
    mArray = new int[array.length];
    for (int i = 0; i < array.length; i++) {
      mArray[i] = (int) array[i];
    }
  }

  /**
   * Construct lengths array where lengths for left and right arms alternate.
   * @param left lengths  of reads on left arm.
   * @param right lengths of reads on right arm.
   */
  public WrappedIntArray(final int[] left, final int[]  right) {
    assert left.length == right.length;
    final int size = left.length + right.length;
    mArray = new int[size];
    for (int i = 0; i < left.length; i++) {
      final int j = i << 1;
      mArray[j] = left[i];
      mArray[j + 1] = right[i];
    }
  }

  /**
   * Get the underlying value from the array.
   * @param index into the array.
   * @return the value.
   */
  @Override
  public int get(final int index) {
    return mArray[index];
  }

  /**
   * Get the length of the wrapped array.
   * @return the length of the wrapped array.
   */
  @Override
  public int length() {
    return mArray.length;
  }

  @Override
  public void toString(final StringBuilder sb) {
    sb.append(Arrays.toString(mArray));
  }

  @Override
  public boolean integrity() {
    Exam.assertTrue(mArray != null);
    return true;
  }
}
