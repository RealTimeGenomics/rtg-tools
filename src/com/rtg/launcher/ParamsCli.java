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

import com.rtg.util.IORunnable;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.Params;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.io.LogStream;

/**
 * Provides a command-line binding between a ParamsTask and a ModuleParams. Oversees the construction
 * and population of an appropriate CFlags, which is then used to create a ModuleParams configuration
 * object.  This ModuleParams is then used as configuration for a ParamsTask.
 *
 * @param <P> type of underlying implementation.
 */
public abstract class ParamsCli<P extends Params> extends LoggedCli {

  /**
   * Get a task to execute with the specified parameters.
   * @param params the parameters for the current execution.
   * @param out where the standard output is written (depending on parameters may not be used).
   * @return the Task.
   * @throws IOException when there is an input output exception during set up.
   */
  protected abstract IORunnable task(P params, OutputStream out) throws IOException;

  /**
   * Uses flags to construct the parameters object.
   * This includes checking the flags for valid values.
   * @return the derived parameters.
   * @throws InvalidParamsException if there are errors in the values of the command line flags.
   * @throws IOException If an I/O error occurs
   */
  protected abstract P makeParams() throws InvalidParamsException, IOException;

  /**
   * Get parameters from command line, set up logging and execute task.
   * @param out where output s to be written.
   * @param initLog where to write the initial log before the command arguments have been parsed and the true log location determined.
   * @return exit code - 0 if all ok - 1 if command line arguments failed.
   * @throws IOException If an IO error occurs
   */
  @Override
  protected int mainExec(final OutputStream out, final LogStream initLog) throws IOException {
    final P localParams;
    try {
      localParams = makeParams();
    } catch (final InvalidParamsException e) {
      e.printErrorNoLog();
      //treat this as an error in the arguments passed to the process
      mFlags.error(mFlags.getInvalidFlagMsg());
      cleanDirectory();
      return 1;
    } catch (final SlimException e) {
      cleanDirectory();
      throw e;
    } catch (final RuntimeException e) {
      mFlags.error("There was an unknown error in your arguments");
      cleanDirectory();
      throw e;
    }
    try {
      Diagnostic.developerLog(localParams.toString());
      task(localParams, out).run();
      return 0;
    } finally {
      localParams.close();
    }
  }
}
