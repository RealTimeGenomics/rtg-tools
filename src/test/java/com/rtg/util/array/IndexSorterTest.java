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

import com.rtg.util.PortableRandom;

import junit.framework.TestCase;

/**
 */
public class IndexSorterTest extends TestCase {

  private CommonIndex dynamic(final int[] x) {
    final CommonIndex a = com.rtg.util.array.intindex.IntCreate.createIndex(x.length);
    for (int i = 0; i < x.length; ++i) {
      a.set(i, x[i]);
    }
    return a;
  }

  private boolean equals(final int[] a, final CommonIndex b) {
    return equals(dynamic(a), b);
  }
  private boolean equals(final CommonIndex a, final CommonIndex b) {
    assertEquals(a.length(), b.length());
    for (long i = 0; i < a.length(); ++i) {
      if (a.get(i) != b.get(i)) {
        return false;
      }
    }
    return true;
  }

  public void testSort() {
    CommonIndex a = dynamic(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        final CommonIndex b = dynamic(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    IndexSorter.sort(a, b, a.length());
    assertTrue(ArrayUtils.isSortedStrict(a, a.length()));
    assertTrue(ArrayUtils.isSortedStrict(b, a.length()));

    a = dynamic(new int[] {12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1});
    IndexSorter.sort(a, b, a.length());
    assertTrue(ArrayUtils.isSortedStrict(a, a.length()));
    assertTrue(equals(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, a));
    assertTrue(equals(new int[] {12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1}, b));

    a = dynamic(new int[] {6, 5, 7, 4, 8, 3, 9, 2, 10, 1, 12});
    IndexSorter.sort(a, b, a.length());
    assertTrue(ArrayUtils.isSortedStrict(a, a.length()));
    assertFalse(ArrayUtils.isSortedStrict(b, a.length()));
    assertTrue(equals(new int[] {3, 5, 7, 9, 11, 12, 10, 8, 6, 4, 2, 1}, b));
  }

  public void testSortOneSegment() {
    CommonIndex a = dynamic(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        final CommonIndex b = dynamic(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});

    IndexSorter.sort(a, b, 3, 7);
    assertTrue(ArrayUtils.isSortedStrict(a, a.length()));
    assertTrue(ArrayUtils.isSortedStrict(b, a.length()));

    a = dynamic(new int[] {12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1});
    // sort first 3 elements
    IndexSorter.sort(a, b, 0, 3);
    assertTrue(equals(new int[] {10, 11, 12,   9, 8, 7, 6, 5, 4,  3,  2,  1}, a));
    assertTrue(equals(new int[] {3,   2,  1,   4, 5, 6, 7, 8, 9, 10, 11, 12}, b));
    // sort the middle 7 elements
    IndexSorter.sort(a, b, 3, 7);
    assertTrue(equals(new int[] {10, 11, 12,   3, 4, 5, 6, 7, 8, 9,    2,  1}, a));
    assertTrue(equals(new int[] {3,   2,  1,  10, 9, 8, 7, 6, 5, 4,   11, 12}, b));
    // sort the last two elements
    IndexSorter.sort(a, b, 10, 2);
    assertTrue(equals(new int[] {10, 11, 12,   3, 4, 5, 6, 7, 8, 9,     1,  2}, a));
    assertTrue(equals(new int[] {3,   2,  1,  10, 9, 8, 7, 6, 5, 4,    12, 11}, b));

    // sort an empty segment and check that there is no change.
    IndexSorter.sort(a, b, 0, 0);
    assertTrue(equals(new int[] {10, 11, 12,   3, 4, 5, 6, 7, 8, 9,     1,  2}, a));
    assertTrue(equals(new int[] {3,   2,  1,  10, 9, 8, 7, 6, 5, 4,    12, 11}, b));

  }

  public void testSortOneSegmentErrors() {
    final CommonIndex a = dynamic(new int[] {1, 2, 3});
        final CommonIndex b = dynamic(new int[]{1, 2, 3});
    try {
      IndexSorter.sort(a, b, -1, 2);
      fail("expected IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertEquals("Start is negative: -1", e.getMessage());
    }
    try {
      IndexSorter.sort(a, b, 1, -1);
      fail("expected IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertEquals("Length is negative: -1", e.getMessage());
    }
    try {
      IndexSorter.sort(a, b, 1, 3);
      fail("expected IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      assertEquals("Arrays are too short", e.getMessage());
    }
  }

  public void testSortNegativeLength() {
    final CommonIndex a = dynamic(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        final CommonIndex b = dynamic(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    try {
      IndexSorter.sort(a, b, -1);
      fail("Imaginery man");
    } catch (final IllegalArgumentException e) {
      // ok
    }
  }


  public void testSortZeroLength() {
    final CommonIndex a = dynamic(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        final CommonIndex b = dynamic(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    IndexSorter.sort(a, b, 0);
    assertTrue(ArrayUtils.isSortedStrict(a, a.length()));
    assertTrue(ArrayUtils.isSortedStrict(b, a.length()));
  }

  public void testSortIllegal() {
    final CommonIndex a = dynamic(new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        final CommonIndex b = dynamic(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
    try {
      IndexSorter.sort(a, b, -1);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Length is negative:-1", e.getMessage());
    }
  }

  public void testSortAtRandomPairs() {
    final PortableRandom r = new PortableRandom();
    for (int i = 0; i < 5; ++i) {
      final long l = r.nextInt(1000);
      final CommonIndex keys = com.rtg.util.array.intindex.IntCreate.createIndex(l);
            final CommonIndex pairs = com.rtg.util.array.intindex.IntCreate.createIndex(l);
      // init arrays to be the same
      for (long j = 0; j < l; ++j) {
        keys.set(j, r.nextInt(100));
        pairs.set(j, keys.get(j));
      }
      final int q = r.nextInt((int) keys.length() + 1);
      IndexSorter.sort(keys, pairs, q);
      for (long j = 0; j < q - 1; ++j) {
        assertTrue(keys.get(j) <= keys.get(j + 1));
        assertTrue(keys.get(j) == pairs.get(j));
      }
    }
  }

  public void testSortAtRandomPairsWithLimitedAction() {
    final PortableRandom r = new PortableRandom();
    for (int i = 0; i < 5; ++i) {
      final long l = 3 + r.nextInt(500);
      final CommonIndex keys = com.rtg.util.array.intindex.IntCreate.createIndex(l);
            final CommonIndex pairs = com.rtg.util.array.intindex.IntCreate.createIndex(l);
      // init arrays to be the same
      for (int j = 0; j < l; ++j) {
        keys.set(j, r.nextInt(100));
        pairs.set(j, keys.get(j));
      }
      final int k1 = (int) keys.get(keys.length() - 3);
      final int k2 = (int) keys.get(keys.length() - 2);
      final int k3 = (int) keys.get(keys.length() - 1);
      IndexSorter.sort(keys, pairs, keys.length() - 3);
      for (int j = 0; j < keys.length() - 4; ++j) {
        assertTrue(keys.get(j) <= keys.get(j + 1));
        assertTrue(keys.get(j) == pairs.get(j));
      }
      assertEquals(k1, keys.get(keys.length() - 3));
      assertEquals(k2, keys.get(keys.length() - 2));
      assertEquals(k3, keys.get(keys.length() - 1));
    }
  }

  public void testMed() {
    final CommonIndex a = com.rtg.util.array.intindex.IntCreate.createIndex(4);
    a.set(1, 16);
    a.set(2, 256);
    a.set(3, 4096);

    assertEquals(2, IndexSorter.med3(a, 1, 2, 3));
    assertEquals(2, IndexSorter.med3(a, 1, 3, 2));
    assertEquals(2, IndexSorter.med3(a, 2, 1, 3));
    assertEquals(2, IndexSorter.med3(a, 2, 3, 1));
    assertEquals(2, IndexSorter.med3(a, 3, 1, 2));
    assertEquals(2, IndexSorter.med3(a, 3, 2, 1));
  }

  public void testExceptions() {
    final CommonIndex a = dynamic(new int[] {8, 7, 6, 5, 4, 3, 2, 1});
        final CommonIndex b = dynamic(new int[]{1, 4, 9, 16});
    try {
      IndexSorter.sort(a, b, 2 * 1024 * 1024);
      fail("Expected IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      // expected
    }

    try {
      IndexSorter.sort(a, b, -1);
      fail("Expected IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
      // expected
    }
  }
}


