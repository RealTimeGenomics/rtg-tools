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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;


/**
 */
public class BgZipTest extends AbstractCliTest {

  public void testHelp() {
    checkHelp("rtg bgzip",
        "Compress a file with block gzip",
        "--decompress", "file to (de)compress",
        "--stdout", "write on standard output, keep original files",
        "--force", "force overwrite of output file");
  }

  public void testCompress() throws Exception {
    final File dir = FileUtils.createTempDir("indexercli", "test");
    try {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam", new File(dir, "test1.sam"));

      final BgZip cli = (BgZip) getCli();
      final MemoryPrintStream mps = new MemoryPrintStream();

      cli.mainInit(new String[] {file1.getPath()}, mps.outputStream(), mps.printStream());
      assertEquals(0, mps.toString().length());

      mps.reset();
      cli.mainInit(new String[] {"--decompress", "-c", new File(dir, "test1.sam.gz").getPath()}, mps.outputStream(), mps.printStream());

      assertTrue(mps.toString().length() > 1);
      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), mps.toString());
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testCompressToStdOut() throws Exception {
    final File dir = FileUtils.createTempDir("indexercli", "test");
    try {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam", new File(dir, "test1.sam"));

      final BgZip cli = (BgZip) getCli();
      final MemoryPrintStream mps = new MemoryPrintStream();

      cli.mainInit(new String[] {"--stdout", file1.getPath()}, mps.outputStream(), mps.printStream());
      assertTrue(mps.toString().length() > 1);
      assertTrue(file1.exists());

      final File bgzippedOutFile = new File(dir, "out.gz");
      FileUtils.byteArrayToFile(mps.outputStream().toByteArray(), bgzippedOutFile);

      mps.reset();
      cli.mainInit(new String[] {"-d", "-c", bgzippedOutFile.getPath()}, mps.outputStream(), mps.printStream());

      assertTrue(mps.toString().length() > 1);
      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), mps.toString());
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testDecompress() throws Exception {
    final File dir = FileUtils.createTempDir("indexercli", "test");
    try {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam.gz", new File(dir, "test1.sam.gz"));

      final BgZip cli = (BgZip) getCli();
      final MemoryPrintStream mps = new MemoryPrintStream();

      assertEquals(0, cli.mainInit(new String[] {"--decompress", file1.getPath()}, mps.outputStream(), mps.printStream()));

      assertEquals(FileHelper.resourceToString("com/rtg/sam/resources/test.sam"), FileUtils.fileToString(new File(dir, "test1.sam")));
      assertEquals(0, mps.toString().length());
      assertFalse(file1.exists());
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testFailures() throws Exception {
    final File dir = FileHelper.createTempDirectory();
    try {
      final File file1 = new File(dir, "input");
      final BgZip cli = (BgZip) getCli();
      final MemoryPrintStream mps = new MemoryPrintStream();
      assertEquals(1, cli.mainInit(new String[] {"--decompress", file1.getPath()}, mps.outputStream(), mps.printStream()));

      assertEquals("Error: The specified file, \"" + file1.getPath() + "\" does not exist.", mps.toString().trim());

      FileUtils.stringToFile("", file1);
      final File fout = new File(dir, "input.gz");
      assertTrue(fout.createNewFile());

      mps.reset();
      assertEquals(1, cli.mainInit(new String[] {file1.getPath()}, mps.outputStream(), mps.printStream()));
      assertEquals("Error: Output file \"" + fout.getPath() + "\" already exists.", mps.toString().trim());
      assertEquals(0, cli.mainInit(new String[] {"-f", file1.getPath()}, mps.outputStream(), mps.printStream()));

      FileUtils.stringToFile("", file1);
      mps.reset();
      assertEquals(1, cli.mainInit(new String[] {"--decompress", file1.getPath()}, mps.outputStream(), mps.printStream()));
      assertEquals("Error: Input file not in GZIP format", mps.toString().trim());


    } finally {
      FileHelper.deleteAll(dir);
    }

  }

  @Override
  protected AbstractCli getCli() {
    return new BgZip();
  }
}
