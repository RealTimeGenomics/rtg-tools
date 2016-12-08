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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rtg.util.test.RandomByteGenerator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Abstract class for testing the compressed and bitwise file streams.
 */
public abstract class AbstractFileStreamRegression extends TestCase {

  private static final int RANGE = 128;
  private static final long NUM_ELEMENTS = 10L * Integer.MAX_VALUE + 9000L;

  protected abstract long calcLength(int range, long elements);
  protected abstract OutputStream createOutputStream(File file, int range) throws IOException;
  protected abstract InputStream createInputStream(File file, int range, long elements, boolean seekable) throws IOException;

  /**
   * Test the input and output streams
   * @throws IOException if an error occurs
   */
  public void testStreams() throws IOException {
    Diagnostic.setLogStream();
    doTest(RANGE, NUM_ELEMENTS);
  }

  private void doTest(int range, long elements) throws IOException {
    final File outDir = FileHelper.createTempDirectory();
    System.err.println(outDir.getPath());
    try {
      final File temp = new File(outDir, "temp.bin");
      final RandomByteGenerator value = new RandomByteGenerator(range);
      final byte[] buffer = new byte[1024];
      try (OutputStream out = createOutputStream(temp, range)) {
        for (long l = 0; l < elements; ) {
          int i = 0;
          for (; i < buffer.length && l < elements; ++i, ++l) {
            buffer[i] = value.nextValue();
          }
          out.write(buffer, 0, i);
        }
        out.flush();
      }
      assertTrue(temp.exists());
      assertEquals(calcLength(range, elements), temp.length());

      value.reset();
      //      final long labelChunkSize = NUM_ELEMENTS / 1000L;
//      long nextLabelOutput = labelChunkSize;
      try (InputStream in = createInputStream(temp, range, elements, false)) {
        for (long l = 0; l < elements; ) {
          final int read = in.read(buffer);
          for (int i = 0; i < read; ++i, ++l) {
            assertEquals(value.nextValue(), buffer[i]);
          }
//          if (l >= nextLabelOutput) {
//            System.err.println("Elements Read: " + l);
//            nextLabelOutput += labelChunkSize;
//          }
        }
      }
    } finally {
      FileHelper.deleteAll(outDir);
    }
  }
}
