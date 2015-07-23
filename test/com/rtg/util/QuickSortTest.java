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

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Test Class
 */
public class QuickSortTest extends TestCase {


  public void testSort() {
    final PortableRandom pr = new PortableRandom();
    final long seed = pr.getSeed();
    try {
      final int[] unsorted = new int[100000];
      for (int i = 0; i < unsorted.length; i++) {
        unsorted[i] = Math.abs(pr.nextInt());
      }
      final int[] unsorted2 = Arrays.copyOf(unsorted, unsorted.length);
      final QuickSort.SortProxy proxy = new QuickSort.SortProxy() {

        @Override
        public int compare(long index1, long index2) {
          return Integer.valueOf(unsorted[(int) index1]).compareTo(unsorted[(int) index2]);
        }

        @Override
        public void swap(long index1, long index2) {
          final int temp = unsorted[(int) index1];
          unsorted[(int) index1] = unsorted[(int) index2];
          unsorted[(int) index2] = temp;
        }

        @Override
        public long length() {
          return unsorted.length;
        }

        @Override
        public String toString() {
          return Arrays.toString(unsorted);

        }

      };
      QuickSort.sort(proxy);
      Arrays.sort(unsorted2);
      assertTrue(Arrays.equals(unsorted2, unsorted));
      assertTrue(QuickSort.isSorted(proxy));
    } catch (RuntimeException e) {
      throw new RuntimeException("Seed: " + seed + " has discovered an error", e);
    }
  }

}
