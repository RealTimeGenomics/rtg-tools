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

import java.util.HashMap;

/**
 * Utility class for reporting progress from multiple threads using the trailing
 * edge of the reports from individual threads.
 *
 */
public class ParallelProgress {

  private static class MyInteger {
    private int mValue = 0;

    MyInteger(final int v) {
      mValue = v;
    }

    int getValue() {
      return mValue;
    }

    void setValue(int value) {
      mValue = value;
    }

  }

  private final String mName;
  private final HashMap<Thread, MyInteger> mIndividualThreadProgress = new HashMap<>();
  private int mLastReportedProgress = -1;

  /**
   * Create progress reporter.
   * @param name name of progress
   */
  public ParallelProgress(final String name) {
    mName = name;
    Diagnostic.progress("Starting: " + mName);
  }

  /**
   * Update progress, safe to use for multithreading.
   * @param value position in current thread
   */
  public synchronized void updateProgress(final int value) {
    final Thread t = Thread.currentThread();
    final MyInteger v = mIndividualThreadProgress.get(t);
    if (v == null) {
      mIndividualThreadProgress.put(t, new MyInteger(value));
    } else {
      v.setValue(value);
    }
    // Only bother to check for global update if this value exceeds last report
    if (value > mLastReportedProgress) {
      int min = Integer.MAX_VALUE;
      for (final MyInteger m : mIndividualThreadProgress.values()) {
        if (m.getValue() < min) {
          min = m.getValue();
        }
      }
      if (min > mLastReportedProgress) {
        mLastReportedProgress = min;
        Diagnostic.progress("Processed " + min + "% of " + mName);
      }
    }
  }

  /**
   * Finish reporting progress.
   */
  public void close() {
    Diagnostic.progress("Finished: " + mName);
  }
}
