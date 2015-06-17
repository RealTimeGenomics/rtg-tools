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
package com.rtg.util.cli;

import java.util.Arrays;
import java.util.UUID;

import com.rtg.util.StringUtils;

/**
 * Utility methods for storing and retrieving the command line arguments.
 *
 */
public final class CommandLine {

  private static String[] sCommandArgs;
  private static String sCommandLine;

  private static UUID sRunId = UUID.randomUUID();

  private CommandLine() {
  }

  /**
   * Sets the current command line
   *
   * @param args the command line
   */
  public static void setCommandArgs(String... args) {
    sCommandArgs = Arrays.copyOf(args, args.length);
    sCommandLine = StringUtils.implode(args, " ", true);
  }

  /**
   * Revert to default state
   */
  public static void clearCommandArgs() {
    sCommandArgs = null;
    sCommandLine = null;
  }

  /**
   * Get the command line arguments used to invoke this instance of Slim
   * @return the command line as individual arguments
   */
  public static String[] getCommandArgs() {
    return Arrays.copyOf(sCommandArgs, sCommandArgs.length);
  }

  /**
   * Get a single printable string containing the command line used to invoke this instance of Slim
   * @return the command line as a single string.
   */
  public static String getCommandLine() {
    return sCommandLine;
  }

  /**
   * @return a UUID for this instance of Slim
   */
  public static UUID getRunId() {
    return sRunId;
  }
}
