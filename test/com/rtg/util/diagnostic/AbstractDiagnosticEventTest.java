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


import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public abstract class AbstractDiagnosticEventTest extends TestCase {

  public abstract DiagnosticEvent<?> getEvent();


  public abstract Class<?> getEnumClass();

  public void testEventParameters() {
    final DiagnosticEvent<?> event = getEvent();
    assertNotNull(event.getType());
    final String[] params = event.getParams();
    assertNotNull(params);
    if (params.length > 0) {
      params[0] = "hi_there_bilbo";
      assertFalse(params[0].equals(event.getParams()[0]));
    }
  }

  public void testMessageGeneration() throws Exception {
    final String methodName = "values";
    for (final DiagnosticType type : (DiagnosticType[]) getEnumClass().getMethod(methodName, new Class<?>[] {}).invoke(null)) {
      final String[] params = new String[type.getNumberOfParameters()];
      for (int k = 0; k < params.length; ++k) {
        params[k] = "XX" + k + "XX";
      }
      final String message = new DiagnosticEvent<>(type, params).getMessage();
      assertNotNull(message);
      assertTrue(message.length() > 0);
      for (String param : params) {
        assertTrue(message, message.contains(param));
      }
    }
  }

}

