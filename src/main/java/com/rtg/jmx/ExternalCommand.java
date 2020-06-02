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
package com.rtg.jmx;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Run a command and return the lines of output. The current
 * implementation assumes that the command produces very little output
 * (such that OS buffers are sufficient to capture stdout)
 */
public class ExternalCommand {

  private static final String LS = System.lineSeparator();

  private final String[] mCommand;

  /**
   * Creates a new <code>ExternalCommand</code> instance.
   *
   * @param command the command arguments.
   */
  public ExternalCommand(String... command) {
    mCommand = command;
  }

  /**
   * Run the configured command and returns the lines of output
   *
   * @return an array of the lines of output, or null if there was a problem executing the command.
   * @exception IOException if an error occurs.
   */
  public String[] runCommand() throws IOException {
    return runCommand(mCommand);
  }


  protected String[] runCommand(String... command) throws IOException {
    try {
      final ProcessBuilder pb = new ProcessBuilder(command);
      final Process p = pb.start();
      // The command will write out less than 1kb of data, so don't need a thread to pull the output.
      p.waitFor();
      final String[] result = readAll(p.getInputStream()).split(LS);
      p.getErrorStream().close();
      p.getOutputStream().close();
      if (p.exitValue() != 0) {
        return null;
      }
      return result;
    } catch (InterruptedException ie) {
      return null;
    }
  }

  static String readAll(final InputStream is) throws IOException {
    try (InputStreamReader isr = new InputStreamReader(is)) {
      final StringBuilder sb = new StringBuilder();
      final char[] buffer = new char[4096];
      int length;
      while ((length = isr.read(buffer)) != -1) {
        sb.append(buffer, 0, length);
      }
      return sb.toString();
    }
  }

}
