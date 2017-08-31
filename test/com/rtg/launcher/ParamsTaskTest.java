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

import com.rtg.AbstractTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 */
public class ParamsTaskTest extends AbstractTest {

  class MockStatistics implements Statistics {
    private boolean mStatsDone;
    private boolean mReportDone;
    @Override
    public void printStatistics(OutputStream reportStream) throws IOException {
      mStatsDone = true;
    }
    @Override
    public void generateReport() throws IOException {
      mReportDone = true;
    }
  }

  public void test() throws IOException, InvalidParamsException {
    try (TestDirectory dir = new TestDirectory()) {
      final File subject = ReaderTestUtils.getDNADir(">s" + LS + "ACTGA" + LS, new File(dir, "s"));
      final File query = ReaderTestUtils.getDNADir(">q" + LS + "CTGA" + LS, new File(dir, "q"));
      final File odir = FileHelper.createTempDirectory(dir);
      final MockCliParams pr = getParamsStatic(new String[]{"-o", odir.getAbsolutePath(), "-i", subject.getAbsolutePath(), "-x", query.getAbsolutePath(), "-l", "4", "-Z"});
      pr.setDirectory(odir);
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final MockStatistics m = new MockStatistics();
      final ParamsTask<MockCliParams, MockStatistics> pc = new DummyTask(pr, out, m);
      assertEquals(pr, pc.parameters());
      pc.run();
      assertTrue(m.mReportDone);
      assertTrue(m.mStatsDone);
      assertEquals("mcp:", pc.toString());
      assertEquals("", out.toString());
    }
  }

  private class DummyTask extends MockTask<MockStatistics> {
    DummyTask(final MockCliParams params, final OutputStream defaultOutput, MockStatistics m) {
      super(params, defaultOutput, m);
    }
  }

  static MockCliParams getParamsStatic(final String[] args) throws InvalidParamsException, IOException {
    final Appendable out = new StringWriter();
    final Appendable err = new StringWriter();
    final CFlags flags = new CFlags("testMockParams", out, err);
    flags.setFlags(args);
    return new MockCliParams(flags);
  }

}
