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
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import com.rtg.util.test.FileHelper;


/**
 */
public class GzipAsynchInputStreamTest extends AsynchInputStreamTest {


  @Override
  AsynchInputStream getStream(File file, String text) throws IOException {
    if (text != null) {
      FileHelper.stringToGzFile(text, file);
    }
    return new GzipAsynchInputStream(file);
  }

  @SuppressWarnings("try")
  public void testNullFile() throws IOException {
    try (InputStream ignored = new GzipAsynchInputStream((File) null)) {
      fail("null file exception expected");
    } catch (final IllegalArgumentException e) {
      assertEquals("File cannot be null", e.getMessage());
    }
  }

  public void testReadLarge() throws IOException {
    // after 600 experiments, these seem to be the fastest settings.
    final boolean asynch = true;
    final int bufSize = 1024 * 1024;
    final int gzipSize = 64 * 1024;
    final int readSize = 1024;
    // for (int gzipSize = 1024; gzipSize <= 64 * 1024; gzipSize *= 2) {
    //    final long t0 = System.nanoTime();
    final int kbytes = 2 * 1024;  // size of uncompressed input file in Kbytes.
    final File temp = makeLargeGzipFile(kbytes);
    //File temp = new File("/home2/marku/taskFilterSam/Test10Gb.gz");
    final File tempOut = File.createTempFile("test", "asynchOut");
    try {
      long count = 0;
      //    final long t1 = System.nanoTime();
      try (GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(tempOut))) {
        final InputStream input;
        if (asynch) {
          final GzipAsynchInputStream asynchInput = new GzipAsynchInputStream(temp, bufSize - 1, gzipSize - 1);
          assertEquals(bufSize - 1, asynchInput.mQueue.maxSize());
          input = asynchInput;
        } else {
          input = FileUtils.createGzipInputStream(temp, false);
        }
        try {
          final byte[] buf = new byte[readSize];
          for (; ; ) {
            final int size = input.read(buf);
            if (size <= 0) {
              break;
            }
            //      // consume some time, to emulate processing the data
            //      byte[] dummy = {'A', 'B', 'C', 'D'};
            //      for (long i = 0; i < size; ++i) {
            //        dummy[(int) i & 0x3] ^= 0x01;
            //      }
            output.write(buf, 0, size);
            count += size;
          }
        } finally {
          input.close();
        }
      }
      //    final long t2 = System.nanoTime();
      assertEquals((long) kbytes * 1024, count);
      //    System.out.println("createtime=" + (t1 - t0) / 1000000.0 + ", "
      //        + gzipSize + ", " + bufSize + ", " + readSize + ", "
      //        + (asynch ? "a" : "") + "synch, " + (t2 - t1) / 1000000.0);
      //}
    } finally {
      assertTrue(FileHelper.deleteAll(tempOut));
      assertTrue(FileHelper.deleteAll(temp));
    }
  }

  /**
   * Creates a largish gzipped file containing <code>numCopies</code>
   * of <code>line</code>.
   *
   * @param kbytes size of file
   * @return the temporary File that has been created.
   * @throws IOException
   */
  public File makeLargeGzipFile(int kbytes) throws IOException {
    final byte[] line = new byte[1024];
    final File file = File.createTempFile("test", "GzipAsynch.gz");
    // fill file with a larger amount of data.
    try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
      for (int i = 0; i < kbytes; ++i) {
        // generate a randomish line.
        for (int pos = 0; pos < 1024; ++pos) {
          line[pos] = (byte) ('!' + (pos * (long) i) % 91);
        }
        line[1023] = (byte) '\n';
        out.write(line);
      }
    }
    return file;
  }

  @Override
  public void testEmpty() {
    try {
      super.testEmpty();
      fail();
    } catch (IOException e) {
      // We require that empty output files contain 0 bytes
      // (i.e. not even including empty gzip blocks) for correct
      // fast concatenation of multi-part output files.

      // Java IOException in this case, .NET does not.

      // We will normally avoid reading such files by checking file length first.
    }
  }

  /*
  public void testConstantly() {
    byte[] fiftyMeg = new byte[500 * 1024 * 1024];
    final File tempFile = File.createTempFile("cont", "ziptest");
    try {
      final PortableRandom rand = new PortableRandom();
      rand.nextBytes(fiftyMeg);
      final GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(tempFile));
      try {
        gzipOut.write(fiftyMeg, 0, fiftyMeg.length);
      } finally {
        gzipOut.close();
      }
      final byte[] buf = new byte[2* 1024 * 1024];
      for (int i = 0; i < 1000; ++i) {
        GzipAsynchInputStream asyncIn = new GzipAsynchInputStream(tempFile);
        final long start = System.currentTimeMillis();
        try {
          int len;
          while ((len = asyncIn.read(buf, 0, buf.length)) > 0) {


          }
        } finally {
          asyncIn.close();
        }
        System.out.println("Time: " + (System.currentTimeMillis() - start) + "ms");
      }
    } finally {
      assertTrue(tempFile.delete());
    }
  }
   */
}
