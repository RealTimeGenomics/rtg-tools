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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 * Tests for the corresponding class
 *
 */

public class MultiByteArrayTest extends TestCase {

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(MultiByteArrayTest.class);
  }

  public void test() throws Exception {
    Diagnostic.setLogStream();
    final MultiByteArray mba = new MultiByteArray(50);
    assertEquals(50, mba.length());
    final byte[] b = new byte[3];

    // set 0 bytes
    mba.set(2, new byte[] {3}, 0);
    mba.get(b, 2, 1);
    assertEquals(0, b[0]);

    // set 1 byte
    mba.set(2, new byte[] {3}, 1);
    mba.get(b, 2, 1);
    assertEquals(3, b[0]);

    mba.get(b, 1, 2);
    assertEquals(0, b[0]);
    assertEquals(3, b[1]);
    assertEquals(0, b[2]);
    try {
      mba.load(new ByteArrayInputStream(b), 0, 55);
      fail("expected index out of bounds exception");
    } catch (final IndexOutOfBoundsException ioe) {
      //expected
    }
    mba.load(new ByteArrayInputStream(b), 0, 2);
  }

  public static byte get1(ByteArray bytearray, long offset) {
    final byte[] tmp = new byte[1];
    bytearray.get(tmp, offset, 1);
    return tmp[0];
  }

  public void testMulti() throws IOException {
    final ByteArrayOutputStream bis = new ByteArrayOutputStream();
    Diagnostic.setLogStream(new PrintStream(bis));
    final MultiByteArray mba = new MultiByteArray(18, 3); // 8 bytes per array
    final long len = mba.length();
    assertEquals((long) 18, len);
    assertEquals((byte) 0, get1(mba, len - 1));
    mba.set(len - 1, new byte[] {3}, 1);
    assertEquals(3, get1(mba, len - 1));
    final byte[] data = {1, 4, 9, 16, 25, 36, 49, 64, 81, 100, 121};
    for (int offset = 0; offset < mba.length(); offset += data.length) {
      mba.set(offset, data, Math.min(data.length, (int) (mba.length() - offset)));
    }
    // check byte by byte
    for (long i = mba.length() - 1; i >= 0; i--) {
      assertEquals(data[(int) i % data.length], get1(mba, i));
    }
    // check the multi-getter.
    for (int start = 0; start < 8; start++) {
      byte[] out = new byte[data.length];
      mba.get(out, start, data.length);
      for (int i = 0; i < data.length; i++) {
        assertEquals(data[(i + start) % data.length], out[i]);
      }
    }
    // do a load longer than one segment.
    mba.load(new ByteArrayInputStream(data, 0, data.length), 3L, data.length);
    for (int i = 0; i < data.length; i++) {
      assertEquals(data[i], get1(mba, 3L + i));
    }
    assertTrue(bis.toString().contains("MultiByteArray allocating 8 bytes (block 1 of 3)"));
    assertTrue(bis.toString().contains("MultiByteArray allocating 8 bytes (block 2 of 3)"));
    assertTrue(bis.toString().contains("MultiByteArray allocating 2 bytes (block 3 of 3)"));
    Diagnostic.setLogStream();
  }

  public void testExtend() {
    final MultiByteArray mb = new MultiByteArray(9, 2);
    mb.set(4, (byte) 1);
    mb.set(8, (byte) 2);
    try {
      mb.set(18, (byte) 3);
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
      //expected
    }
    mb.extendTo(19);
    mb.set(18, (byte) 3);
    final byte[] exp = {0, 0, 0, 0, 1,
                                   0, 0, 0, 2, 0,
                                   0, 0, 0, 0, 0,
                                   0, 0, 0, 3};
    assertEquals((long) exp.length, mb.length());
    for (int i = 0; i < exp.length; i++) {
      assertEquals(exp[i], mb.get(i));
    }
  }
}
