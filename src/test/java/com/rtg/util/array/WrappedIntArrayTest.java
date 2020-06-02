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


import junit.framework.TestCase;

/**
 */
public class WrappedIntArrayTest extends TestCase {

  /**
   * Test method for {@link WrappedIntArray}.
   */
  public final void test() {
    final int[] a = {1, 2, 3};
    final WrappedIntArray wa = new WrappedIntArray(a);
    wa.integrity();
    assertEquals(3, wa.length());
    assertEquals("[1, 2, 3]", wa.toString());
    assertEquals(1, wa.get(0));
    assertEquals(2, wa.get(1));
    assertEquals(3, wa.get(2));
    try {
      wa.get(-1);
      fail();
    } catch (final ArrayIndexOutOfBoundsException e) {
      //expected
    }
    try {
      wa.get(3);
      fail();
    } catch (final ArrayIndexOutOfBoundsException e) {
      //expected
    }
  }

  public final void testLong() {
    final long[] a = {1, 2, 3};
    final WrappedIntArray wa = new WrappedIntArray(a);
    wa.integrity();
    assertEquals(3, wa.length());
    assertEquals("[1, 2, 3]", wa.toString());
    assertEquals(1, wa.get(0));
    assertEquals(2, wa.get(1));
    assertEquals(3, wa.get(2));
  }

  public final void testPaired() {
    final int[] a = {1, 2, 3};
    final int[] b = {4, 5, 6};
    final WrappedIntArray wa = new WrappedIntArray(a, b);
    wa.integrity();
    assertEquals(6, wa.length());
    assertEquals("[1, 4, 2, 5, 3, 6]", wa.toString());
    assertEquals(1, wa.get(0));
    assertEquals(4, wa.get(1));
    assertEquals(2, wa.get(2));
    assertEquals(5, wa.get(3));
    assertEquals(3, wa.get(4));
    assertEquals(6, wa.get(5));
  }
}
