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

/**
 * Quick Sort for dynamic arrays.
 *
 */
public final class IndexSorter {

  private IndexSorter() {
  }

  /**
   * Paired sort of two <code>CommonIndex</code>s.
   *
   * @param primary array to sort
   * @param secondary array to keep in sync with <code>primary</code>
   * @param length the length of the arrays.
   * @exception IllegalArgumentException if the arrays are shorter than
   * <code>length</code>.
   */
  public static void sort(final CommonIndex primary, final CommonIndex secondary, final long length) {
    if (length < 0) {
      throw new IllegalArgumentException("Length is negative:" + length);
    }
    if (primary.length() < length || secondary.length() < length) {
      throw new IllegalArgumentException("Arrays are too short");
    }
    //System.err.println(" primary:" + primary + " secondary:" + secondary);
    sort(primary, 0, length, new Swapper(primary, secondary));
    assert ArrayUtils.isSorted(primary, length);
    //System.err.println("sorted:" + Utils.isSorted(primary, length) + " primary:" + primary + " secondary:" + secondary);
  }

  /**
   * Paired sort of a subsequence of two <code>CommonIndex</code>s.
   * This sorts only the subsequence <code>start .. start + length - 1</code>
   * and leaves all other elements unchanged.
   *
   * @param primary array to sort
   * @param secondary array to keep in sync with <code>primary</code>
   * @param start the start index of the sort
   * @param length the number of items to sort
   * @exception IllegalArgumentException if the arrays are shorter than
   * <code>length</code>.
   */
  public static void sort(final CommonIndex primary, final CommonIndex secondary,
      final long start, final long length) {
    if (length < 0) {
      throw new IllegalArgumentException("Length is negative: " + length);
    }
    if (start < 0) {
      throw new IllegalArgumentException("Start is negative: " + start);
    }
    final long end = start + length;
    if (primary.length() < end || secondary.length() < end) {
      throw new IllegalArgumentException("Arrays are too short");
    }
    sort(primary, start, length, new Swapper(primary, secondary));
  }

  /**
   * Sorts the specified sub-array of integers into ascending order. A
   * modified version of the JDK sort to handle three arrays.
   *
   * @param x The array to do comparisons on
   * @param off offset to start at
   * @param len length the sort
   * @param swapper interface to the swap method
   */
  public static void sort(final CommonIndex x, final long off, final long len, final Swapper swapper) {
//    System.err.println("CI: " + off + " len: " + len + " :: " + x);
    //System.err.println("start sort off:" + off + " length:" + len);
    // Insertion sort on smallest arrays
    if (len < 7) {
      for (long i = off; i < len + off; i++) {
        for (long j = i; j > off && x.get(j - 1) > x.get(j); j--) {
          swapper.swap(j, j - 1);
        }
      }
      //System.err.println("end insert off:" + off + " length:" + len);
     return;
    }

    // Choose a partition element, v
    long m = off + (len >> 1); // Small arrays, middle element
    if (len != 7) {
      long l = off;
      long n = off + len - 1;
      if (len > 40) { // Big arrays, pseudomedian of 9
        final long s = len / 8;
        l = med3(x, l, l + s, l + 2 * s);
        m = med3(x, m - s, m, m + s);
        n = med3(x, n - 2 * s, n - s, n);
      }
      m = med3(x, l, m, n); // Mid-size, med of 3
    }
//    System.err.println("CI pivot: " + m);
    //System.err.println("pivot:" + m);
    final long v = x.get(m);

    // Establish Invariant: v* (<v)* (>v)* v*
    long a = off;

    // Establish Invariant: v* (<v)* (>v)* v*
    long b = a;

    // Establish Invariant: v* (<v)* (>v)* v*
    long c = off + len - 1;

    // Establish Invariant: v* (<v)* (>v)* v*
    long d = c;
    while (true) {
      while (b <= c && x.get(b) <= v) {
        if (x.get(b) == v) {
          swapper.swap(a++, b);
        }
        b++;
      }
      while (c >= b && x.get(c) >= v) {
        if (x.get(c) == v) {
          swapper.swap(c, d--);
        }
        c--;
      }
      if (b > c) {
        break;
      }
      swapper.swap(b++, c--);
    }

    // Swap partition elements back to middle
    long s2;

    // Swap partition elements back to middle
    final long n2 = off + len;
    s2 = Math.min(a - off, b - a);
    vecswap(swapper, off, b - s2, s2);
    s2 = Math.min(d - c, n2 - d - 1);
    vecswap(swapper, b, n2 - s2, s2);

    // Recursively sort non-partition-elements
    if ((s2 = b - a) > 1) {
      sort(x, off, s2, swapper);
    }
    if ((s2 = d - c) > 1) {
      sort(x, n2 - s2, s2, swapper);
    }
    //System.err.println("end sort off:" + off + " length:" + len);
  }


  /**
   * Swaps vector of elements.
   *
   * @param aa first starting point
   * @param bb second starting point
   * @param n length
   */
  private static void vecswap(final Swapper s, final long aa, final long bb, final long n) {
    for (long i = 0, a = aa, b = bb; i < n; i++, a++, b++) {
      s.swap(a, b);
    }
  }


  /**
   * Returns the index of the median of the three indexed integers.
   *
   * @param x the array
   * @param a first index
   * @param b second index
   * @param c third index
   * @return the median
   */
  static long med3(final CommonIndex x, final long a, final long b, final long c) {
//    System.err.println("med3 CI: " + a + " " + b + " " + c);
    final long xa = x.get(a);
    final long xb = x.get(b);
    final long xc = x.get(c);

    return xa < xb
      ? (xb < xc ? b : xa < xc ? c : a)
      : (xb > xc ? b : xa > xc ? c : a);
  }
}


