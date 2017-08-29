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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.util.IORunnable;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.Params;
import com.rtg.util.diagnostic.CliDiagnosticListener;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.LogRecord;
import com.rtg.util.io.LogStream;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class ParamsCliTest extends TestCase {

  protected File mDir;

  @Override
  public void setUp() throws IOException {
    Diagnostic.setLogStream();
    GlobalFlags.resetAccessedStatus();
    mDir = FileHelper.createTempDirectory();
  }

  @Override
  public void tearDown() {
    assertTrue(FileHelper.deleteAll(mDir));
    mDir = null;
  }

  public void test() throws IOException, InvalidParamsException {
    try (final TestDirectory outDir = new TestDirectory("paramscli")) {
      final ParamsCli<MockCliParams> cli = new MockCli(outDir);
      final MainResult res = MainResult.run(cli);
      assertEquals(1, res.rc());
      assertEquals("Mock task did something", res.out());
    }
  }

  public final void testSomethingOK() throws IOException, InvalidParamsException {
    final ParamsCli<?> cli = new MockCli();
    final ByteArrayOutputStream berr = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(berr);
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bout);
    cli.createFlags(out, err);
    cli.initFlags();
    cli.mFlags.setFlags();
    final Params p = cli.makeParams();
    assertNotNull(p);
    final String estr = berr.toString();
    //System.err.println(estr);
    assertEquals("", estr);
  }

  //error in validator
  public final void testErrV() throws IOException, InvalidParamsException {
    final LogStream log = new LogRecord();
    Diagnostic.setLogStream(log);
    final ByteArrayOutputStream berr = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(berr);
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bout);
    final ParamsCli<?> cli = new MockCli();
    cli.createFlags(out, err);
    cli.initFlags();
    try {
      final CliDiagnosticListener listener = new CliDiagnosticListener(err);
      Diagnostic.addListener(listener);
      try {
        assertFalse(cli.handleFlags(new String[] {"-v"}, out, err));
        //System.err.println("err:" + estr);
        //System.err.println("log:" + logs);
      } finally {
        Diagnostic.removeListener(listener);
      }
    } finally {
      Diagnostic.setLogStream();
      err.close();
    }
    final String exp = "The specified flag \"validator\" has invalid value \"42\". It should be greater than or equal to 1.";
    final String estr = berr.toString();
    final String logs = log.toString();
    assertTrue(logs.contains(exp));
    assertTrue(estr.contains(exp));
  }

  //error in constructor
  public final void testErrC() throws IOException, InvalidParamsException {
    final LogStream log = new LogRecord();
    Diagnostic.setLogStream(log);
    final ByteArrayOutputStream berr = new ByteArrayOutputStream();
    final PrintStream err = new PrintStream(berr);
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final PrintStream out = new PrintStream(bout);
    final ParamsCli<?> cli = new MockCli();
    cli.createFlags(out, err);
    cli.initFlags();
    try {
      final CliDiagnosticListener listener = new CliDiagnosticListener(err);
      Diagnostic.addListener(listener);
      try {
        cli.mFlags.setFlags("-c");
        try {
          cli.makeParams();
          fail();
        } catch (final InvalidParamsException e) {
          assertEquals(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, e.getErrorType());
        }
        //System.err.println("err:" + estr);
        //System.err.println("log:" + logs);
      } finally {
        Diagnostic.removeListener(listener);
      }
    } finally {
      Diagnostic.setLogStream();
      err.close();
    }
  }

  public void testGood() throws IOException {
    try (final TestDirectory dir = new TestDirectory("paramscli")) {
      final TestParamsCli cli = new TestParamsCli(false, false, dir);
      final MainResult res = MainResult.run(cli);
      assertEquals(res.err(), 0, res.rc());
      assertTrue(cli.hasRan());
      assertTrue(dir.exists());
      assertTrue(FileHelper.deleteAll(dir));
      final MainResult res2 = MainResult.run(cli);
      assertEquals(res2.err(), 0, res2.rc());
      assertTrue(dir.exists());
    }
  }

  public void testMakeParamsHandling() throws IOException {
    try (final TestDirectory dir = new TestDirectory("paramscli")) {
      final TestParamsCli cli = new TestParamsCli(true, false, dir);
      final MainResult res = MainResult.run(cli);
      assertEquals(res.err(), 1, res.rc());
      assertTrue(res.err().contains("Test error"));
      assertFalse(cli.hasRan());
      assertTrue(dir.exists());
      assertTrue(FileHelper.deleteAll(dir));
      final MainResult res2 = MainResult.run(cli);
      assertEquals(res2.err(), 1, res2.rc());
      assertFalse(dir.exists());
    }
  }

  public void testMakeParamsHandling2() throws IOException {
    try (final TestDirectory dir = new TestDirectory("paramscli")) {
      final TestParamsCli cli = new TestParamsCli(true, true, dir);
      final MainResult res = MainResult.run(cli);
      assertEquals(res.err(), 1, res.rc());
      assertTrue(res.err().contains("Test error"));
      assertFalse(cli.hasRan());
      assertTrue(dir.exists());
      assertTrue(FileHelper.deleteAll(dir));
      final MainResult res2 = MainResult.run(cli);
      assertEquals(res2.err(), 1, res2.rc());
      assertFalse(dir.exists());
    }
  }

  private static class BogusParams implements Params {
    @Override
    public void close() {
    }
  }
  private static class TestParamsCli extends ParamsCli<BogusParams> {
    private final boolean mThrowException;
    private final boolean mNoTalkback;
    private boolean mRan;
    private final File mDir;

    TestParamsCli(boolean throwException, boolean notalkback, File dir) {
      mThrowException = throwException;
      mNoTalkback = notalkback;
      mDir = dir;
    }

    public boolean hasRan() {
      return mRan;
    }
    @Override
    protected BogusParams makeParams() throws InvalidParamsException, IOException {
      if (mThrowException) {
        if (mNoTalkback) {
          throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Test error");
        }
        throw new InvalidParamsException(ErrorType.INFO_ERROR, "Test error");
      } else {
        return new BogusParams();
      }
    }
    @Override
    protected IORunnable task(BogusParams params, OutputStream out) {
      return new IORunnable() {

        @Override
        public void run() {
          mRan = true;
        }
      };
    }
    @Override
    protected File outputDirectory() {
      return mDir;
    }
    @Override
    protected void initFlags() {
    }
    @Override
    public String moduleName() {
      return "test";
    }
  }
}
