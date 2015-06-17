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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.util.RuntimeIOException;

import junit.framework.TestCase;


/**
 */
public class AsynchOutputStreamTest extends TestCase {

  private static class DiskFullStream extends OutputStream {
    private int mCount;
    DiskFullStream(int size) {
      mCount = size;
    }
    @Override
    public void write(int b) {
      if (--mCount <= 0) {
        //emulating stupid picard exceptions
        throw new RuntimeIOException("Some stupid message", new IOException("Disk out of space."));
      }
    }
  }

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  private static final String EXPECT1 = ""
    + "a\n"
    + "line\ttwo!\n"
    + "ne\ttw";  // no trailing newline, just for fun.

  public void testBufferSize() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      GzipAsynchOutputStream out = new GzipAsynchOutputStream(file, 1024, 1024);
      checkWrite(file, out, 1024);
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  public void testDiskFailure() throws IOException {
    final AsynchOutputStream out = new AsynchOutputStream(new DiskFullStream(80));
    try {
      out.write(new byte[3201]);
      out.flush();
      fail("Wrote more bytes than space available, without exception.");
    } catch (IOException e) {
      assertEquals("Disk out of space.", e.getMessage()); // expected
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        // don't care
      }
    }
  }

  public void testDefaultBufferSize() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (GzipAsynchOutputStream out = new GzipAsynchOutputStream(file)) {
        checkWrite(file, out, GzipAsynchOutputStream.DEFAULT_PIPE_SIZE);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  private void checkWrite(File file, AsynchOutputStream out, int pipeSize) throws IOException {
    out.write((int) 'a');
    out.write((int) '\n');
    final byte[] line = "line\ttwo!\n".getBytes();
    out.write(line);
    out.write(line, 2, 5);
    out.close();
    final String contents = FileHelper.gzFileToString(file);
    assertTrue(TestUtils.sameLines(EXPECT1, contents, false));
    assertEquals(17, contents.length());
    assertEquals(pipeSize, out.getMaxSize());
  }

  public void testFlush() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try {
      try (GzipAsynchOutputStream out = new GzipAsynchOutputStream(file, 1024, 1024)) {
        for (int i = 0; i < 1028; i++) {
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

  public void testThreadName() throws IOException {
    final File file = File.createTempFile("test", "gzipasynch");
    try (FileOutputStream os = new FileOutputStream(file)) {
      AsynchOutputStream out = new AsynchOutputStream(os);
      try {
        assertEquals("AsynchOutputStream", out.mThread.getName());
      } finally {
        out.close();
      }

    } finally {
      assertTrue(FileHelper.deleteAll(file));
    }
  }

  //Testing the Self-suppression problem
  public void testExceptionHandling() throws InterruptedException {
    final byte[] buff = new byte[3];
    try {
      try (final AsynchOutputStream stream = new AsynchOutputStream(new OutputStream() {
        int mNum = 2;
        @Override
        public void write(int b) throws IOException {
          if (mNum > 0) {
            mNum--;
          } else {
            throw new IOException("Expected");
          }
        }
      })) {
        stream.write(buff, 0, 3);
        Thread.sleep(1000);
        stream.flush();
      }
      fail("Should have thrown an IOException");
    } catch (IOException e) {
      assertEquals("Expected", e.getMessage());
    }
  }

}
