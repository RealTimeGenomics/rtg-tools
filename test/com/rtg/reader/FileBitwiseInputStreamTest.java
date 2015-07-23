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
package com.rtg.reader;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.rtg.util.PortableRandom;
import com.rtg.util.bytecompression.BitwiseByteArray;

import junit.framework.TestCase;

/**
 * Test class
 */
public class FileBitwiseInputStreamTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final PortableRandom pr = new PortableRandom(42);
    final byte[] b = new byte[1000000];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) pr.nextInt(5);
    }
    final BitwiseByteArray bwba = new BitwiseByteArray(b.length, 3);
    bwba.set(0, b, b.length);
    final File f = File.createTempFile("bwstreamtest", "test");
    try {
      try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
        bwba.dumpCompressedValues(dos, b.length);
      }
      try (FileBitwiseInputStream fbs = new FileBitwiseInputStream(f, 3, b.length, true)) {
        for (int i = 0; i < 1000; i++) {
          final int pos = pr.nextInt(b.length);
          fbs.seek(pos);
          assertEquals(bwba.get(pos), (byte) fbs.read());
        }
        fbs.seek(0);
        for (int i = 0; i < b.length; i++) {
          assertEquals(bwba.get(i), (byte) fbs.read());
        }
        fbs.seek(0);
        final byte[] buf = new byte[1000];
        int pos1 = 0;
        int len;
        while ((len = fbs.read(buf, 0, buf.length)) > 0) {
          for (int i = 0; i < len; i++) {
            assertEquals(bwba.get(pos1 + i), buf[i]);
          }
          pos1 += len;
        }
        assertEquals(b.length, pos1);
      }
    } finally {
      assertTrue(f.delete());
    }
  }

}
