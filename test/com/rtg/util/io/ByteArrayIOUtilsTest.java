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
package com.rtg.util.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public final class ByteArrayIOUtilsTest extends TestCase {

  /**
   * run the tests
   * @param args ignored
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(ByteArrayIOUtilsTest.class);
  }

  //last two are a bit of jumble meta-gaming
  private static final long[] TEST_ARRAY_LONG = {5L, 3L, 9L, 2752435L,
                                                Long.MAX_VALUE, Long.MIN_VALUE,
                                                (long) Integer.MAX_VALUE + 1L, (long) Integer.MIN_VALUE - 1L,
                                                -127, -128, 127, 128, -1, 0, 1,
                                                0x0000FF0000000000L, 0x000000FF00000000L};


  private static final int[] TEST_ARRAY_INT = {1, 0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE,
                                                0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF,
                                                0xAABBCCDD, 0xBBCCAADD, 0xDDAACCBB, 0x11223344};

  public void testConvertToLongArray() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream dos = new DataOutputStream(bos)) {
      for (final long a : TEST_ARRAY_LONG) {
        dos.writeLong(a);
      }
    }
    final byte[] bytes = bos.toByteArray();
    final long[] result = ByteArrayIOUtils.convertToLongArray(bytes);
    assertEquals("length", TEST_ARRAY_LONG.length, result.length);
    for (int i = 0; i < TEST_ARRAY_LONG.length; i++) {
      assertEquals("pos: " + i, TEST_ARRAY_LONG[i], result[i]);
    }
    final long[] result2 = new long[bytes.length / 8];
    final int l = ByteArrayIOUtils.convertToLongArray(bytes, result2);
    assertEquals(result2.length, l);
    assertEquals("length", TEST_ARRAY_LONG.length, result2.length);
    for (int i = 0; i < TEST_ARRAY_LONG.length; i++) {
      assertEquals("pos: " + i, TEST_ARRAY_LONG[i], result2[i]);
    }
  }

  public void testConvertToIntArray() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (DataOutputStream dos = new DataOutputStream(bos)) {
      for (int i = 0; i < TEST_ARRAY_INT.length; i++) {
        dos.writeInt(TEST_ARRAY_INT[i]);
      }
    }
    final byte[] bytes = bos.toByteArray();
    final int[] result = ByteArrayIOUtils.convertToIntArray(bytes);
    assertEquals("length", TEST_ARRAY_INT.length, result.length);
    for (int i = 0; i < TEST_ARRAY_INT.length; i++) {
      assertEquals("pos: " + i, TEST_ARRAY_INT[i], result[i]);
    }
    final int[] result2 = new int[bytes.length / 4];
    final int l = ByteArrayIOUtils.convertToIntArray(bytes, result2);
    assertEquals(result2.length, l);
    assertEquals("length", TEST_ARRAY_INT.length, result2.length);
    for (int i = 0; i < TEST_ARRAY_INT.length; i++) {
      assertEquals("pos: " + i, TEST_ARRAY_INT[i], result2[i]);
    }
  }

  public void testEndianness() {
    try {
      ByteArrayIOUtils.convertToLongArray(new byte[5]);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertTrue(iae.getMessage().startsWith("Source data lengt"));
    }
    try {
      ByteArrayIOUtils.convertToLongArray(new byte[32], new long[3]);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertTrue(iae.getMessage().startsWith("Destination length needs to be at least: 4 was: 3"));
    }

    assertEquals(513, ByteArrayIOUtils.bytesToShortLittleEndian(new byte[] {1, 2, 3, 4}, 0));

    assertEquals(67305985, ByteArrayIOUtils.bytesToIntLittleEndian(new byte[] {1, 2, 3, 4}, 0));
    assertEquals(16909060, ByteArrayIOUtils.bytesToIntBigEndian(new byte[] {1, 2, 3, 4}, 0));

    byte[] bs = new byte[4];
    ByteArrayIOUtils.intToBytesLittleEndian(67305985, bs, 0);
    assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, bs));

    final long[] ls = new long[5];

    try {
      assertEquals(1, ByteArrayIOUtils.convertToIntInLongArray(new byte[8], 1, 8, ls, 1, 3));
      fail();
    } catch (final IllegalArgumentException iae) {
      assertTrue(iae.getMessage().startsWith("Source data length needs to be multiple of 4 was: 7"));
    }
    try {
      assertEquals(1, ByteArrayIOUtils.convertToIntInLongArray(new byte[8], 1, 9, ls, 1, 2));
      fail();
    } catch (final IllegalArgumentException iae) {
      assertTrue(iae.getMessage().startsWith("Destination length needs to be at least: 2 was: 1"));
    }

    assertEquals(1, ByteArrayIOUtils.convertToIntInLongArray(new byte[] {5, 1, 2, 3, 4}, 1, 5, ls, 1, 5));
    assertTrue(Arrays.equals(new long[] {0L, 16909060L, 0L, 0L, 0L}, ls));

    final int[] is = new int[4];
    try {
      ByteArrayIOUtils.convertToIntArray(new byte[8], 1, 8, is, 1, 3);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertTrue(iae.getMessage().startsWith("Source data length needs to be multiple of 4 was: 7"));
    }
    try {
      ByteArrayIOUtils.convertToIntArray(new byte[8], 1, 9, is, 1, 2);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertEquals("Destination length needs to be at least: 2 was: 1", iae.getMessage());
    }

    assertEquals(1, ByteArrayIOUtils.convertToIntArray(new byte[] {5, 1, 2, 3, 4}, 1, 5, is, 1, 5));
    assertTrue(Arrays.equals(new long[] {0L, 16909060L, 0L, 0L, 0L}, ls));

    try {
      ByteArrayIOUtils.convertToIntArray(new byte[8], new int[1]);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertEquals("Destination length needs to be at least: 2 was: 1", iae.getMessage());
    }


    ByteBuffer bb1 = ByteBuffer.wrap(new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    long l1 = bb1.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get();
    assertEquals(l1 /*578437695752307201L*/, ByteArrayIOUtils.bytesToLongLittleEndian(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, 0));
    bs = new byte[8];
    ByteArrayIOUtils.longToBytesLittleEndian(578437695752307201L, bs, 0);
    assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, bs));


    ByteBuffer bb = ByteBuffer.wrap(new byte[] {80, 77, -108, -128, 0, 0, 0, 0});
    long l = bb.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get();
    assertEquals(l, ByteArrayIOUtils.bytesToLongLittleEndian(new byte[] {80, 77, -108, -128, 0, 0, 0, 0}, 0));
    try {
      ByteArrayIOUtils.convertToLongArray(new byte[16], new long[1]);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertEquals("Destination length needs to be at least: 2 was: 1", iae.getMessage());
    }
    try {
      ByteArrayIOUtils.convertToLongArray(new byte[18], 2, 18, new long[2], 1, 2);
      fail();
    } catch (final IllegalArgumentException iae) {
      assertEquals("Destination length needs to be at least: 2 was: 1", iae.getMessage());
    }

    assertEquals(1, ByteArrayIOUtils.convertToLongArray(new byte[] {5, 1, 2, 3, 4, 5, 6, 7, 8}, 1, 9, ls, 1, 9));
    assertTrue(Arrays.equals(new long[] {0L, 72623859790382856L, 0L, 0L, 0L}, ls));
  }
}
