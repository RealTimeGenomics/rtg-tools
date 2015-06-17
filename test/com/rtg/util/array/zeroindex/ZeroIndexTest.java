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
package com.rtg.util.array.zeroindex;

import static com.rtg.util.StringUtils.LS;

import junit.framework.TestCase;

/**
 */
public class ZeroIndexTest extends TestCase {

  public void testLengthNegative() {
    checkError(-1, "length=-1");
  }

  public void testLengthEmpty() {
    final ZeroIndex index = ZeroCreate.createIndex(0);
    // test toString() when all zeroes
    assertEquals("Index [0]" + LS, index.toString());
    assertEquals(0, index.bytes());
    assertEquals("     ", index.formatValue().blanks());
  }

  public void test() {
    final ZeroIndex index = ZeroCreate.createIndex(2);
    index.set(1, 0);
    index.setSigned(1, 0);
    try {
      index.get(2);
      fail();
    } catch (final IndexOutOfBoundsException e) {
      assertEquals("Index out of bounds:2 : 2", e.getMessage());
    }
    try {
      index.get(-1);
      fail();
    } catch (final IndexOutOfBoundsException e) {
      assertEquals("Index out of bounds:-1 : 2", e.getMessage());
    }
    assertEquals(0, index.get(0));
    assertEquals(0, index.getSigned(0));
    assertEquals("Index [2]" + LS, index.toString());
  }

  public void testConstant() {
    final ZeroIndex index = new ZeroIndex(2, -42);
    assertEquals(-42, index.get(0));
    assertEquals(-42, index.getSigned(0));
    assertEquals("Index [2]" + LS + "-42 constant" + LS, index.toString());
  }

  public void checkError(final long length, final String expected) {
    try {
      ZeroCreate.createIndex(length);
      fail("expected exception: " + expected);
    } catch (final Exception e) {
      assertEquals(expected, e.getMessage());
    }
  }

  public void testExtend() {
    final ZeroIndex index = ZeroCreate.createIndex(2);
    assertEquals(2, index.length());
    assertEquals(2, index.extendBy(3));
    assertEquals(5, index.length());
  }

  public void testTrim() {
    final ZeroIndex index = ZeroCreate.createIndex(5);
    assertEquals(5, index.length());
    index.trim(3);
    assertEquals(3, index.length());
  }
}
