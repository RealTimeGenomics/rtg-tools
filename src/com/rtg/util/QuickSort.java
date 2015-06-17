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
 * Quick Sort for arrays.
 *
 */
public final class QuickSort {

  /**
   * Provides abstraction for items to be sorted
   */
  public interface SortProxy {
    /**
     * See {@link java.util.Comparator}
     * @param index1 index of first item
     * @param index2 index of second item
     * @return the comparison int
     */
    int compare(long index1, long index2);

    /**
     * Swap values of items at given positions
     * @param index1 index of first item
     * @param index2 index of second item
     */
    void swap(long index1, long index2);

    /**
     * Total number of items to be sorted
     * @return the number
     */
    long length();
  }

  private QuickSort() {
  }

  /**
   * This sorts the collection of items through a proxy.
   *
   * @param proxy proxy for sort
   */
  public static void sort(SortProxy proxy) {
    sort(proxy, 0, proxy.length());
  }

  /**
   * Sorts the specified sub-collection of items through a proxy
   *
   * @param proxy proxy for the sort
   * @param off offset to start at
   * @param len length the sort
   */
  public static void sort(SortProxy proxy, final long off, final long len) {
//    System.err.println("proxy: " + off + " len: " + len + " :: " + proxy);
    //System.err.println("start sort off:" + off + " length:" + len);
    // Insertion sort on smallest arrays
    if (len < 7) {
      for (long i = off; i < len + off; i++) {
        for (long j = i; j > off && proxy.compare(j - 1, j) > 0; j--) {
          proxy.swap(j, j - 1);
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
        l = med3(proxy, l, l + s, l + 2 * s);
        m = med3(proxy, m - s, m, m + s);
        n = med3(proxy, n - 2 * s, n - s, n);
      }
      m = med3(proxy, l, m, n); // Mid-size, med of 3
    }
    //System.err.println("pivot:" + m);

    // Establish Invariant: v* (<v)* (>v)* v*
    long a = off;

    // Establish Invariant: v* (<v)* (>v)* v*
    long b = a;

    // Establish Invariant: v* (<v)* (>v)* v*
    long c = off + len - 1;

    // Establish Invariant: v* (<v)* (>v)* v*
    long d = c;
    while (true) {
      int bcompm;
      while (b <= c && (bcompm = proxy.compare(b, m)) <= 0) {
        if (bcompm == 0) {
          proxy.swap(a++, b);
          if (b == m) {
            m = a - 1;
          }
        }
        b++;
      }
      int ccompm;
      while (c >= b && (ccompm = proxy.compare(c, m)) >= 0) {
        if (ccompm == 0) {
          proxy.swap(c, d--);
          if (c == m) {
            m = d + 1;
          }
        }
        c--;
      }
      if (b > c) {
        break;
      }
      proxy.swap(b++, c--);
    }

    // Swap partition elements back to middle
    long s2;

    // Swap partition elements back to middle
    final long n2 = off + len;
    s2 = Math.min(a - off, b - a);
    vecswap(proxy, off, b - s2, s2);
    s2 = Math.min(d - c, n2 - d - 1);
    vecswap(proxy, b, n2 - s2, s2);

    // Recursively sort non-partition-elements
    if ((s2 = b - a) > 1) {
      sort(proxy, off, s2);
    }
    if ((s2 = d - c) > 1) {
      sort(proxy, n2 - s2, s2);
    }
    //System.err.println("end sort off:" + off + " length:" + len);
  }

  /**
   * Check if collection is sorted
   * @param proxy the proxy for the collection
   * @return true if sorted
   */
  public static boolean isSorted(SortProxy proxy) {
    for (long i = 1; i < proxy.length(); i++) {
      if (proxy.compare(i - 1, i) > 0) {
        return false;
      }
    }
    return true;
  }
  /**
   * Swaps vector of elements.
   *
   * @param s the proxy
   * @param aa first starting point
   * @param bb second starting point
   * @param n length
   */
  private static void vecswap(final SortProxy s, final long aa, final long bb, final long n) {
    for (long i = 0, a = aa, b = bb; i < n; i++, a++, b++) {
      s.swap(a, b);
    }
  }


  /**
   * Returns the index of the median of the three indexed integers.
   *
   * @param x the proxy
   * @param a first index
   * @param b second index
   * @param c third index
   * @return the median
   */
  static long med3(final SortProxy x, final long a, final long b, final long c) {
//    System.err.println("med3 proxy: " + a + " " + b + " " + c);
    return x.compare(a, b) < 0
      ? (x.compare(b, c) < 0 ? b : x.compare(a, c) < 0 ? c : a)
      : (x.compare(b, c) > 0 ? b : x.compare(a, c) > 0 ? c : a);
  }
}


