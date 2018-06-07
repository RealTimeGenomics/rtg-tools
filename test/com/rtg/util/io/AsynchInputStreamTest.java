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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;


/**
 */
public class AsynchInputStreamTest extends TestCase {

  private static final String EXAMPLE1 = "abc\ndef ghi\n";

  AsynchInputStream getStream(File file, String text) throws IOException {
    if (text != null) {
      FileUtils.stringToFile(text, file);
    }
    return new AsynchInputStream(new FileInputStream(file));
  }

  public void testReadSmall() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (AsynchInputStream input = getStream(file, EXAMPLE1)) {
        final byte[] buf = new byte[100];
        assertEquals(12, input.read(buf));
        for (int i = 0; i < EXAMPLE1.length(); ++i) {
          assertEquals(EXAMPLE1.charAt(i), (char) buf[i]);
        }
        final int exp = -1;
        assertEquals(exp, input.read(buf));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  public void testEarlyClose() throws IOException {
    Diagnostic.setLogStream();
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      final AsynchInputStream input = getStream(file, EXAMPLE1);
      input.close(); // check that this does not throw an exception
      // Note: it would be nice to check that the close has stopped the
      // input from being read, but we do not know how far the thread has
      // already read, so there is a race condition if we uncomment the
      // following lines.  (It fails on some linux machines, but not others).
      // Is there any nice way of checking that the early close is working?
      // final byte[] buf = new byte[100];
      // assertEquals(-1, input.read(buf));
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  public void testMarkNotSupported() throws IOException {
    Diagnostic.setLogStream();
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (AsynchInputStream input = getStream(file, EXAMPLE1)) {
        assertFalse(input.markSupported());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  public void testEmpty() throws IOException {
    Diagnostic.setLogStream();
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (AsynchInputStream in = getStream(file, null)) {
        assertEquals(1024 * 1024, in.mQueue.maxSize());
        final byte[] buf = new byte[1];
        assertEquals(-1, in.read(buf, 0, 1));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  //Testing the Self-suppression problem
  public void testExceptionHandling() {
    final byte[] buff = new byte[3];
    try {
      try (final AsynchInputStream stream = new AsynchInputStream(new InputStream() {
        int mNum = 2;
        @Override
        public int read() throws IOException {
          if (mNum > 0) {
            return mNum--;
          } else {
            throw new IOException("Expected");
          }
        }
      })) {
        final int b = stream.read(buff, 0, 3);
        final int n = stream.read(buff, 0, 1);
        //These assert statements should not be reached, the second read statement should be throwing an exception
        //They are here because findbugs does not like ignoring the return value of a method
        assertEquals(2, b);
        assertEquals(-1, n);
      }
      fail("Should have thrown an IOException");
    } catch (IOException e) {
      assertEquals("Expected", e.getMessage());
    }
  }

}
