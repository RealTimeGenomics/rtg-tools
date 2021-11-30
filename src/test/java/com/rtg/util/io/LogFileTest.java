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
import java.io.IOException;

import com.rtg.util.StringUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;



/**
 */
public class LogFileTest extends TestCase {

  private static final String FS = System.getProperty("file.separator");

  /**
   * Test method for {@link com.rtg.util.io.LogFile}.
   * @throws IOException
   */
  public final void test() throws IOException {
    final File file = File.createTempFile("logFileTest", "log");
    try (final LogStream ls = new LogFile(file)) {
      assertTrue(file.exists());
      final String ts = ls.toString();
      assertTrue(ts.startsWith("LogFile "));
      assertTrue(ts.contains(FS + "logFileTest"));
      assertTrue(ts.endsWith("log"));
      ls.stream().println("l1");
      ls.stream().println("l2");
      ls.stream().flush();
      assertEquals("l1" + StringUtils.LS + "l2" + StringUtils.LS, FileUtils.fileToString(file));
      assertEquals(file, ls.file());
      assertEquals(new File(file.getPath()), file);
      ls.removeLog();
    }
    assertTrue(!file.exists());
  }

  /**
   * Test method for {@link com.rtg.util.io.LogFile}.
   * @throws IOException
   */
  public final void testNull() throws IOException {
    final File impossibleLog = FileHelper.createTempDirectory();
    try (final LogStream ls = new LogFile(impossibleLog)) {
      assertNull(ls.stream());
      ls.removeLog();
    } finally {
      FileHelper.deleteAll(impossibleLog);
    }
  }
}

