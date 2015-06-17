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


import static com.rtg.util.StringUtils.LS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogRecord;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class ParamsTaskTest extends TestCase {

  protected File mDir;

  @Override
  public void setUp() throws IOException {
    Diagnostic.setLogStream();
    mDir = FileHelper.createTempDirectory();
  }

  @Override
  public void tearDown() {
    assertTrue(FileHelper.deleteAll(mDir));
    mDir = null;
  }

  public void test() throws IOException, InvalidParamsException {
    final LogRecord log = new LogRecord();
    Diagnostic.setLogStream(log);
    try {
      final File subject = ReaderTestUtils.getDNADir(">s" + LS + "ACTGA" + LS, new File(mDir, "s"));
      final String su = subject.getAbsolutePath();
      final File query = ReaderTestUtils.getDNADir(">q" + LS + "CTGA" + LS, new File(mDir, "q"));
      final String qu = query.getAbsolutePath();
      final File dir = FileUtils.createTempDir("test", "outdir");
      final String di = dir.getAbsolutePath();
      final MockCliParams pr = getParamsStatic(new String[]{"-o", di, "-i", su, "-x", qu, "-l", "4", "-Z"});
      final File diro = FileHelper.createTempDirectory();
      pr.setDirectory(diro);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final ParamsTask<MockCliParams, NoStatistics> pc = new DummyTask(pr, out);
      assertEquals(pr, pc.parameters());

      pc.run();

      assertEquals("", out.toString());
      assertTrue(FileHelper.deleteAll(diro));
      assertTrue(FileHelper.deleteAll(query));
      assertTrue(FileHelper.deleteAll(subject));
      assertTrue(FileHelper.deleteAll(dir));
    } finally {
      Diagnostic.setLogStream();
    }
  }

  private class DummyTask extends MockTask {
    DummyTask(final MockCliParams params, final OutputStream defaultOutput) {
      super(params, defaultOutput);
    }
  }

  static MockCliParams getParamsStatic(final String[] args) throws InvalidParamsException, IOException {
    final Appendable out = new StringWriter();
    final Appendable err = new StringWriter();
    final CFlags flags = new CFlags("testMockParams", out, err);
    //MockCliParams.initFlags(flags);
    flags.setFlags(args);
    return new MockCliParams(flags);
  }

}
