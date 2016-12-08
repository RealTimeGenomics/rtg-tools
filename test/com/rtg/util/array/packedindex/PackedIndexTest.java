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
package com.rtg.util.array.packedindex;

import static com.rtg.util.StringUtils.LS;

import com.rtg.util.array.AbstractCommonIndexTest;
import com.rtg.util.array.bitindex.BitIndex;

/**
 */
public class PackedIndexTest extends AbstractCommonIndexTest {

  public void testSwap() {
    final PackedIndex index = new PackedIndex(10, 7, BitIndex.IndexType.DEFAULT);
    checkSwap(index);
  }

  public void test2() {
    check(2);
  }

  public void test8() {
    check(8);
  }

  public void test9() {
    check(9);
  }

  public void testBig() {
    // 2 ^ 32
    check(Integer.MAX_VALUE);
  }

  public void testHuge() {
    // 2 ^ 62
    check(1L << 62);
  }

  protected void check(final long range) {
    final PackedIndex index = new PackedIndex(100, range, BitIndex.IndexType.DEFAULT);
    assertTrue(index.globalIntegrity());
    for (long i = 0; i < index.length(); ++i) {
      index.set(i, range - 1);
    }
    for (long i = index.length() - 1; i >= 0; i -= 3) {
      index.set(i, (int) (i % range));
    }
    for (long i = 0; i < index.length(); ++i) {
      assertEquals("i=" + i, i % 3 == 0 ? i % range : range - 1, index.get(i));
    }
    assertEquals(100, index.length());
    assertEquals(8L * (100 * index.getBits() / 64 + 1), index.bytes());
    assertTrue(index.globalIntegrity());
    // check that the minimum number of bits is used.
    int bits = 1;
    while (range > (1L << bits)) {
      ++bits;
    }
    assertTrue(bits < 64);
    assertEquals(bits, index.getBits());
  }

  public void testRangeTooSmall() {
    checkError(10, 1, "Illegal range value=1");
  }

  public void testRangeTooBig() {
    final long big = Integer.MAX_VALUE + 1L;
    checkError(10, (int) big, "Illegal range value=" + -big);
  }

  public void testLengthNegative() {
    checkError(-1, 2, "length=-1");
  }

  public void testLengthEmpty() {
    final PackedIndex index = new PackedIndex(0, 42, BitIndex.IndexType.DEFAULT);
    // test toString() when all zeroes
    assertEquals("Index [0]" + LS, index.toString());
  }

  public void testToString5() {
    final PackedIndex index = PackedCreate.createIndex(2, 32);
    index.set(1, 31);
    assertEquals("Index [2]" + LS + "[0]     0,    31" + LS, index.toString());
  }

  public void testToString17() {
    final PackedIndex index = PackedCreate.createIndex(2, 1 << 17);
    index.set(1, 31);
    assertEquals("Index [2]" + LS + "[0]          0,         31" + LS, index.toString());
  }

  public void testToString33() {
    final PackedIndex index = PackedCreate.createIndex(2, 1L << 33);
    index.set(1, 31);
    assertEquals("Index [2]" + LS + "[0]                    0,                   31" + LS, index.toString());
  }

  public void checkError(final long length, final int range, final String expected) {
    try {
      new PackedIndex(length, range, BitIndex.IndexType.DEFAULT);
      fail("expected exception: " + expected);
    } catch (final Exception e) {
      assertEquals(expected, e.getMessage());
    }
  }
}
