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

/**
 * Provide output to Diagnostic of timer information.
 * This timer is very simple. All it can do is be created and
 * log the time. It uses the same conventions for output format as
 * <code>Timer</code>.
 *
 * Precision is to nano-seconds and output is in fractions of a second.
 *
 * Output to the log is done in a standard form with the word "Time" as the
 * first token followed by an identifier (without any embedded spaces)
 * followed by the total time.
 *
 */
public class OneShotTimer extends AbstractTimer {

  /** The time at the start. */
  private final long mStart;

  /**
   * Create a new timer and start ticking.
   * @param name name of timer
   */
  public OneShotTimer(final String name) {
    super(name);
    mStart = System.nanoTime();
  }

  /**
   * Finish timing and log the resulting interval.
   */
  public void stopLog() {
    final long finish = System.nanoTime();
    final long diff = finish - mStart;
    Diagnostic.developerLog(toString(diff));
  }

  /**
   * @param diff time difference in nanoseconds to be displayed.
   * @return formatted time output using time difference.
   */
  String toString(final long diff) {
    final StringBuilder sb = new StringBuilder();
    sb.append(TIMER_PREFIX).append(mName);
    //System.err.println("start=" + mStart + " finish=" + finish);
    final double time = diff / BILLION;
    TIME_FORMAT.format(sb, time);
    return sb.toString();
  }

  @Override
  public String toString() {
    return mName;
  }
}

