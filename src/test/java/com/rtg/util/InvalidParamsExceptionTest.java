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

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.io.LogRecord;
import com.rtg.util.io.LogStream;

import junit.framework.TestCase;

/**
 */
public class InvalidParamsExceptionTest extends TestCase {


  @Override
  protected void tearDown() {
    Diagnostic.setLogStream();
  }

  public void test() {
    final LogStream rec = new LogRecord();
    Diagnostic.setLogStream(rec);
    try {
      throw new InvalidParamsException(ErrorType.SLIM_ERROR);
    } catch (final InvalidParamsException e) {
      assertEquals(ErrorType.SLIM_ERROR, e.getErrorType());
    }
    //System.err.println(rec.toString());
  }

  public void testOtherConstructor() {
    final LogStream rec = new LogRecord();
    Diagnostic.setLogStream(rec);
    final DiagnosticListener listener = new DiagnosticListener() {
      @Override
      public void close() {
      }
      @Override
      public void handleDiagnosticEvent(DiagnosticEvent<?> event) {
        assertEquals(ErrorType.INFO_ERROR, event.getType());
      }
    };
    Diagnostic.addListener(listener);
    try {
      try {
        throw new InvalidParamsException("This is a message");
      } catch (final InvalidParamsException e) {
        assertEquals("This is a message", e.getMessage());
      }
    } finally {
      Diagnostic.removeListener(listener);
    }
  }
}
