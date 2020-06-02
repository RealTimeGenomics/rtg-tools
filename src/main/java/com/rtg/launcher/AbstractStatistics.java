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
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.HtmlReportHelper;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

/**
 * Abstract statistics class with common code for outputting to log and stream
 */
public abstract class AbstractStatistics implements Statistics {

  static {
    System.setProperty("java.awt.headless", "true");
  }

  private final File mOutputDirectory;

  /**
   * @param outputDirectory The base output directory to generate statistics and reports in. May be null if no statistics or reports are to be generated.
   */
  public AbstractStatistics(File outputDirectory) {
    mOutputDirectory = outputDirectory;
  }

  protected HtmlReportHelper getReportHelper() {
    if (mOutputDirectory != null) {
      return new HtmlReportHelper(mOutputDirectory, "index");
    }
    return null;
  }

  /**
   * Method to get the statistics to print or log.
   * May return null if nothing is to be printed or logged.
   *
   * @return the statistics string or null.
   */
  protected abstract String getStatistics();

  @Override
  public void printStatistics(OutputStream reportStream) throws IOException {
    final String stats = getStatistics();
    if (stats != null) {
      if (reportStream != null) {
        //print to output stream (std out often)
        reportStream.write(stats.getBytes());

        //now log to diagnostic
        final String[] lines = stats.split(StringUtils.LS);
        for (final String line : lines) {
          Diagnostic.userLog(line);
        }
      }
      //write to summary file if we have an output directory
      if (mOutputDirectory != null && mOutputDirectory.isDirectory()) {
        try (OutputStream summaryFile = FileUtils.createOutputStream(new File(mOutputDirectory, CommonFlags.SUMMARY_FILE))) {
          summaryFile.write(stats.getBytes());
        }
      }
    }
  }
}
