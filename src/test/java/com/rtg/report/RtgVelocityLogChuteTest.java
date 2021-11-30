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
package com.rtg.report;

import org.apache.velocity.runtime.log.LogChute;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class RtgVelocityLogChuteTest extends TestCase {

  public void test() {
    try (final MemoryPrintStream mps = new MemoryPrintStream()) {
      Diagnostic.setLogStream(mps.printStream());
      final RtgVelocityLogChute chute = new RtgVelocityLogChute();
      chute.log(LogChute.TRACE_ID, "not gonna log");
      chute.log(LogChute.DEBUG_ID, "totally logged");
      assertFalse(chute.isLevelEnabled(LogChute.TRACE_ID));
      assertTrue(chute.isLevelEnabled(LogChute.DEBUG_ID));
      assertTrue(chute.isLevelEnabled(LogChute.INFO_ID));
      assertFalse(mps.toString().contains("not gonna log"));
      assertTrue(mps.toString().contains("totally logged"));
    } finally {
      Diagnostic.setLogStream();
    }
  }
}
