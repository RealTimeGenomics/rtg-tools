
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

package com.rtg.util.io.bzip2;

import java.io.IOException;

import com.rtg.util.Resources;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * test class
 */
public class CBZip2InputStreamTest extends TestCase {

  public void testSomeMethod() throws IOException {
    try (CBZip2InputStream s = new CBZip2InputStream(Resources.getResourceAsStream("com/rtg/util/io/bzip2/resources/textfile.bz2"))) {
      final String bzString = FileUtils.streamToString(s);
      final String expString = FileHelper.resourceToString("com/rtg/util/io/bzip2/resources/textfile");
      assertEquals(expString, bzString);
    }
  }

  public void testMultimember() throws IOException {
    try (CBZip2InputStream s = new CBZip2InputStream(Resources.getResourceAsStream("com/rtg/util/io/bzip2/resources/textfilemulti.bz2"))) {
      final String bzString = FileUtils.streamToString(s);
      final String expString = FileHelper.resourceToString("com/rtg/util/io/bzip2/resources/textfile");
      assertEquals(expString, bzString);
    }
  }

  public void testBadFile() throws IOException {
    try (CBZip2InputStream s = new CBZip2InputStream(Resources.getResourceAsStream("com/rtg/util/io/bzip2/resources/textfilebad.bz2"))) {
      try {
        FileUtils.streamToString(s);
        fail();
      } catch (final IOException e) {
        assertTrue(e.getMessage().contains("crc"));
        // expected
      }
    }
  }

  public void testRepetitiveFile() throws IOException {
    try (CBZip2InputStream s = new CBZip2InputStream(Resources.getResourceAsStream("com/rtg/util/io/bzip2/resources/sample3.ref.bz2"))) {
      final String bzString = FileUtils.streamToString(s);
      final String expString = FileHelper.resourceToString("com/rtg/util/io/bzip2/resources/sample3.ref");
      assertEquals(expString, bzString);
    }
  }
}
