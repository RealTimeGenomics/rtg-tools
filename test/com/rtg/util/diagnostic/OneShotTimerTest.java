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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.TestCase;

/**
 */
public class OneShotTimerTest extends TestCase {

  /**
   * Test method for {@link com.rtg.util.diagnostic.OneShotTimer#stopLog()}.
   * @throws IOException if an I/O error occurs.
   */
  public final void testStopLog() throws IOException {
    final ByteArrayOutputStream ba = new ByteArrayOutputStream();
    final PrintStream pr = new PrintStream(ba);
    Diagnostic.setLogStream(pr);
    final long start = System.nanoTime();
    final OneShotTimer ti = new OneShotTimer("One_timer");
    ti.stopLog();
    final long finish = System.nanoTime();

    final String s = ba.toString();
    //System.err.println(s);
    assertTrue(s.contains(" Timer One_timer "));
    double time = Double.parseDouble(s.substring(s.lastIndexOf(' ')));
    double timeActual = (finish - start) / 1000000000.0;
    assertEquals(time, timeActual, 0.1);
    pr.close();
    ba.close();
    Diagnostic.setLogStream();
  }

  public final void testFormat() {
    final OneShotTimer ti = new OneShotTimer("One_timer");
    assertEquals("Timer One_timer      0.12", ti.toString(123456789L));
    assertEquals("One_timer", ti.toString());
  }
}

