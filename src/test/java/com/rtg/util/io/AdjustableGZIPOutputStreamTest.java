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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rtg.util.PortableRandom;
import com.rtg.util.gzip.WorkingGzipInputStream;

import junit.framework.TestCase;

/**
 */
public class AdjustableGZIPOutputStreamTest extends TestCase {

  private class DummyOutputStream extends AdjustableGZIPOutputStream {

    DummyOutputStream(OutputStream out) throws IOException {
      super(out);
    }

    public void checkBufferSize(int bufferSize) {
      assertEquals(bufferSize, buf.length);
    }
  }
  public void testBufSize() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final DummyOutputStream gzipStream = new DummyOutputStream(bos);
    gzipStream.checkBufferSize(65536);
  }


  public void testCompression() throws IOException {
    // generate the uncompressed file/contents.
    final String contents = makeJunk();

    // now check that levels 1..3 of compression give decreasing sizes.
    long size = contents.length();
    long size2 = 0;
    for (int level = 1; level <= 3; ++level) {
      final long compressedSize = compress(contents, level);
      //System.out.println("level " + level + " compresses to " + compressedSize + " bytes");
      assertTrue(compressedSize < size);
      size = compressedSize;
      if (level == 2) {
        size2 = compressedSize;
      }
    }

    // now check the default constructor does level 2.
    final ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
    final OutputStream out = new AdjustableGZIPOutputStream(bos2);
    out.write(contents.getBytes());
    out.flush();
    out.close();
    final long size3 = bos2.size();
    assertEquals(size2, size3);
  }

  /**
   * Compress <code>contents</code> with the given level of compression.
   * @param level compression level (1..9)
   * @return the size of the compressed contents in bytes.
   * @throws IOException
   */
  public long compress(String contents, int level) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final OutputStream out = new AdjustableGZIPOutputStream(bos, 1024, level);
    out.write(contents.getBytes());
    out.flush();
    out.close();
    final byte[] bytes = bos.toByteArray();
    final long size = bos.size();

    // now check that decompressing gives the original contents
    final InputStream in = new WorkingGzipInputStream(new ByteArrayInputStream(bytes));
    for (int i = 0; i < contents.length(); ++i) {
      assertEquals(contents.charAt(i), in.read());
    }
    assertEquals(-1, in.read());
    return size;
  }

  private String makeJunk() {
    final String[] words = {"the", "theme", "of", "this", "is", "to", "have",
        "a", "medium", "level", "of", "randomness!"};
    final StringBuilder builder = new StringBuilder();
    final PortableRandom ran = new PortableRandom(42);
    for (int i = 0; i < 1000; ++i) {
      final String word = words[ran.nextInt(words.length)];
      builder.append(word);
    }
    return builder.toString();
  }
}
