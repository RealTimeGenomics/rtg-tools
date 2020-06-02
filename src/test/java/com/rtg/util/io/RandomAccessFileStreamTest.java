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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Test class
 */
public class RandomAccessFileStreamTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final File tmp = FileUtils.stringToFile("01234567890123456789", File.createTempFile("tmp", "file"));
    try {
      try (RandomAccessFileStream raf = new RandomAccessFileStream(new RandomAccessFile(tmp, "r"))) {
        raf.seek(5);
        assertEquals('5', (char) raf.read());
        raf.seek(11);
        final byte[] buf = new byte[5];
        final int len = raf.read(buf);
        assertEquals("12345".substring(0, len), new String(buf, 0, len));

        assertEquals(16, raf.getPosition());

        assertEquals(20, raf.length());

        assertEquals(4, raf.available());

        assertFalse(raf.markSupported());

        final byte[] b = new byte[2];
        assertEquals(2, raf.read(b));
        assertTrue(Arrays.equals(new byte[]{(byte) '6', (byte) '7'}, b));
        assertEquals(2, raf.read(b, 0, 2));
        assertTrue(Arrays.toString(b), Arrays.equals(new byte[]{(byte) '8', (byte) '9'}, b));
      }
    } finally {
      assertTrue(tmp.delete());
    }
  }
}
