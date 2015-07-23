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

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests the corresponding class.
 *
 */
public class SlimExceptionTest extends TestCase {

  //    public SlimExceptionTest(final String name) {
  //        super(name);
  //    }

  public static Test suite() {
    return new TestSuite(SlimExceptionTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public void test() throws IOException {
    final DiagnosticListener l = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        Assert.assertTrue(event instanceof ErrorEvent);
        bump();
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(l);
    checkExceptionLogging(new SlimException());
    assertEquals(1, mCount);
    checkExceptionLogging(new SlimException(new RuntimeException()));
    assertEquals(2, mCount);
    checkExceptionLogging(new SlimException(ErrorType.NOT_A_DIRECTORY, "dir"));
    assertEquals(3, mCount);
    checkExceptionLogging(new SlimException(new IOException(), ErrorType.NOT_A_DIRECTORY, "dir"));
    assertEquals(4, mCount);
    checkExceptionLogging(new SlimException(new OutOfMemoryError(), ErrorType.SLIM_ERROR));
    assertEquals(5, mCount);

    SlimException se = new SlimException(false, null, null);
    assertEquals("", se.getMessage());
    checkExceptionLogging(se);
    assertEquals(5, mCount);

    se = new SlimException(new RuntimeException(), ErrorType.INFO_ERROR, "blkjsdfk");
    final String expected = "java.lang.RuntimeException";
    assertTrue(se.getMessage().startsWith(expected));
    assertEquals(ErrorType.INFO_ERROR, se.getErrorType());
    checkExceptionLogging(se);
    assertEquals(6, mCount);

    se = new SlimException("bjseh!");
    assertEquals(ErrorType.INFO_ERROR, se.getErrorType());
    checkExceptionLogging(se);
    assertEquals(7, mCount);

    Diagnostic.removeListener(l);
  }

  private void checkExceptionLogging(final SlimException e) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      final PrintStream ps = new PrintStream(bos);
      Diagnostic.setLogStream(ps);
      try {
        e.logException();
        e.printErrorNoLog();
      } finally {
        ps.close();
      }
    } finally {
      Diagnostic.setLogStream();
      bos.close();
    }
    checkLog(bos.toString().trim());
  }

  private int mCount = 0;

  private void bump() {
    mCount++;
  }

  private void checkLog(final String t) {
    assertTrue(t.length() > 0);
  }
}

