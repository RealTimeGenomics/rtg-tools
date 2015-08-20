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

import com.rtg.util.Utils;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.io.MemoryPrintStream;

/**
 * Simple object for holding the results of a command main execution.
 */
public final class MainResult {

  /**
   * Runs the main method of a cli class and collect the results for comparison in tests.
   *
   * @param cli the command module.
   * @param args command line arguments.
   * @return a result object containing the return code, and contents of stdout and stderr.
   */
  public static MainResult run(AbstractCli cli, String... args) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final MemoryPrintStream err = new MemoryPrintStream();
    GlobalFlags.resetAccessedStatus();
    CommandLine.setCommandArgs(Utils.append(new String[] {cli.moduleName() }, args));
    final int rc = cli.mainInit(args, out, err.printStream());
    return new MainResult(rc, out.toString(), err.toString());
  }

  private final int mRc;
  private final String mOut;
  private final String mErr;

  private MainResult(int rc, String out, String err) {
    mRc = rc;
    mOut = out;
    mErr = err;
  }

  /** @return the result code */
  public int rc() {
    return mRc;
  }

  /** @return the contents of standard output */
  public String out() {
    return mOut;
  }

  /** @return the contents of standard error */
  public String err() {
    return mErr;
  }

}
