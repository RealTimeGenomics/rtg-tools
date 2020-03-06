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
package com.rtg.launcher;

import static com.rtg.util.StringUtils.FS;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;

import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class OutputParamsTest extends TestCase {

  static final String PROGRESS_FLAG = "progress";

  public static void initFlags(final CFlags flags) {
    CommonFlags.initOutputDirFlag(flags);
    flags.registerOptional('P', PROGRESS_FLAG, "report progress");
    CommonFlags.initNoGzip(flags);
  }

  protected OutputParams getParams(final String[] args) {
    final Appendable out = new StringWriter();
    final CFlags flags = new CFlags("testOutputParams", out, null);
    initFlags(flags);
    flags.setFlags(args);
    return new OutputParams((File) flags.getValue(CommonFlags.OUTPUT_FLAG), !flags.isSet(CommonFlags.NO_GZIP));
  }

  public void test() throws Exception {
    final File dirName = FileUtils.createTempDir("output", "");
    assertTrue(dirName.delete());
    final OutputParams c = getParams(new String[] {"-o", dirName.getPath()});
    final OutputParams d = getParams(new String[] {"-o", dirName.getPath(), "-P", "-Z"});
    TestUtils.equalsHashTest(new OutputParams[][]{{c}, {d}});

    assertEquals("OutputParams output directory=" + dirName.getPath() + " zip=" + Boolean.TRUE.toString(), c.toString());
    assertEquals(dirName.getPath(), c.directory().toString());
    assertTrue(c.file("child").toString().endsWith(dirName.getPath() + FS + "child"));

    assertEquals("OutputParams output directory=" + dirName.getPath() + " zip=" + Boolean.FALSE.toString(), d.toString());
    assertEquals(dirName.getPath(), d.directory().toString());
    assertTrue(d.file("child").toString().endsWith(dirName.getPath() + FS + "child"));

    c.close();
    d.close();
  }

  public void testStreamO() throws Exception {
    final File dirName = FileUtils.createTempDir("output", "");
    assertTrue(dirName.delete());
    final OutputParams a = getParams(new String[] {"-o", dirName.getPath(), "-Z"});
    try {
      final File dir = a.directory();
      assertTrue(dir.mkdir());
      final File file = new File(dir, "out");
      final PrintStream ps = new PrintStream(a.outStream("out"));
      ps.append("foobar");
      ps.close();
      try (InputStream in = new FileInputStream(file)) {
        assertEquals("foobar", FileUtils.streamToString(in));
      }
    } finally {
      a.close();
      FileHelper.deleteAll(a.directory());
    }
  }

  public void testStreamOZ() throws Exception {
    final File dirName = FileUtils.createTempDir("output", "");
    assertTrue(dirName.delete());
    final OutputParams a = getParams(new String[] {"-o", dirName.getPath()});
    try {
      final File dir = a.directory();
      assertTrue(dir.mkdir());
      final File file = new File(dir, "out.gz");
      final PrintStream ps = new PrintStream(a.outStream("out"));
      ps.append("foobar");
      ps.close();
      a.close();

      assertEquals("foobar", FileHelper.zipFileToString(file));
    } finally {
      a.close();
      FileHelper.deleteAll(a.directory());
    }
  }

}
