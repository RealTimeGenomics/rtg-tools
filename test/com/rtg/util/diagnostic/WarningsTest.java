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

import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class WarningsTest extends TestCase {

  public void test() {
    Diagnostic.clearListeners();
    final MemoryPrintStream err = new MemoryPrintStream();
    final MemoryPrintStream out = new MemoryPrintStream();
    final CliDiagnosticListener listener = new CliDiagnosticListener(err.printStream(), out.printStream());
    Diagnostic.addListener(listener);
    Diagnostic.setLogStream();

    final MemoryPrintStream mps = new MemoryPrintStream();
    Diagnostic.setLogStream(mps.printStream());

    final Warnings warnings = new Warnings();
    final Warnings.Warning spy1 = warnings.create(2, "foo1", false);
    final Warnings.Warning spy2 = warnings.create(1, "bar2", true);

    spy1.warn("A");
    spy2.warn("B");
    spy2.warn("C");

    warnings.report();
    mps.close();

    final String t = mps.toString().trim();
    //System.err.println(t);
    assertTrue(t.contains("foo1 A"));
    assertTrue(t.contains("bar2 B"));
    assertTrue(t.contains("bar2 C"));
    assertTrue(t.contains("bar2 occurred 2 times"));

    err.close();
    final String e = err.toString();
    //System.err.println(e);
    assertTrue(e.contains("foo1 A"));
    assertTrue(e.contains("bar2 B"));
    assertFalse(e.contains("bar2 C"));
    assertTrue(e.contains("bar2 occurred 2 times"));

    out.close();
    assertEquals("", out.toString());
    Diagnostic.setLogStream();
  }

}
