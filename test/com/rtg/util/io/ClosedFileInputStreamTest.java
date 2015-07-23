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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class ClosedFileInputStreamTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final File dir = FileUtils.createTempDir("closedStream", "test");
    try {
      final File data = new File(dir, "data");
      final Random r = new Random();
      final byte[] hundy = new byte[10 * 1024 * 1024];
      r.nextBytes(hundy);
      FileHelper.streamToFile(new ByteArrayInputStream(hundy), data);
      assertTrue(Arrays.equals(hundy, IOUtils.readData(new ClosedFileInputStream(data))));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void readAndCheckBlock(ClosedFileInputStream cfi, byte[] src, int position) throws IOException {
    final int toread = 100;
    final byte[] expected = new byte[toread];
    System.arraycopy(src, position, expected, 0, toread);

    final byte[] readdata = new byte[toread];
    cfi.seek(position);

    int read = 0;
    int batchread;
    while (read < toread && (batchread = cfi.read(readdata, read, toread - read)) != -1) {
      read += batchread;
    }

    assertTrue(Arrays.equals(expected, readdata));
  }

  public void testSeek() throws IOException {
    final int bufSize = 16 * 1024;
    final int fileSize = 5 * bufSize;
    final File dir = FileUtils.createTempDir("closedStream", "test");
    try {
      final File data = new File(dir, "data");
      final Random r = new Random();
      final byte[] fileContents = new byte[fileSize];
      r.nextBytes(fileContents);
      FileHelper.streamToFile(new ByteArrayInputStream(fileContents), data);

      try (ClosedFileInputStream cfi = new ClosedFileInputStream(data, bufSize)) {
        // First will seek and read from disk
        readAndCheckBlock(cfi, fileContents, 100);
        assertEquals(1, cfi.getDiskSeeksDone());

        // This will seek and read from disk since it's > bufSize away
        readAndCheckBlock(cfi, fileContents, bufSize + 100);
        assertEquals(2, cfi.getDiskSeeksDone());

        // This seek but not read from disk
        readAndCheckBlock(cfi, fileContents, bufSize + 200);
        assertEquals(2, cfi.getDiskSeeksDone());

        // This seek but not read from disk (back to the start read previously)
        readAndCheckBlock(cfi, fileContents, bufSize + 100);
        assertEquals(2, cfi.getDiskSeeksDone());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testSomeMethod2() throws IOException {
    final File dir = FileUtils.createTempDir("closedStream", "test");
    try {
      final File data = new File(dir, "data");
      final String exp = "a line of text is easier to read than line of bytes";
      FileUtils.stringToFile(exp, data);
      assertEquals(exp, IOUtils.readAll(new ClosedFileInputStream(data, 10)));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
