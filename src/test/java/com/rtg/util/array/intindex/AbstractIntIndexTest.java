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
package com.rtg.util.array.intindex;

import com.rtg.util.array.CommonIndex;

import junit.framework.TestCase;

/**
 * Tests for <code>IntIndex</code>.
 *
 */
public abstract class AbstractIntIndexTest extends TestCase {

  protected static final int STEP = 1000;
  protected static final int LENGTH = 1000007; //this isnt a multiple of two because then it might not catch many tricky cases

  /** Local new line convention */
  private static final String LS = System.lineSeparator();

  protected abstract IntIndex create(long length);

  protected abstract IntIndex create(long length, int bits);

  public void testMasks() {
    assertEquals(IntIndex.INT_MASK & IntIndex.HIGH_MASK, 0);
    assertEquals(IntIndex.HIGH_SIGNED_MASK & IntIndex.HIGH_MASK, IntIndex.HIGH_MASK);
    assertEquals(IntIndex.HIGH_SIGNED_MASK & IntIndex.INT_MASK, 1L << 31);
  }

  public void testUnsigned() {
    final CommonIndex ix = create(1);
    for (int i = 0; i < 32; ++i) {
      final long v = 1L << i;
      ix.set(0, v);
      assertEquals(v, ix.get(0));
    }
  }

  private static final String TO_STR = ""
    + "Index [100]" + LS
    + "[0]          0,          1,          2,          0,          0,          0,          0,          0,          0,          0" + LS
    + "[10]          0,          0,         12,          0,          0,          0,          0,          0,          0,          0" + LS
    + "[50]          0,          0,         52,          0,          0,          0,          0,          0,          0,          0" + LS;

  public void testToString() {
    final IntIndex index = create(100);
    index.setInt(1, 1);
    index.setInt(2, 2);
    index.setInt(12, 12);
    index.setInt(52, 52);
    final String str = index.toString();
    assertEquals(TO_STR, str);
  }

  private static final String TO_STR15 = ""
    + "Index [15]" + LS
    + "[0]          0,          1,          2,          0,          0,          0,          0,          0,          0,          0" + LS
    + "[10]          0,          0,         12,          0,          0" + LS;

  public void testToString15() {
    final IntIndex index = create(15);
    index.setInt(1, 1);
    index.setInt(2, 2);
    index.setInt(12, 12);
    final String str = index.toString();
    assertEquals(TO_STR15, str);
  }

  public void testShortToString() {
    final IntIndex index = create(3);
    index.setInt(1, 1);
    final String str = index.toString();
    assertEquals("Index [3]" + LS + "[0]          0,          1,          0" + LS, str);
  }

  public void testLength() {
    final int le = 42000;
    final IntIndex a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(4 * le, a.bytes());
    a.integrity();
  }

  public void testLength0() {
    final int le = 0;
    final IntIndex a = create(le);
    a.integrity();
    assertEquals(le, a.length());
    assertEquals(4 * le, a.bytes());
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
    final IntIndex a = create(length, 3);
    a.integrity();
    assertEquals(length, a.length());
    for (int i = 0; i < a.length(); ++i) {
      assertEquals(0, a.getInt(i));
      final int j = i * 3;
      a.setInt(i, j);
    }
    for (int i = 0; i < a.length(); ++i) {
      final int j = i * 3;
      assertEquals(j, a.getInt(i));
    }
    a.integrity();
  }

  public void testGetSetLong() {
    final IntIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getInt(i));
      final int j = i * 3;
      a.setInt(i, j);
    }
    for (int i = 0; i < a.length(); i += STEP) {
      final int j = i * 3;
      assertEquals(j, a.getInt(i));
      assertEquals(j, a.get(i));
    }
  }

  public void testSwap() {
    final IntIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    for (int i = 0; i < a.length(); i += STEP) {
      assertEquals(0, a.getInt(i));
      final int j = i * 3;
      a.setInt(i, j);
      assertEquals(j, a.getInt(i));
    }
    for (int i = 0; i < a.length() - 1; i += STEP) {
      final int j = i * 3;
      assertEquals(i + ":" + 0, 0, a.getInt(i + 1));
      assertEquals(i + ":" + j, j, a.getInt(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + j, j, a.getInt(i + 1));
      assertEquals(i + ":" + 0, 0, a.getInt(i));
      a.swap(i, i + 1);
      assertEquals(i + ":" + 0, 0, a.getInt(i + 1));
      assertEquals(i + ":" + j, j, a.getInt(i));
    }
  }


  public void testEdges() {
    final IntIndex a = create(LENGTH);
    a.integrity();
    assertEquals(LENGTH, a.length());
    a.setInt(LENGTH - 1, 1);
    assertEquals(1L, a.getInt(LENGTH - 1));
    checkLimits(a, LENGTH);
  }

  private void checkLimits(final IntIndex a, final int length) {
    a.setInt(length - 1, 42);
    assertEquals(42, a.getInt(length - 1));
    try {
      a.getInt(length);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setInt(length, 0);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    a.setInt(0, 1);
    assertEquals(1L, a.getInt(0));
    try {
      a.getInt(-1);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
    try {
      a.setInt(-1, 0);
      fail("Exception expected");
    } catch (final RuntimeException | AssertionError e) {
      //expected
    }
  }
}


