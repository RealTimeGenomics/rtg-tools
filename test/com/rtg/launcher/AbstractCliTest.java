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
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

import com.rtg.util.Pair;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.cli.TestCFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.NanoRegression;

import junit.framework.TestCase;


/**
 * Abstract tests for AbstractCli subclasses
 *
 */
public abstract class AbstractCliTest extends TestCase {

  protected AbstractCli mCli;

  protected NanoRegression mNano;

  @Override
  public void setUp() throws IOException {
    GlobalFlags.resetAccessedStatus();
    CommandLine.clearCommandArgs();
    Diagnostic.setLogStream();
    mCli = getCli();
    mNano = new NanoRegression(this.getClass());
  }

  @Override
  public void tearDown() throws IOException {
    GlobalFlags.resetAccessedStatus();
    Diagnostic.setLogStream();
    mCli = null;
    try {
      mNano.finish();
    } finally {
      mNano = null;
    }
  }

  protected abstract AbstractCli getCli();

  protected CFlags getCFlags() {
    return mCli.mFlags;
  }

  public void testApplicationName() {
    assertEquals("rtg", mCli.applicationName());
  }

  /**
   * Checks the help output of the CLI class for consistency and that
   * it contains the specified strings.
   *
   * @param expected a <code>String</code> value
   */
  protected void checkHelp(String... expected) {
    mCli.createRegisterFlags(TestUtils.getNullPrintStream(), null);
    TestCFlags.check(mCli.getCFlags(), expected);
  }

  protected void checkExtendedHelp(String... expected) {
    mCli.createRegisterFlags(TestUtils.getNullPrintStream(), null);
    TestCFlags.checkExtendedUsage(mCli.getCFlags(), expected);
  }


  /**
   * Runs the supplied arguments through the CFlags, under the
   * assumption that they should fail validation.
   *
   * @param args command line arguments.
   * @return the concatenation of stderr and the log stream.
   */
  protected String checkHandleFlagsErr(String... args) {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(os);
    Diagnostic.setLogStream(ps);
    final StringWriter err = new StringWriter();
    try {
      assertFalse(mCli.handleFlags(args, TestUtils.getNullPrintStream(), err));
      assertNotNull(mCli.mFlags);
      ps.flush();
    } finally {
      Diagnostic.setLogStream();
    }
    return os.toString() + err.toString();
  }

  /**
   * Runs the supplied arguments through the CFlags, under the
   * assumption that they should be validated fine.
   *
   * @param args command line arguments.
   * @return the contents of stdout.
   */
  protected String checkHandleFlagsOut(String... args) {
    final StringWriter writer = new StringWriter();
    final MemoryPrintStream err = new MemoryPrintStream();
    final boolean val = mCli.handleFlags(args, writer, err.printStream());
    assertTrue(err.toString(), val);
    return writer.toString();
  }

  /**
   * Runs the supplied arguments through the CFlags.
   * @param args command line arguments.
   */
  protected void checkHandleFlags(String... args) {
    mCli.handleFlags(args, TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
  }

  /**
   * Runs the main method of the cli class under the assumption that
   * it should run with return code 0.
   *
   * @param args command line arguments.
   * @return a pair containing the contents of stdout and stderr, respectively.
   */
  protected Pair<String, String> checkMainInit(String... args) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final MemoryPrintStream err = new MemoryPrintStream();
    final int rc = mCli.mainInit(args, out, err.printStream());
    assertNotNull(mCli.mMainListener);
    assertEquals(err.toString(), 0, rc);
    return new Pair<>(out.toString(), err.toString());
  }

  /**
   * Runs the main method of the cli class under the assumption that
   * it should run without errors.
   *
   * @param args command line arguments.
   * @return the contents of stdout.
   */
  protected String checkMainInitOk(String... args) {
    final Pair<String, String> res = checkMainInit(args);
    final String err = res.getB();
    assertEquals("Error: " + err, "", err);
    return res.getA();
  }

  /**
   * Runs the main method of the cli class under the assumption that
   * it should run but produce warnings.
   *
   * @param args command line arguments.
   * @return the contents of stderr.
   */
  protected String checkMainInitWarn(String... args) {
    final Pair<String, String> res = checkMainInit(args);
    final String err = res.getB();
    assertTrue(err.length() > 0);
    return err;
  }

  /**
   * Runs the main method of the cli class under the assumption that
   * it should fail for some reason.
   *
   * @param args command line arguments.
   * @return the contents of stderr.
   */
  protected String checkMainInitBadFlags(String... args) {
    final MemoryPrintStream err = new MemoryPrintStream();
    assertEquals(1, mCli.mainInit(args, TestUtils.getNullOutputStream(), err.printStream()));
    return err.toString();
  }
}
