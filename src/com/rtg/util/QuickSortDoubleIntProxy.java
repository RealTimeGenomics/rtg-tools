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
 * Quick sort proxy for pair of arrays
 */
public class QuickSortDoubleIntProxy implements QuickSort.SortProxy {
  private final double[] mVals;
  private final int[] mPairs;

  /**
   * Constructor
   * @param valArray values to sort on
   * @param pairArray corresponding array.
   */
  public QuickSortDoubleIntProxy(double[] valArray, int[] pairArray) {
    mVals = valArray;
    mPairs = pairArray;
  }

  @Override
  public int compare(long index1, long index2) {
    return Double.compare(mVals[(int) index2], mVals[(int) index1]);
  }

  @Override
  public long length() {
    return mVals.length;
  }

  @Override
  public void swap(long index1, long index2) {
    final double t = mVals[(int) index1];
    mVals[(int) index1] = mVals[(int) index2];
    mVals[(int) index2] = t;
    final int t2 = mPairs[(int) index1];
    mPairs[(int) index1] = mPairs[(int) index2];
    mPairs[(int) index2] = t2;
  }

  public double[] getVals() {
    return mVals;
  }

  public int[] getPairArray() {
    return mPairs;
  }
}
