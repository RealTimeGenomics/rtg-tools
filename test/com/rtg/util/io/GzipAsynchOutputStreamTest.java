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

import com.rtg.util.test.FileHelper;


/**
 */
public class GzipAsynchOutputStreamTest extends AsynchOutputStreamTest {

  @SuppressWarnings("try")
  public void testNullFile() throws IOException {
    try {
      try (GzipAsynchOutputStream ignored = new GzipAsynchOutputStream(null)) {
        fail("IllegalArgumentException expected");
      }
    } catch (IllegalArgumentException e) {
      assertEquals("File cannot be null", e.getMessage());
    }
  }

  @Override
  public void testFlush() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (GzipAsynchOutputStream out = new GzipAsynchOutputStream(file, 1024, 1024)) {
        for (int i = 0; i < 1028; ++i) {
          out.write((int) 'a');
        }
        out.flush();
        assertEquals(0, out.mQueue.available());
        out.write((int) 'b');
      }
      final String contents = FileHelper.gzFileToString(file);
      assertTrue(contents.startsWith("aaaaaaa"));
      assertTrue(contents.endsWith("aaab"));
      assertEquals(1028 + 1, contents.length());
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }
}
