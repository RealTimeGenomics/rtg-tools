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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.util.License;
import com.rtg.util.SpawnJvm;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.CliDiagnosticListener;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;


/**
 * Test AbstractCli.
 *
 */
public class DummyCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ConcreteCli();
  }

  /** A subclass of AbstractCli, just for testing purposes. */
  protected static class ConcreteCli extends AbstractCli {

    /** Which exception should mainExec throw.  Null means none. */
    Throwable mMainException = null;

    @Override
    protected void initFlags() {
      // we assume createFlags has been called.
      mFlags.registerRequired("foo", Integer.class, "usage", "description");
    }

    @Override
    protected int mainExec(OutputStream out, PrintStream err) throws IOException {
      if (mMainException instanceof IOException) {
        throw (IOException) mMainException;
      } else if (mMainException instanceof RuntimeException) {
        throw (RuntimeException) mMainException;
      } else if (mMainException instanceof OutOfMemoryError) {
        throw (OutOfMemoryError) mMainException;
      }
      return 0;
    }

    @Override
    public String moduleName() {
      return "module";
    }
  }

  public void testMainInitOk() {
    checkMainInitOk("--foo", "42");
  }

  public void testMainInitBadFlags() {
    assertTrue(checkMainInitBadFlags("--bar").contains("Error: Unknown flag --bar"));
  }

  public void testMainInitIOException() {
    ((ConcreteCli) mCli).mMainException = new IOException("yuck");
    assertTrue(checkMainInitBadFlags("--foo", "0").contains("An IO problem occurred: \"yuck\"" + StringUtils.LS));
  }

  public void testMainInitSlimException() {
    ((ConcreteCli) mCli).mMainException = new NoTalkbackSlimException("slam slem slim");
    assertEquals("Error: slam slem slim" + StringUtils.LS, checkMainInitBadFlags("--foo", "0"));
  }

  /**
   * Allow testing out of memory condition.
   */
  public static final class TestMainInitOutOfMemoryClass {
    private TestMainInitOutOfMemoryClass() {
    }
    public static void main(String[] args) {
      Diagnostic.setLogStream(System.out);
      final ConcreteCli cli = new ConcreteCli();
      GlobalFlags.resetAccessedStatus();
      cli.mMainException = new OutOfMemoryError();
      final int returnCode = cli.mainInit(new String[] {"--foo", "0" }, System.out, System.err);
      System.exit(returnCode);
    }
  }

  public void testMainInitOutOfMemory() throws IOException, InterruptedException {
    //System.out.println(TestMainInitOutOfMemoryClass.class.getName());
    final Process p = SpawnJvm.spawn(TestMainInitOutOfMemoryClass.class.getName());
    assertEquals(1, p.waitFor());
    TestUtils.containsAll(FileUtils.streamToString(p.getErrorStream()), StringUtils.LS + "Error: There was not enough memory available for the task.");
    assertEquals("", FileUtils.streamToString(p.getInputStream()));

    // close the streams so peeping dave doesn't complain
    p.getOutputStream().close();
    p.getInputStream().close();
    p.getErrorStream().close();
    Diagnostic.setLogStream();
  }



  public void testMainInitOtherException() {
    ((ConcreteCli) mCli).mMainException = new RuntimeException("SOME runtime ERROR");
    final String err = checkMainInitBadFlags("--foo", "0");
    assertTrue(err.contains("RTG has encountered a difficulty, please contact support@realtimegenomics.com"));
    if (License.isDeveloper()) {
      //System.out.println("you are a developer");
      assertTrue(err.contains("SOME runtime ERROR"));
    }
  }

  public void testInitializeMainListener() {
    final MemoryPrintStream out = new MemoryPrintStream();
    final CliDiagnosticListener listener = mCli.initializeMainListener(out.printStream(), TestUtils.getNullPrintStream());
    assertNotNull(listener);
    Diagnostic.setLogStream();
    Diagnostic.error("Error1");
    Diagnostic.closeLog();
    assertEquals("Error: Error1" + StringUtils.LS, out.toString());
  }

  public void testCreateRegisterFlags() {
    assertNull(getCFlags());
    mCli.createRegisterFlags(TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream());
    assertNotNull(getCFlags());
  }

  public void testHandleFlags() {
    assertTrue(checkHandleFlagsErr("--foo").contains("Error: Expecting value for flag --foo"));
    assertEquals("rtg module", getCFlags().getName());
  }
}
