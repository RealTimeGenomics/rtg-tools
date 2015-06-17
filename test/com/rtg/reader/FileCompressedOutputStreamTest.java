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
import com.rtg.util.bytecompression.CompressedByteArray;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class FileCompressedOutputStreamTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final File dir = FileUtils.createTempDir("cstreamtest", "test");
    try {
      PortableRandom pr = new PortableRandom(42);
      byte[] b = new byte[1000000];
      for (int i = 0; i < b.length; i++) {
        b[i] = (byte) pr.nextInt(CompressedMemorySequencesReader.MAX_QUAL_VALUE);
      }
      CompressedByteArray bwba = new CompressedByteArray(b.length, CompressedMemorySequencesReader.MAX_QUAL_VALUE, false);
      bwba.set(0, b, b.length);
      File f = new File(dir, "cstreamtest");
      try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
        bwba.dumpCompressedValues(dos, b.length);
      }
      File fo = new File(dir, "costreamtest");
      try (FileCompressedOutputStream out = new FileCompressedOutputStream(fo, CompressedMemorySequencesReader.MAX_QUAL_VALUE)) {
        for (int i = 0; i < b.length; ) {
          int amount = pr.nextInt(b.length - i + 1);
          out.write(b, i, amount);
          i += amount;
          if (pr.nextInt(4) < 1) {
            out.flush();
          }
        }
      }
      byte[] exp = FileBitwiseOutputStreamTest.fileToByteArray(f);
      byte[] res = FileBitwiseOutputStreamTest.fileToByteArray(fo);
      assertEquals(exp.length, res.length);
      for (int i = 0; i < exp.length; i++) {
        assertEquals("i: " + i + " exp: " + exp[i] + " res: " + res[i], exp[i], res[i]);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
    //assertTrue(Arrays.equals(exp, res));
  }

  public void testCanRead() throws IOException {
    final File dir = FileUtils.createTempDir("bwstreamtest", "test");
    try {
      PortableRandom pr = new PortableRandom(42);
      byte[] b = new byte[1000000];
      for (int i = 0; i < b.length; i++) {
        b[i] = (byte) pr.nextInt(CompressedMemorySequencesReader.MAX_QUAL_VALUE);
      }
      final File outF = new File(dir, "out");
      final FileCompressedOutputStream fos = new FileCompressedOutputStream(outF, CompressedMemorySequencesReader.MAX_QUAL_VALUE);
      try {
        fos.write(b);
      } finally {
        fos.close();
      }
      byte[] res = new byte[b.length];
      try (FileCompressedInputStream fis = new FileCompressedInputStream(outF, CompressedMemorySequencesReader.MAX_QUAL_VALUE, fos.values(), false)) {
        int pos = 0;
        int len;
        while ((len = fis.read(res, pos, res.length - pos)) > 0) {
          pos += len;
        }
      }
      assertEquals(b.length, res.length);
      for (int i = 0; i < b.length; i++) {
        assertEquals("i: " + i + " exp: " + b[i] + " res: " + res[i], b[i], res[i]);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

}
