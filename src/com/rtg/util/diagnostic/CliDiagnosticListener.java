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
package com.rtg.util.diagnostic;

import java.io.PrintStream;


/**
 * A diagnostic listener suitable for command line programs.  Error and
 * warnings are simply printed on the standard error stream.
 *
 */
public class CliDiagnosticListener implements DiagnosticListener {

  private final PrintStream mErr;
  private final PrintStream mOut;

  /**
   * Listener with warning and error output to specified stream,
   * and default info output to stdout.
   * @param err where warning and error diagnostics are to be written.
   */
  public CliDiagnosticListener(final PrintStream err) {
    this(err, System.out);
  }

  /**
   * Listener with warning and error output to specified stream,
   * and info output to other specified stream.
   * @param err where warning and error diagnostics are to be written.
   * @param out where info diagnostics are to be written.
   */
  public CliDiagnosticListener(final PrintStream err, final PrintStream out) {
    mErr = err;
    mOut = out;
  }

  @Override
  public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
    if (event instanceof WarningEvent || event instanceof ErrorEvent) {
      mErr.println(event.getMessage());
      mErr.flush();
    } else if (event instanceof InformationEvent) {
      mOut.println(event.getMessage());
      mOut.flush();
    }
  }

  @Override
  public void close() { }
}

