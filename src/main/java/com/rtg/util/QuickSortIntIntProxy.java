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
package com.rtg.util;

/**
 * Quick sort proxy for paired int arrays
 */
public class QuickSortIntIntProxy implements QuickSort.SortProxy {
  protected final int[] mVals;
  protected final int[] mPairs;
  protected final boolean mAscending;

  /**
   * Constructor that sorts in ascending value
   * @param valArray values to sort on
   * @param pairArray corresponding array.
   */
  public QuickSortIntIntProxy(int[] valArray, int[] pairArray) {
    this(valArray, pairArray, true);
  }

  /**
   * Constructor
   * @param valArray values to sort on
   * @param pairArray corresponding array.
   * @param ascending true if sorting should be in ascending order.
   */
  public QuickSortIntIntProxy(int[] valArray, int[] pairArray, boolean ascending) {
    mVals = valArray;
    mPairs = pairArray;
    mAscending = ascending;
  }

  @Override
  public int compare(long index1, long index2) {
    return mAscending
        ? Integer.compare(mVals[(int) index1], mVals[(int) index2])
        : Integer.compare(mVals[(int) index2], mVals[(int) index1]);
  }

  @Override
  public long length() {
    return mVals.length;
  }

  @Override
  public void swap(long index1, long index2) {
    final int t = mVals[(int) index1];
    mVals[(int) index1] = mVals[(int) index2];
    mVals[(int) index2] = t;
    final int t2 = mPairs[(int) index1];
    mPairs[(int) index1] = mPairs[(int) index2];
    mPairs[(int) index2] = t2;
  }

}
