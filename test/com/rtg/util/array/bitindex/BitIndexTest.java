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
package com.rtg.util.array.bitindex;

import static com.rtg.util.StringUtils.LS;

import java.util.Random;

import com.rtg.util.TestUtils;
import com.rtg.util.array.AbstractCommonIndexTest;

/**
 */
public class BitIndexTest extends AbstractCommonIndexTest {

  public void testZeroBits() {
    try {
      new BitIndex(10, 0);
      fail();
    } catch (final IllegalArgumentException e) {
      assertEquals("Illegal range value=0", e.getMessage());
    }
  }

  public void testIndexType() {
    TestUtils.testEnum(BitIndex.IndexType.class, "[DEFAULT, CHUNKED, SINGLE]");
  }

  public void testSwap() {
    final BitIndex index = BitCreate.createIndex(10, 3);
    checkSwap(index);
  }

  public void test1() {
    check(1);
  }

  public void test3() {
    check(3);
  }
  public void test4() {
    check(4);
  }
  public void testBig() {
    // 2 ^ 32
    check(32);
  }

  protected void check(final int bits) {
    final BitIndex index = BitCreate.createIndex(100, bits);
    final int mask = (1 << bits) - 1;
    assertTrue(index.globalIntegrity());
    for (long i = 0; i < index.length(); i++) {
      index.set(i, mask);
    }
    for (long i = index.length() - 1; i >= 0; i -= 3) {
      index.set(i, (int) (i & mask));
    }
    for (long i = 0; i < index.length(); i++) {
      assertEquals("i=" + i, i % 3 == 0 ? i & mask : mask, index.get(i));
    }
    assertEquals(100, index.length());
    assertEquals(8L * (100 / (64 / index.getBits()) + 1), index.bytes());
    assertTrue(index.globalIntegrity());
  }

  public void testRangeTooSmall() {
    checkError(10, 0, "Illegal bits value=0");
  }

  public void testRangeTooBig() {
    final long big = Integer.MAX_VALUE + 1L;
    checkError(10, (int) big, "Illegal bits value=" + -big);
  }

  public void testLengthNegative() {
    checkError(-1, 2, "length=-1");
  }

  public void testLengthEmpty() {
    final BitIndex index = BitCreate.createIndex(0, 5);
    // test toString() when all zeroes
    assertEquals("Index [0]" + LS, index.toString());
  }

  public void testToString() {
    final BitIndex index = BitCreate.createIndex(2, 5);
    index.set(1, 31);
    assertEquals("Index [2]" + LS + "[0]     0,    31" + LS, index.toString());
  }
  public void checkError(final long length, final int range, final String expected) {
    try {
      BitCreate.createIndex(length, range);
      fail("expected exception: " + expected);
    } catch (final Exception e) {
      assertEquals(expected, e.getMessage());
    }
  }

  public void testSigns() {
    final BitIndex bi = new BitIndex(100, 3);
    int index = 0;
    while (index < 100) {
      for (long i = -4; i < 4; i++) {
        bi.setSigned(index++, i);
      }
    }
    index = 0;
    while (index < 100) {
      for (long i = -4; i < 4; i++) {
        assertEquals(i, bi.getSigned(index++));
      }
    }
  }

  public void testMask() {
    final BitIndex bi = new BitIndex(1000, 3);
    final Random rand = new Random(113);
    final int[] r = new int[1000];
    int index = 0;
    while (index < 1000) {
      final int rr = rand.nextInt(8);
      r[index] = rr;
      bi.set(index++, rr);
    }
    index = 0;
    while (index < 1000) {
      for (long i = 0; i < 8; i++) {
        assertEquals(r[index], bi.get(index++));
      }
    }
  }

  public void testSimple() {
    final BitIndex bi = new BitIndex(10, 3);
    int index = 0;
    for (long i = 0; i < 8; i++) {
      bi.set(index++, i);
    }
    index = 0;
    for (long i = 0; i < 8; i++) {
      assertEquals(i, bi.get(index++));
    }
  }

  public void testTrim() {
    final int length = 1000;
    final BitIndex bi = new BitIndex(length, 4);
    assertEquals(length, bi.length());
    assertEquals(504, bi.bytes()); //Regression
    for (int i = 0; i < length; i++) {
      bi.set(i, i & 15);
    }
    final int trim = 42;
    bi.trim(trim);
    assertEquals(trim, bi.length());
    assertEquals(24, bi.bytes()); //Regression
    for (int i = 0; i < trim; i++) {
      assertEquals(i & 15, bi.get(i));
    }
  }

  public void testExtend() {
    final int length = 10;
    final BitIndex bi = new BitIndex(length, 4);
    assertEquals(length, bi.length());
    assertEquals(8, bi.bytes()); //Regression
    for (int i = 0; i < length; i++) {
      bi.set(i, i & 15);
    }

    final int extend = 1000;
    bi.extendBy(extend - length);
    assertEquals(extend, bi.length());
    assertEquals(504, bi.bytes()); //Regression
    for (int i = 0; i < extend; i++) {
      bi.set(i, (i + 1) & 15);
    }
    for (int i = 0; i < extend; i++) {
      assertEquals((i + 1) & 15, bi.get(i));
    }
  }

  public void testExtensible() {
    final BitIndex bi = new BitIndex(3, 3);
    bi.set(0, 5);
    bi.set(1, 7);
    bi.set(2, 2);
    assertEquals(5, bi.get(0));
    assertEquals(7, bi.get(1));
    assertEquals(2, bi.get(2));
    assertEquals(3, bi.extendBy(3));
    assertEquals(5, bi.get(0));
    assertEquals(7, bi.get(1));
    assertEquals(2, bi.get(2));
    bi.set(3, 1);
    bi.set(4, 2);
    bi.set(5, 3);
    assertEquals(1, bi.get(3));
    assertEquals(2, bi.get(4));
    assertEquals(3, bi.get(5));
    assertEquals(6, bi.extendBy(3));
    assertEquals(5, bi.get(0));
    assertEquals(7, bi.get(1));
    assertEquals(2, bi.get(2));
    assertEquals(1, bi.get(3));
    assertEquals(2, bi.get(4));
    assertEquals(3, bi.get(5));
  }

  public void testRoundUpBits() {
    assertEquals(1, BitIndex.roundUpBits(1));
    assertEquals(2, BitIndex.roundUpBits(2));
    assertEquals(4, BitIndex.roundUpBits(3));
    assertEquals(4, BitIndex.roundUpBits(4));
    assertEquals(8, BitIndex.roundUpBits(5));
    assertEquals(8, BitIndex.roundUpBits(8));
  }
}
