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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests the corresponding class.
 *
 */
public class DiagnosticTest extends TestCase {

  /**
   */
  public DiagnosticTest(final String name) {
    super(name);
  }
  public static Test suite() {
    return new TestSuite(DiagnosticTest.class);
  }
  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  @Override
  public void setUp() {
    mCount = 0;
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    Diagnostic.setLogStream();
  }

  private int mCount = 0;

  private void bump() {
    mCount++;
  }

  public void testListener() {
    Diagnostic.removeListener(null);
    final DiagnosticListener l = new DiagnosticListener() {
        @Override
        public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
          Assert.fail();
        }

        @Override
        public void close() {
        }
      };
    Diagnostic.removeListener(l);
    Diagnostic.addListener(null);
    Diagnostic.addListener(l);
    Diagnostic.addListener(l);
    Diagnostic.removeListener(l);
    // there are no listeners, so nothing happens
    Diagnostic.warning(WarningType.SEQUENCE_TOO_LONG, "hi");
    final DiagnosticListener x = new DiagnosticListener() {
        @Override
        public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
          bump();
        }

        @Override
        public void close() {
        }
      };
    Diagnostic.addListener(x);
    Diagnostic.addListener(x);
    Diagnostic.warning(WarningType.SEQUENCE_TOO_LONG, "hi");
    assertEquals(1, mCount);
    Diagnostic.warning(null, "hi");
    assertEquals(1, mCount);
    try {
      Diagnostic.warning(WarningType.SEQUENCE_TOO_LONG, "hi", "there");
    } catch (final RuntimeException e) {
      assertEquals("SEQUENCE_TOO_LONG:[hi, there]", e.getMessage());
    }
    assertEquals(1, mCount);

    Diagnostic.setLogStream();
    assertEquals(1, mCount);
    Diagnostic.error((ErrorType) null);
    assertEquals(1, mCount);
    Diagnostic.error(ErrorType.DISK_SPACE);
    assertEquals(1, mCount);
    Diagnostic.error(ErrorType.DISK_SPACE, "myfile");
    assertEquals(2, mCount);
    Diagnostic.setLogStream(System.err);
    Diagnostic.removeListener(x);
  }

  public void testLogging() throws IOException {
    Diagnostic.setLogStream();
    assertNull(Diagnostic.getLogStream());
    Diagnostic.setLogStream(System.err);
    assertEquals(System.err, Diagnostic.getLogStream());
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream ps = new PrintStream(bos)) {
        Diagnostic.setLogStream(ps);
        Diagnostic.userLog("log-message");
      }
    } finally {
      bos.close();
    }
    final String t = bos.toString().trim();
    assertTrue(t.endsWith("log-message"));
    assertTrue(t.startsWith("20"));
    assertEquals('-', t.charAt(4));
    assertEquals('-', t.charAt(7));
    assertEquals(' ', t.charAt(10));
    assertEquals(':', t.charAt(13));
    assertEquals(':', t.charAt(16));
    assertEquals(' ', t.charAt(19));
  }

  public void testLoggingEnv() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream ps = new PrintStream(bos)) {
        Diagnostic.setLogStream(ps);
        Diagnostic.logEnvironment();
      }
    } finally {
      bos.close();
    }
    final String t = bos.toString().trim();
    assertEquals('-', t.charAt(4));
    assertEquals('-', t.charAt(7));
    assertEquals(' ', t.charAt(10));
    assertEquals(':', t.charAt(13));
    assertEquals(':', t.charAt(16));
    assertEquals(' ', t.charAt(19));
    assertTrue(t.contains("os.name"));
    assertTrue(t.contains("path.separator"));
    assertTrue(t.contains("user.dir = "));
    assertTrue(t.contains("RTG version "));
  }

  public void testLoggingNasty() throws IOException {
    final PrintStream oldErr = System.err;
    try {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        System.setErr(new PrintStream(output));
        Diagnostic.setLogStream();
        assertNull(Diagnostic.getLogStream());
        Diagnostic.setLogStream(System.err);
        assertEquals(System.err, Diagnostic.getLogStream());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
          final PrintStream ps = new PrintStream(bos);
          ps.close(); // i.e. can't log to this stream
          Diagnostic.setLogStream(ps);
          Diagnostic.userLog("log-message");
          Diagnostic.userLog("log-messagx");
          assertEquals("", bos.toString().trim());
        }
      } finally {
        output.close();
      }
      final String s = output.toString();
      assertTrue(s.contains("log-message"));
      assertTrue(s.contains("log-messagx"));
      final int t = s.indexOf("Logging problem: redirecting logging to System.err.");
      assertTrue(t != -1);
      assertEquals(-1, s.indexOf("Logging problem: redirecting logging to System.err.", t + 1));
    } finally {
      System.setErr(oldErr);
    }
  }

  public void testThrowableLogging() throws IOException {
    final DiagnosticListener x = new DiagnosticListener() {
        @Override
        public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
          Assert.assertTrue(event instanceof ErrorEvent);
          Assert.assertTrue(event.getType() == ErrorType.SLIM_ERROR);
          bump();
        }

        @Override
        public void close() {
        }
      };
    Diagnostic.addListener(x);
    Diagnostic.setLogStream();
    assertNull(Diagnostic.getLogStream());
    Diagnostic.setLogStream(System.err);
    assertEquals(System.err, Diagnostic.getLogStream());
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream ps = new PrintStream(bos)) {
        Diagnostic.setLogStream(ps);
        Diagnostic.userLog(TestUtils.fillStackTrace(new NullPointerException()));
      }
    } finally {
      bos.close();
    }
    assertTrue(bos.toString().contains(NullPointerException.class.getSimpleName()));
    assertEquals(0, mCount);
    Diagnostic.removeListener(x);
  }

  public void testThrowableLoggingNasty() throws IOException {
    final DiagnosticListener x = new DiagnosticListener() {
        @Override
        public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
          Assert.assertTrue(event instanceof ErrorEvent);
          Assert.assertTrue(event.getType() == ErrorType.SLIM_ERROR);
          bump();
        }

        @Override
        public void close() {
        }
      };
    Diagnostic.addListener(x);
    final PrintStream oldErr = System.err;
    try {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        System.setErr(new PrintStream(output));
        Diagnostic.setLogStream();
        assertNull(Diagnostic.getLogStream());
        Diagnostic.setLogStream(System.err);
        assertEquals(System.err, Diagnostic.getLogStream());
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
          final PrintStream ps = new PrintStream(bos);
          ps.close(); // i.e. can't log to this stream
          Diagnostic.setLogStream(ps);
          Diagnostic.userLog(TestUtils.fillStackTrace(new NullPointerException()));
          Diagnostic.userLog(TestUtils.fillStackTrace(new IllegalArgumentException()));
          assertEquals("", bos.toString().trim());
        }
      } finally {
        output.close();
      }
      final String s = output.toString();
      assertTrue(s.contains(NullPointerException.class.getSimpleName()));
      assertTrue(s.contains(IllegalArgumentException.class.getSimpleName()));
      final int t = s.indexOf("Logging problem: redirecting logging to System.err.");
      assertTrue(t != -1);
      assertEquals(-1, s.indexOf("Logging problem: redirecting logging to System.err.", t + 1));
    } finally {
      System.setErr(oldErr);
    }
    assertEquals(0, mCount);
    Diagnostic.removeListener(x);
  }

  public void testDeleteLog() throws IOException {
    final File t = File.createTempFile("log", ".log");
    t.deleteOnExit();
    try {
      Diagnostic.switchLog(t.getPath());
      Diagnostic.userLog("hello");
      assertTrue(t.exists());
      Diagnostic.deleteLog();
      assertFalse(t.exists());
    } finally {
      assertFalse(t.delete());
    }
  }

  public void testWarnErrorMessage() {
    final boolean[] results = new boolean[2];
    final DiagnosticListener testL = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(DiagnosticEvent<?> event) {
        if (event instanceof WarningEvent && event.getType() == WarningType.INFO_WARNING) {
          results[0] = true;
        }
        if (event instanceof ErrorEvent && event.getType() == ErrorType.INFO_ERROR) {
          results[1] = true;
        }
      }

      @Override
      public void close() {
      }
    };
    assertFalse(results[0]);
    assertFalse(results[1]);
    final ByteArrayOutputStream log = new ByteArrayOutputStream();
    Diagnostic.setLogStream(new PrintStream(log));
    try {
      Diagnostic.addListener(testL);
      try {
        Diagnostic.warning("a warning");
        assertTrue(results[0]);
        assertFalse(results[1]);
        Diagnostic.error("an error");
        assertTrue(results[0]);
        assertTrue(results[1]);
      } finally {
        Diagnostic.removeListener(testL);
      }
    } finally {
      Diagnostic.setLogStream();
    }
    final String stripTime = log.toString().replaceAll("[0-9]{4}-[0-9]{1,2}-[0-9]{1,2} [0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2} ", "");
    assertEquals("a warning" + StringUtils.LS + "Error: an error" + StringUtils.LS, stripTime);
  }


  public void testDeveloperLogging() throws IOException {
    Diagnostic.setLogStream();
    assertNull(Diagnostic.getLogStream());
    Diagnostic.setLogStream(System.err);
    assertEquals(System.err, Diagnostic.getLogStream());
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      try (PrintStream ps = new PrintStream(bos)) {
        Diagnostic.setLogStream(ps);
        Diagnostic.developerLog("log-message");
      }
    } finally {
      bos.close();
    }
    final String t = bos.toString().trim();
    assertTrue(t.endsWith("log-message"));
    assertTrue(t.startsWith("20"));
    assertEquals('-', t.charAt(4));
    assertEquals('-', t.charAt(7));
    assertEquals(' ', t.charAt(10));
    assertEquals(':', t.charAt(13));
    assertEquals(':', t.charAt(16));
    assertEquals(' ', t.charAt(19));
  }

 }

