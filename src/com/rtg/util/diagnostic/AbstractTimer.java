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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.format.FormatReal;

/**
 * Provides some support for the two timer classes
 * <code>(Timer, OneShotTimer)</code> to ensure that they use the
 * same output formats.
 *
 */
@TestClass(value = {"com.rtg.util.diagnostic.TimerTest"})
public abstract class AbstractTimer {

  /** Used as start of output to log (enables timing information to be easily extracted from logs). */
  protected static final String TIMER_PREFIX = "Timer ";

  protected static final FormatReal TIME_FORMAT = new FormatReal(7, 2);

  /** Divisor for calculating seconds from nano-seconds. */
  protected static final double BILLION = 1000000000.0;

  /** The name of the timer (not permitted to contain any spaces). */
  protected String mName;

  /**
   * Create a new timer and start ticking.
   * @param name of times is used in printing timing results.
   */
  public AbstractTimer(final String name) {
    if (name != null && name.contains(" ")) {
      throw new IllegalArgumentException("Name contains spaces:" + name);
    }
    mName = name;
  }

  /**
   * Set name of timer.
   * @param name of timer.
   */
  public void reset(final String name) {
    mName = name;
  }

}

