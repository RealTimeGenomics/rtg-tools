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
package com.rtg.tabix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;


/**
 */
public class BgZipTest extends AbstractCliTest {

  public void testHelp() {
    checkHelp("rtg bgzip",
        "Compress a file with block gzip",
        "--decompress", "file to (de)compress",
        "--stdout", "write on standard output, keep original files",
        "--force", "force overwrite of existing output file");
  }

  public void testCompress() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam", new File(dir, "test1.sam"));

      String out = checkMainInitOk(file1.getPath());
      assertEquals(0, out.length());

      out = checkMainInitOk("--decompress", "-c", new File(dir, "test1.sam.gz").getPath());

      assertTrue(out.length() > 1);
      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), out);
    }
  }

  public void testCompressToStdOut() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam", new File(dir, "test1.sam"));

      final MemoryPrintStream mps = new MemoryPrintStream();
      getCli().mainInit(new String[]{"--stdout", file1.getPath()}, mps.outputStream(), mps.printStream());
      assertTrue(mps.toString().length() > 1);
      assertTrue(file1.exists());
      final File bgzippedOutFile = new File(dir, "out.gz");
      FileUtils.byteArrayToFile(mps.outputStream().toByteArray(), bgzippedOutFile);

      final String out = checkMainInitOk("-d", "-c", bgzippedOutFile.getPath());

      assertTrue(out.length() > 1);
      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), out);
    }
  }

  public void testDecompress() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam.gz", new File(dir, "test1.sam.gz"));

      final String out = checkMainInitOk("--decompress", file1.getPath());

      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), FileUtils.fileToString(new File(dir, "test1.sam")));
      assertEquals(0, out.length());
      assertFalse(file1.exists());
    }
  }

  public void testFailures() throws Exception {
    try (TestDirectory dir = new TestDirectory()) {
      final File file1 = new File(dir, "input");

      String err = checkMainInitBadFlags("--decompress", file1.getPath());
      assertEquals("Error: The specified file, \"" + file1.getPath() + "\" does not exist.", err.trim());

      FileUtils.stringToFile("", file1);
      final File fout = new File(dir, "input.gz");
      assertTrue(fout.createNewFile());

      err = checkMainInitBadFlags(file1.getPath());
      assertEquals("Error: Output file \"" + fout.getPath() + "\" already exists.", err.trim());

      checkMainInitOk("-f", file1.getPath());

      FileUtils.stringToFile("", file1);

      err = checkMainInitBadFlags("--decompress", file1.getPath());
      assertEquals("Error: Input file not in GZIP format", err.trim());

    }

  }

  public void testStdIn() throws IOException {
    final String data = "blah";
    try (TestDirectory dir = new TestDirectory()) {
      final File gzFile = FileHelper.stringToGzFile(data, new File(dir, "gzippedFile.gz"));
      final InputStream beforeIn = System.in;
      try {
        try (InputStream newIn = new FileInputStream(gzFile)) {
          System.setIn(newIn);
          final MainResult res = MainResult.run(new BgZip(), "-d", "-");
          assertEquals(0, res.rc());
          assertEquals(data, res.out());
        }
      } finally {
        System.setIn(beforeIn);
      }
    }
  }


  @Override
  protected AbstractCli getCli() {
    return new BgZip();
  }
}
