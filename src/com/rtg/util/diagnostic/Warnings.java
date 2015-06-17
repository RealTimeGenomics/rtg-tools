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

import java.util.ArrayList;
import java.util.List;

/**
 * Enable easy handling of multiple warnings where you only want to print out some smallish initial number of them and
 * at the end summarize the total number.
 */
public final class Warnings {

  private final List<Warning> mWarnings = new ArrayList<>();

  /**
   * Generate a report to the log from all the current warnings.
   */
  public void report() {
    for (final Warning warn : mWarnings) {
      warn.finalMsg();
    }
  }

  /**
   * Create a new warning registered with this warning set.
   * @param limit maximum number of warnings to be written (ones after this are counted and the total written at the end).
   * @param message to be written for each warning that is written and in final summary when needed.
   * @param logAll true iff all warnings are written to the user log.
   * @return the newly created warning.l
   */
  public Warning create(int limit, String message, boolean logAll) {
    final Warning warning = new Warning(limit, message, logAll);
    mWarnings.add(warning);
    return warning;
  }

  /**
   * A single warning that will issue a limited number of times.
   */
  public static class Warning {

    private final String mMessage;

    private final int mLimit;

    private final boolean mLogAll;

    private long mCount = 0L;

    /**
     * @param limit maximum number of warnings to be written (ones after this are counted and the total written at the end).
     * @param message to be written for each warning that is written and in final summary when needed.
     * @param logAll true iff all warnings are written to the user log.
     */
    public Warning(int limit, String message, boolean logAll) {
      assert limit >= 1;
      mLogAll = logAll;
      mLimit = limit;
      mMessage = message;
    }

    /**
     * Conditionally output a warning and count how many warnings have occurred.
     * @param msg to be included in the warning (appended to this objects warning).
     */
    public void warn(final String msg) {
      mCount++;
      if (mCount <= mLimit) {
        Diagnostic.warning(mMessage + " " + msg);
      } else {
        if (mLogAll) {
          Diagnostic.userLog(mMessage + " " + msg);
        }
      }
    }

    private void finalMsg() {
      if (mCount < mLimit) {
        return;
      }
      final String msg = mMessage + " occurred " + mCount + " times";
      Diagnostic.warning(msg);
    }
  }
}
