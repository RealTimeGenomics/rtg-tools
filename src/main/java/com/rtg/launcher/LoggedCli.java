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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.rtg.jmx.LocalStats;
import com.rtg.util.cli.CommandLine;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.diagnostic.Spy;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LogFile;
import com.rtg.util.io.LogStream;

/**
 * Basic handling of logged command line modules. This class assumes
 * the modules have the concept of an output directory into which the
 * log is written.
 *
 */
public abstract class LoggedCli extends AbstractCli {

  static final String LOG_EXT = ".log";

  private File mDirectory = null;

  private boolean mCleanDirectory = false;


  /**
   * Determine output directory from args
   * @return output directory
   */
  protected abstract File outputDirectory();

  protected void createDirectory(File dir) {
    if (!dir.exists()) {
      mDirectory = dir;
    }
    if (!dir.isDirectory() && !dir.mkdirs()) {
      throw new NoTalkbackSlimException(ErrorType.DIRECTORY_NOT_CREATED, dir.getPath());
    }
  }

  protected void cleanDirectory() {
    if (mDirectory != null) {
      mCleanDirectory = true;
    }
  }

  protected List<DiagnosticListener> initializeOtherListeners() {
    return new ArrayList<>();
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    mDirectory = null;         // Resets state between multiple invocations
    mCleanDirectory = false;   // Resets state between multiple invocations
    final File outputDir = outputDirectory();
    createDirectory(outputDir);
    if (LocalStats.MON_DEST_OUTDIR.equals(System.getProperty(LocalStats.MON_DEST))) {
      System.setProperty(LocalStats.MON_DEST, new File(outputDir, "jmxmon.log").toString());
      LocalStats.startRecording();
    }
    final String prefix = moduleName().length() > 0 ? moduleName() : applicationName();
    final File logFile = new File(outputDir, prefix + LOG_EXT);
    removePrevious(logFile);
    final LogStream outputLog = new LogFile(logFile);
    final long startTime = System.currentTimeMillis();
    boolean successful = false;
    try {
      removePrevious(new File(outputDir, "done"));
      removePrevious(new File(outputDir, "progress"));
      initializeLogs(outputLog);
      try {
        final List<DiagnosticListener> listeners = initializeOtherListeners();
        try {
          final int ret = mainExec(out, outputLog);
          if (ret == 0) {
            successful = true;
            final String time = "Finished successfully in " + timeDifference(System.currentTimeMillis(), startTime) + " s.";
            try (PrintStream done = new PrintStream(new FileOutputStream(new File(outputDir, "done")))) {
              done.println(time);
            }
          }
          return ret;
        } finally {
          for (final DiagnosticListener list : listeners) {
            Diagnostic.removeListener(list);
          }
        }
      } catch (final SlimException e) {
        e.logException();
        throw e;
      } catch (final IOException | RuntimeException | Error e) {
        Diagnostic.userLog(e);
        throw e;
      }
    } finally {
      Spy.report();
      final String time = getDuration(startTime, successful);
      Diagnostic.userLog(time);
      Diagnostic.progress(time);
      Diagnostic.closeLog();
      if (mCleanDirectory) {
        FileUtils.deleteFiles(mDirectory);
      }
    }
  }

  private void removePrevious(File file) throws IOException {
    if (file.exists()) {
      if (!file.isFile()) {
        throw new IOException("Previous file \"" + file.getPath() + "\" cannot be removed: not a file");
      } else if (!file.delete()) {
        throw new IOException("Previous file \"" + file.getPath() + "\" could not be removed");
      }
    }
  }

  /**
   * Get the duration for a run.
   * @param startTime start time in milliseconds
   * @param successful true if the run was successful
   * @return time description
   */
  public static String getDuration(long startTime, boolean successful) {
    return (successful ? "Finished successfully" : "Run failed") + " in " + timeDifference(System.currentTimeMillis(), startTime) + " s.";
  }

  /**
   * Subclasses implement this method to do the work of the
   * module. Modules can assume that logging has been set up, flags
   * are configured, and that exceptions will be handled by the
   * caller.
   *
   * @param out the <code>OutputStream</code> to be used for standard output.
   * @param log a <code>LogStream</code> to be used for logging.
   * @return main return code. 0 for usual operation, non-zero in the case of an error.
   * @exception IOException if an error occurs.
   */
  protected abstract int mainExec(OutputStream out, LogStream log) throws IOException;

  /**
   * Do common tasks for initializing logs.
   * @param initLog where to send the inital logging.
   */
  protected void initializeLogs(final LogStream initLog) {
    if (initLog != null) {
      Diagnostic.setLogStream(initLog);
      Diagnostic.logEnvironment();
      Diagnostic.progress("Started");
    } else {
      Diagnostic.setLogStream();
    }
    Diagnostic.userLog("Command line arguments: " + mFlags.getCommandLine());
    Diagnostic.userLog("Run Id: " + CommandLine.getRunId());
  }

  protected static long timeDifference(long currentTime, long startTime) {
    return (currentTime - startTime) / 1000;
  }
}
