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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;


/**
 */
public class ChooseMemoryTest extends TestCase {

  public void test() {
    final PrintStream o = System.out;
    final PrintStream e = System.err;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ByteArrayOutputStream err = new ByteArrayOutputStream();
    final PrintStream pout = new PrintStream(out);
    final PrintStream perr = new PrintStream(err);
    System.setOut(pout);
    System.setErr(perr);
    ChooseMemory.main(new String[] {});
    ChooseMemory.main(new String[] {"0"});
    ChooseMemory.main(new String[] {"101"});
    ChooseMemory.main(new String[] {"100"});
    ChooseMemory.main(new String[] {});
    pout.flush();
    perr.flush();
    final String outString = out.toString();
    final String errString = err.toString();
    assertTrue(outString.contains("m"));
    TestUtils.containsAll(errString, "Percentage must be greater than 0."
                                   , "Percentage must be less than or equal to 100.");
    pout.close();
    perr.close();
    System.setOut(o);
    System.setErr(e);
  }
}
