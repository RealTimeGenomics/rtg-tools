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
package com.rtg.util;

import com.rtg.util.ProgramState.SlimAbortException;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.LogRecord;

import junit.framework.TestCase;

/**
 */
public class ProgramStateTest extends TestCase {

  public void test() {
    final LogRecord log = new LogRecord();
    Diagnostic.setLogStream(log);
    //be careful this is all static stuff so cant be sure of the state coming in.
    final String msg = "Aborting operation in thread: ";
    assertFalse(log.toString().contains(msg));
    ProgramState.checkAbort();
    assertFalse(log.toString().contains(msg));
    ProgramState.setAbort();
    try {
      ProgramState.checkAbort();
      fail();
    } catch (final SlimAbortException e) {
      assertTrue(e.getMessage().contains(msg));
      assertTrue(log.toString().contains(msg));
    }
    Diagnostic.setLogStream();
    final LogRecord log2 = new LogRecord();
    Diagnostic.setLogStream(log2);
    ProgramState.clearAbort();
    ProgramState.checkAbort();
    assertFalse(log2.toString().contains(msg));
    Diagnostic.setLogStream();
  }
}
