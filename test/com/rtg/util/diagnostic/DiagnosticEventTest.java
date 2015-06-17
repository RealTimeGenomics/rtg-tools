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

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the corresponding class.
 *
 */
public final class DiagnosticEventTest extends AbstractDiagnosticEventTest {

  /**
   */
  public DiagnosticEventTest(final String name) {
    super(name);
  }
  public static Test suite() {
    return new TestSuite(DiagnosticEventTest.class);
  }
  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  @Override
  public DiagnosticEvent<?> getEvent() {
    return new DiagnosticEvent<ErrorType>(ErrorType.NOT_A_DIRECTORY, "1") { };
  }

  @Override
  public Class<?> getEnumClass() {
    return ErrorType.class;
  }

  public void testExplicitMessageForProgress() {
    assertTrue(getEvent().getMessage().contains("\"1\""));
  }

  public void test() {
    try {
      new DiagnosticEvent<>(ErrorType.NOT_A_DIRECTORY);
    } catch (IllegalArgumentException e) {
      assertEquals(ErrorType.NOT_A_DIRECTORY + ":" + 0 + ":" + 1, e.getMessage());
    }
  }

  static final class MyErrorType implements DiagnosticType, PseudoEnum {
    public static final MyErrorType NO_SUCH_ERROR = new MyErrorType(0, "NO_SUCH_ERROR");

    private final int mOrdinal;
    private final String mName;
    private MyErrorType(final int ordinal, final String name) {
      mOrdinal = ordinal;
      mName = name;
    }
    @Override
    public int ordinal() {
      return mOrdinal;
    }
    @Override
    public String name() {
      return mName;
    }

    public String toString() {
      return mName;
    }
    private static final EnumHelper<MyErrorType> HELPER = new EnumHelper<>(MyErrorType.class, new MyErrorType[] {NO_SUCH_ERROR});

    public static MyErrorType valueOf(final String str) {
      return HELPER.valueOf(str);
    }

    public static MyErrorType[] values() {
      return HELPER.values();
    }
    @Override
    public int getNumberOfParameters() {
      return 0;
    }
    @Override
    public String getMessagePrefix() {
      return "";
    }
  }

  public void testNonExistentErrorType() throws IOException {
    try {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        try (PrintStream ps = new PrintStream(bos)) {
          Diagnostic.setLogStream(ps);
          assertEquals("NO_SUCH_ERROR", new DiagnosticEvent<MyErrorType>(MyErrorType.NO_SUCH_ERROR) {
          }.getMessage());
        }
      } finally {
        bos.close();
      }
      final String t = bos.toString().trim();
      assertTrue(t.endsWith("Missing resource information for diagnostic: NO_SUCH_ERROR"));
      assertTrue(t.startsWith("20"));
      assertEquals('-', t.charAt(4));
      assertEquals('-', t.charAt(7));
      assertEquals(' ', t.charAt(10));
      assertEquals(':', t.charAt(13));
      assertEquals(':', t.charAt(16));
      assertEquals(' ', t.charAt(19));
    } finally {
      Diagnostic.setLogStream();
    }
  }
}

