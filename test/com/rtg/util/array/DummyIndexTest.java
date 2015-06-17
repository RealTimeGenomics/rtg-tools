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

import static com.rtg.util.StringUtils.LS;

import com.rtg.util.array.intindex.IntArray;
import com.rtg.util.array.intindex.IntChunks;

import junit.framework.TestCase;

/**
 */
public class DummyIndexTest extends TestCase {

  public void test() {
    final AbstractIndex index = new IntArray(2);
    index.integrity();
    checkExtensible(index, 2);

    final String exp = ""
        + "Index [2]" + LS
        + "[0]          1,          2" + LS
        ;
    assertEquals(exp, index.toString());

    final StringBuilder sb = new StringBuilder();
    index.dumpString(sb, 0, 1);
    assertEquals("[0]   00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000001" + LS, sb.toString());

    index.swap(0, 1);
    assertEquals(2, index.get(0));
    assertEquals(1, index.get(1));
  }

  private void check(final AbstractIndex index, final long i, final String msg) {
    if (msg == null) {
      index.check(i);
    } else {
      try {
        index.check(i);
        fail();
      } catch (final IndexOutOfBoundsException e) {
        assertEquals(msg, e.getMessage());
      }
    }
  }

  public void test0() {
    final AbstractIndex index = new IntArray(0);
    index.integrity();
  }

  public void testBadLength() {
    try {
      new IntArray(-1);
      fail();
    } catch (final NegativeArraySizeException e) {
      assertEquals("length=-1", e.getMessage());
    }
  }
  public void testToString() {
    final AbstractIndex index = new IntArray(40);
    for (int i = 5; i < 15; i++) {
      index.set(i, i);
    }
    for (int i = 35; i < 40; i++) {
      index.set(i, i);
    }

    final String exp = ""
        + "Index [40]" + LS
        + "[0]          0,          0,          0,          0,          0,          5,          6,          7,          8,          9" + LS
        + "[10]         10,         11,         12,         13,         14,          0,          0,          0,          0,          0" + LS
        + "[30]          0,          0,          0,          0,          0,         35,         36,         37,         38,         39" + LS
        ;
    assertEquals(exp, index.toString());

    final StringBuilder sb = new StringBuilder();
    index.dumpString(sb, 12, 17);
    assertEquals("[12]   00000000:00000000:00000000:00000000:00000000:00000000:00000000:00001100  00000000:00000000:00000000:00000000:00000000:00000000:00000000:00001101  00000000:00000000:00000000:00000000:00000000:00000000:00000000:00001110  00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000  00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000" + LS, sb.toString());
  }

  public void testExtensible() {
    final IntChunks index = new IntChunks(2);
    checkExtensible(index, 2);

    index.extendBy(1);
    checkExtensible(index, 3);

    index.extendTo(5);
    checkExtensible(index, 5);

    index.trim();
    checkExtensible(index, 5);

    index.append(53);
    assertEquals(6, index.length());
    assertEquals(53, index.get(5));

    index.appendSigned(-53);
    assertEquals(7, index.length());
    assertEquals(-53, index.getSigned(6));
  }

  private void checkExtensible(final AbstractIndex index, final long len) {
    index.integrity();
    final long length = index.length();
    assertEquals(len, length);
    check(index, 0, null);
    check(index, length - 1, null);
    check(index, -1, "Index out of bounds:-1 : " + length);
    check(index, length, "Index out of bounds:" + length + " : " + length);
    index.set(0, 1);
    assertEquals(1, index.get(0));
    index.set(length - 1, length);
    assertEquals(length, index.get(length - 1));
  }
}
