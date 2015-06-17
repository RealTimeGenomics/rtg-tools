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
import java.io.PrintStream;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 *
 */
public class NoTalkbackSlimExceptionTest extends TestCase {

  public NoTalkbackSlimExceptionTest(String name) {
    super(name);
  }

  public final void testNoTalkbackSlimExceptionString() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream perr = new PrintStream(bos);
    Diagnostic.setLogStream(perr);
    try {
      final NoTalkbackSlimException e = new NoTalkbackSlimException(new RuntimeException(), ErrorType.INFO_ERROR, "really really long message");
      e.logException();
      perr.flush();
      //System.err.println(bos.toString());
      TestUtils.containsAll(bos.toString(), "really really long message");
    } finally {
      Diagnostic.setLogStream();
      perr.close();
    }

  }

  public final void testNoTalkbackSlimExceptionString2() {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final PrintStream perr = new PrintStream(bos);
    Diagnostic.setLogStream(perr);
    try {
      final NoTalkbackSlimException e = new NoTalkbackSlimException("really really long message");
      e.logException();
      perr.flush();
      //System.err.println(bos.toString());
      TestUtils.containsAll(bos.toString(), "really really long message");

    } finally {
      Diagnostic.setLogStream();
      perr.close();
    }

  }
}
