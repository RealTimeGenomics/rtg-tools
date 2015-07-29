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
package com.rtg.util.array.objectindex;


import junit.framework.TestCase;

/**
 * Tests for <code>ObjectIndex</code>.
 *
 */
public abstract class AbstractObjectIndexTest extends TestCase {

  protected static final int STEP = 1000;
  protected static final int LENGTH = 1000007; //this isnt a multiple of two because then it might not catch many tricky cases

  /** Local new line convention */
  private static final String LS = System.lineSeparator();

  protected abstract ObjectIndex<Integer> create(final long length);

  protected abstract ObjectIndex<Integer> create(final long length, final int bits);

  public void testMasks() {
    assertEquals(ObjectIndex.INT_MASK & ObjectIndex.HIGH_MASK, 0);
    assertEquals(ObjectIndex.HIGH_SIGNED_MASK & ObjectIndex.HIGH_MASK, ObjectIndex.HIGH_MASK);
    assertEquals(ObjectIndex.HIGH_SIGNED_MASK & ObjectIndex.INT_MASK, 1L << 31);
  }

  private static final String TO_STR = ""
    + "Index [100]" + LS
    + "[         0] " + null + ", 1, 2, " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + "" + LS
    + "[        10] " + null + ", " + null + ", 12, " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + "" + LS
    + "[        50] " + null + ", " + null + ", 52, " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + "" + LS;

  public void testToString() {
    final ObjectIndex<Integer> index = create(100);
    index.set(1, 1);
    index.set(2, 2);
    index.set(12, 12);
    index.set(52, 52);
    final String str = index.toString();
    assertEquals(TO_STR, str);
  }

  private static final String TO_STR15 = ""
    + "Index [15]" + LS
    + "[         0] " + null + ", 1, 2, " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + ", " + null + "" + LS
    + "[        10] " + null + ", " + null + ", 12, " + null + ", " + null + "" + LS;

  public void testToString15() {
    final ObjectIndex<Integer> index = create(15);
    index.set(1, 1);
    index.set(2, 2);
    index.set(12, 12);
    final String str = index.toString();
    assertEquals(TO_STR15, str);
  }

  public void testShortToString() {
    final ObjectIndex<Integer> index = create(3);
    index.set(1, 1);
    final String str = index.toString();
    assertEquals("Index [3]" + LS + "[         0] " + null + ", 1, " + null + "" + LS, str);
  }

  public void testLength() {
    final int le = 42000;
    final ObjectIndex<Integer> a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(4 * le, a.bytes());
    a.close();
    a.integrity();
  }

  public void testLength0() {
    final int le = 0;
    final ObjectIndex<Integer> a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(4 * le, a.bytes());
    a.close();
    a.integrity();
  }

  public void testBadLength1() {
    try {
      create(Integer.MIN_VALUE);
      fail("Expected NegativeArraySizeException");
    } catch (final NegativeArraySizeException e) {
      // expected
    }
    try {
      create(-1);
      fail("Expected NegativeArraySizeException");
    } catch (final NegativeArraySizeException e) {
      // expected
    }
  }

  public void testIntensiveSetGet() {
    //needed for subtle errors in underlying mapping in disk backed cases
    final int length = 100; // > 2^5 (see ShortDiskChunksTest and ShortChunksTest - also not a multiple of 2^3
    final ObjectIndex<Integer> a = create(length, 3);
    a.integrity();
    assertEquals(length, a.length());
    for (int i = 0; i < a.length(); i++) {
      assertEquals(null, a.get(i));
      final int j = i * 3;
      a.set(i, j);
    }
    for (int i = 0; i < a.length(); i++) {
      final int j = i * 3;
      assertEquals(Integer.valueOf(j), a.get(i));
    }
    a.close();
    a.integrity();
  }

  public void testGetSetLong() {
    final ObjectIndex<Integer> a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(null, a.get(i));
      final int j = i * 3;
      a.set(i, j);
    }
    for (int i = 0; i < a.length(); i += STEP) {
      final int j = i * 3;
      assertEquals(Integer.valueOf(j), a.get(i));
    }
    a.close();
    a.integrity();
    try {
      a.set(0L, 0);
      fail("Expected Exception");
    } catch (final RuntimeException e) {
      // expected
    }
    try {
      a.get(0L);
      fail("Expected Exception");
    } catch (final RuntimeException e) {
      // expected
    }
  }

  public void testSwap() {
    final ObjectIndex<Integer> a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(null, a.get(i));
      final int j = i * 3;
      a.set(i, j);
      assertEquals(Integer.valueOf(j), a.get(i));
    }
    for (int i = 0; i < a.length() - 1; i += STEP) {
      final int j = i * 3;
      assertEquals(i + ":" + 0, null, a.get(i + 1));
      assertEquals(i + ":" + j, Integer.valueOf(j), a.get(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + j, Integer.valueOf(j), a.get(i + 1));
      assertEquals(i + ":" + 0, null, a.get(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + 0, null, a.get(i + 1));
      assertEquals(i + ":" + j, Integer.valueOf(j), a.get(i));
    }
    a.close();
    a.integrity();
    try {
      a.swap(0L, 0L);
      fail("Expected Exception");
    } catch (final RuntimeException e) {
      // expected
    }
  }


  public void testEdges() {
    final ObjectIndex<Integer> a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    a.set(LENGTH - 1, 1);
    assertEquals(Integer.valueOf(1), a.get(LENGTH - 1));
    checkLimits(a, LENGTH);
    a.close();
    a.integrity();
    checkClose(a, LENGTH);
  }

  private void checkLimits(final ObjectIndex<Integer> a, final int length) {
    a.set(length - 1, 42);
    assertEquals(Integer.valueOf(42), a.get(length - 1));
    try {
      a.get(length);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.set(length, 0);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    a.set(0, 1);
    assertEquals(Integer.valueOf(1), a.get(0));
    try {
      a.get(-1);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.set(-1, 0);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
  }

  private void checkClose(final ObjectIndex<Integer> a, final int length) {
    assertEquals(length, a.length());
    assertEquals(4L * length, a.bytes());
    try {
      a.set(length - 1, 42);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      a.get(length - 1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      a.set(0, 1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      a.get(0);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }
}


