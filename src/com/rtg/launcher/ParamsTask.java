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

import com.rtg.usage.UsageMetric;
import com.rtg.util.IORunnable;

/**
 * Common code for those modules/tasks that can be configured via a ModuleParams.
 * @param <P> type of underlying implementation.
 * @param <S> type of statistics object.
 */
public abstract class ParamsTask<P extends ModuleParams, S extends Statistics> implements IORunnable {

  protected P mParams;  //see bug 1447 for why this is not final
  protected final OutputStream mReportStream;
  protected final UsageMetric mUsageMetric;

  /** Declared here to keep the testing boffins happy */
  protected S mStatistics;

  /**
   * @param params parameters for the build and search.
   * @param reportStream stream to write statistics to.
   * @param stats statistics object to populate.
   * @param usageMetric keeps track of usage for the current module.
   */
  protected ParamsTask(final P params, final OutputStream reportStream, S stats, final UsageMetric usageMetric) {
    assert params != null;
    mParams = params;
    mReportStream = reportStream;
    mStatistics = stats;
    mUsageMetric = usageMetric;
  }

  /**
   * @return usage counter.
   */
  public long usage() {
    return mUsageMetric.getMetric();
  }

  /**
   * Execute the current task.  Does not return until the task
   * (including possible subtasks) have completed. This implementation
   * manages creation of the output directory, calling the exec
   * method, creating a done file, and closing the params upon
   * completion.
   * @throws IOException if there is a problem.
   */
  @Override
  public void run() throws IOException {
    try {
      exec();
      generateSummary();
      generateReport();
    } finally {
      mParams.close();
    }
  }

  /**
   * Subclasses should do all their work here
   * @throws IOException if there is a problem.
   */
  protected abstract void exec() throws IOException;

  /**
   * Default statistics output delegates to Statistics object
   * @throws IOException if there is a problem.
   */
  protected void generateSummary() throws IOException {
    mStatistics.printStatistics(mReportStream);
  }

  /**
   * Default report generation delegates to Statistics object
   * @throws IOException if there is a problem.
   */
  protected void generateReport() throws IOException {
    mStatistics.generateReport();
  }

  /**
   * @return the parameters for this task.
   */
  public P parameters() {
    return mParams;
  }

  @Override
  public String toString() {
    return mParams.toString();
  }

  /**
   * Get the map containing the statistics from the run
   * @return the map
   */
  public S getStatistics() {
    return mStatistics;
  }

}
