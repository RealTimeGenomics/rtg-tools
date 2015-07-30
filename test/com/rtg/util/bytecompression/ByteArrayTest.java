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
package com.rtg.util.bytecompression;

import junit.framework.TestCase;

/**
 * Tests for the corresponding class
 */
public class ByteArrayTest extends TestCase {

  protected ByteArray getByteArray(long size, int bits) {
    return ByteArray.allocate(size);
  }

  public void test() {
    final ByteArray ba = ByteArray.allocate(20);
    assertEquals(20, ba.length());
    assertEquals(ba.bytes(), ba.length());
    assertTrue(ba instanceof SingleByteArray);
  }

  public void testSet() {
    final int bits = 3;
    final ByteArray array = getByteArray(128, bits);
    final byte[] data = {1, 3, 5, 7, 0};
    long offset = 0L;
    while (offset + data.length < array.length()) {
      //System.out.println("offset = " + offset);
      array.set(offset, data, data.length);
      assertEquals(1, array.get(offset));
      assertEquals(3, array.get(offset + 1));
      assertEquals(5, array.get(offset + 2));
      assertEquals(7, array.get(offset + 3));
      offset += data.length;
    }
  }

  public void testSimple() {
    final int bits = 3;
    final ByteArray array = getByteArray(128, bits);

    final byte[] data = new byte[(int) array.length()];
    for (int i = 0; i < data.length; i++) {
      final int value = (i + 3) % (1 << bits);
      assert value < 128;
      data[i] = (byte) value;
    }
    data[data.length - 1] = (byte) ((1 << bits) - 1);
    array.set(0L, data, data.length);
    final byte[] tmp = new byte[data.length];
    array.get(tmp, 0L, data.length);
    for (int i = 0; i < data.length; i++) {
      assertEquals("data[" + i + "]", data[i], tmp[i]);
      assertEquals("data[" + i + "]", data[i], array.get(i));
    }
  }

  /**
   * This tests a sequence of sets that moves along the array,
   * and a get that goes back before the set, to make sure nothing
   * was overwritten.
   */
  public void testGetSet() {
    final byte[] data = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0};

    for (int size = 1; size < data.length; size++) {
      final ByteArray array = getByteArray(100, 7);
      long offset = 1L;  // because our get goes one byte before offset.
      while (offset < array.length()) {
        final int safeWrite = Math.min(size, (int) (array.length() - offset));
        array.set(offset, data, safeWrite);

        // now check the contents.
        final byte[] tmp = new byte[safeWrite + 1];
        array.get(tmp, offset - 1, safeWrite + 1);
        assertEquals("tmp[0]", offset == 1L ? (byte) 0 : data[size - 1], tmp[0]);
        for (int i = 1; i <= safeWrite; i++) {
          assertEquals("tmp[" + i + "]", data[i - 1], tmp[i]);
          assertEquals("tmp[" + i + "]", data[i - 1], array.get(offset + i - 1));
        }

        // move along to the next set.
        offset += size;
      }
    }
  }

}
