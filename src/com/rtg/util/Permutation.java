/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import java.util.Arrays;

/**
 * Provides a mechanism for generating all the permutations of the integers
 * up to some specified bound.
 */
public class Permutation {

  private final int[] mPermutation;
  private boolean mFirst = true;

  /**
   * Construct a new permutation on the specified elements.
   * Individual elements can appear multiple times.
   *
   * @param seq elements
   */
  public Permutation(final int... seq) {
    mPermutation = Arrays.copyOf(seq, seq.length);
    Arrays.sort(mPermutation);
  }

  private void swap(final int j, final int l) {
    final int t = mPermutation[j];
    mPermutation[j] = mPermutation[l];
    mPermutation[l] = t;
  }

  private boolean step() {
    if (mFirst) {
      // Handle the initial identity permutation
      mFirst = false;
      return true;
    }
    if (mPermutation.length <= 1) {
      return false;
    }

    int j = mPermutation.length - 2;
    while (mPermutation[j] >= mPermutation[j + 1]) {
      if (--j < 0) {
        return false;
      }
    }
    int l = mPermutation.length - 1;
    while (mPermutation[j] >= mPermutation[l]) {
      --l;
    }
    swap(j, l);
    int k = j + 1;
    l = mPermutation.length - 1;
    while (k < l) {
      swap(k, l);
      ++k;
      --l;
    }
    return true;
  }

  /**
   * Step to the next element in the permutation sequence and return
   * a copy of the permutation in this position. If no further permutations
   * are available then null is returned.
   *
   * @return the permutation
   */
  public int[] next() {
    return step() ? mPermutation : null;
  }

  /**
   * Return the current permutation.
   * @return permutation
   */
  public int[] current() {
    return mPermutation;
  }

  /**
   * Print the current value of the permutation.
   *
   * @param s actual elements to print
   * @param permutation the permutation
   */
  public static void printPermutation(final String s, final int[] permutation) {
    int p = permutation.length;
    while (--p >= 0) {
      System.out.print(s.charAt(permutation[p]));
    }
  }

  /**
   * String representation of the permutation.
   *
   * @param s actual elements to print
   * @param permutation the permutation
   * @return string representation
   */
  public static String toString(final String s, final int[] permutation) {
    final StringBuilder sb = new StringBuilder();
    int p = permutation.length;
    while (--p >= 0) {
      sb.append(s.charAt(permutation[p]));
    }
    return sb.toString();
  }

  /**
   * Example use.
   *
   * @param args a bunch of integers
   */
  public static void main(final String[] args) {
    final Permutation p = new Permutation(Arrays.stream(args).mapToInt(Integer::parseInt).toArray());
    int[] r;
    long c = 0;
    while ((r = p.next()) != null) {
      System.out.println(Arrays.toString(r));
      ++c;
    }
    System.out.println("Total permutations: " + c);
  }
}
