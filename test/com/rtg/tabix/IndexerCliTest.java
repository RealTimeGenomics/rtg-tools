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
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.gzip.GzipUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Test class
 */
public class IndexerCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new IndexerCli();
  }

  public void testUsage() {
    checkHelp("-f", "--format", "sam", "bam", "coveragetsv", "bed", "-I", "format of input", "input-list-file", "list of block compressed", "containing data", "TAB-delimited");
  }

  public void testFlags() throws IOException {
    final File f = FileUtils.createTempDir("foo", "rah");
    try {
      checkHandleFlagsOut("-f", "sam", f.getPath());
      checkHandleFlagsOut("-f", "bam", f.getPath());
      checkHandleFlagsOut("-f", "vcf", f.getPath());
      checkHandleFlagsOut("-f", "coveragetsv", f.getPath());
      checkHandleFlagsErr("-f", "hobbit", f.getPath());
      checkHandleFlagsErr(f.getPath());
    } finally {
      assertTrue(f.delete());
    }
  }

  public void testOperation() throws IOException {
    try (final TestDirectory dir = new TestDirectory("indexercli")) {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam.gz", new File(dir, "test1.sam.gz"));
      final File file2 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam.gz", new File(dir, "test2.sam.gz"));
      final File file3 = FileHelper.resourceToFile("com/rtg/sam/resources/test.sam.gz", new File(dir, "test3.sam.gz"));
      final File file4;
      try (InputStream is = GzipUtils.createGzipInputStream(Resources.getResourceAsStream("com/rtg/sam/resources/test.sam.gz"))) {
        file4 = FileHelper.streamToFile(is, new File(dir, "test4.sam"));
      }
      try (MemoryPrintStream out = new MemoryPrintStream();
           MemoryPrintStream err = new MemoryPrintStream()) {
          final int code = getCli().mainInit(new String[]{"-f", "sam", file1.getPath(), file2.getPath(), file3.getPath(), file4.getPath()}, out.outputStream(), err.printStream());
          assertEquals(err.toString(), 1, code);
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file1.getPath())), "Creating index for", "test1.sam.gz.tbi");
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file2.getPath())), "Creating index for", "test2.sam.gz.tbi");
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file3.getPath())), "Creating index for", "test3.sam.gz.tbi");
          assertEquals("Cannot create index for " + file4.getPath() + " as it is not in bgzip format." + StringUtils.LS, err.toString());
        }
    }
  }

  public void testBamOperation() throws IOException {
    try (final TestDirectory dir = new TestDirectory("indexercli")) {
      final File file1 = FileHelper.resourceToFile("com/rtg/sam/resources/bam.bam", new File(dir, "bam1.bam"));
      final File file2 = FileHelper.resourceToFile("com/rtg/sam/resources/bam.bam", new File(dir, "bam2.bam"));
      final File file3 = FileHelper.resourceToFile("com/rtg/sam/resources/bam.bam", new File(dir, "bam3.bam"));
      final File file4 = FileHelper.resourceToFile("com/rtg/sam/resources/unmated.sam", new File(dir, "test4.sam"));
      try (MemoryPrintStream out = new MemoryPrintStream();
           MemoryPrintStream err = new MemoryPrintStream()) {
          final int code = getCli().mainInit(new String[]{"-f", "bam", file1.getPath(), file2.getPath(), file3.getPath(), file4.getPath()}, out.outputStream(), err.printStream());
          assertEquals(err.toString(), 1, code);
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file1.getPath())), "Creating index for", "bam1.bam.bai");
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file2.getPath())), "Creating index for", "bam2.bam.bai");
          TestUtils.containsAll(StringUtils.grep(out.toString(), Pattern.quote(file3.getPath())), "Creating index for", "bam3.bam.bai");
          assertEquals("Cannot create index for " + file4.getPath() + " as it is not in bgzip format." + StringUtils.LS, err.toString());
      }
    }
  }
}
