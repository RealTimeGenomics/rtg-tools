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
package com.rtg;

import java.io.OutputStream;
import java.io.PrintStream;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.jmx.LocalStats;
import com.rtg.util.Constants;
import com.rtg.util.License;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Talkback;
import com.rtg.util.io.FileUtils;

/**
 */
@TestClass({"com.rtg.RtgToolsTest", "com.rtg.DummyCliEntryTest"})
public abstract class AbstractCliEntry {

  /**
   * Launches actual main
   * @param args first argument is module to run, rest depend on the module.
   */
  public void mainImpl(final String[] args) {
    try {
      final OutputStream out = FileUtils.getStdoutAsOutputStream();
      final int returnValue;
      returnValue = intMain(args, out, System.err);
      System.exit(returnValue);
    } catch (final RuntimeException ex) {
      System.exit(1);
    }
  }

  /**
   * Like main, but with an int return value
   * @param args as in regular main
   * @param out Output stream
   * @param err Error stream
   * @return return code
   */
  public int intMain(final String[] args, final OutputStream out, final PrintStream err) {
    if (!License.checkLicense()) {
      VersionCommand.mainInit(err);
      return 1;
    }
    if (args == null || args.length == 0 || args[0].trim().isEmpty()) {
      return help(null, out, err);
    }

    Talkback.setModuleName(args[0]);
    CommandLine.setCommandArgs(args);
    // Look for common flags people might try giving without a module name
    if ("-h".equals(args[0]) || "--help".equals(args[0]) || "-help".equals(args[0])) {
      return help(shift(args), out, err);
    } else if ("-v".equals(args[0]) || "--version".equals(args[0]) || "-version".equals(args[0])) {
      return VersionCommand.mainInit(shift(args), out);
    }

    final Command module = getSlimModule(args[0]);
    if (module == null) {  //unknown module
      help(args, err, err);
      return 1;
    }
    if (!module.isLicensed()) {
      System.err.println(getErrorMessage(module));
      return 1;
    } else {
      try {
        LocalStats.startRecording();
        return module.mainInit(shift(args), out, err);
      } finally {
        LocalStats.stopRecording();
      }
    }
  }

  protected abstract Command getSlimModule(String arg);

  protected int help(String[] shiftArgs, OutputStream out, PrintStream err) {
    return getSlimModule("HELP").mainInit(shiftArgs, out, err);
  }

  static String getErrorMessage(Command module) {
    return "The " + module.getCommandName()
        + " command has not been enabled by your current license.\nPlease contact "
        + Constants.SUPPORT_EMAIL_ADDR + " to have this command licensed.";
  }

  static String[] shift(final String[] arg) {
    final String[] ret = new String[arg.length - 1];
    System.arraycopy(arg, 1, ret, 0, arg.length - 1);
    return ret;
  }
}
