/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import static com.rtg.util.StringUtils.LS;

import junit.framework.TestCase;

/**
 */
public class HistogramWithNegativesTest extends TestCase {

  public void test() {
    final HistogramWithNegatives hist = new HistogramWithNegatives();
    assertEquals(0, hist.getLength());
    assertEquals(Integer.MAX_VALUE, hist.min());
    assertEquals("[]" + LS, hist.toString());
    hist.increment(3);
    assertEquals(3, hist.min());
    assertEquals(1, hist.getLength());
    assertEquals("[3..3]" + LS + "1 " + LS, hist.toString());
    assertEquals(1, hist.getValue(3));
    hist.increment(2, 10);
    assertEquals(2, hist.min());
    assertEquals(2, hist.getLength());
    assertEquals(10, hist.getValue(2));
    hist.increment(0);
    assertEquals(0, hist.min());
    final String exp = ""
      + "[0..3]" + LS
      + "1 0 10 1 " + LS
      ;
    assertEquals(exp, hist.toString());
    hist.increment(9, 9);
    assertEquals(10, hist.getLength());
    final String exp9 = ""
      + "[0..9]" + LS
      + "1 0 10 1 0 0 0 0 0 9 " + LS
      ;
    assertEquals(exp9, hist.toString());
  }

  public void testBug() {
    final HistogramWithNegatives hist = new HistogramWithNegatives();
    assertEquals(0, hist.getLength());
    assertEquals(Integer.MAX_VALUE, hist.min());
    assertEquals("[]" + LS, hist.toString());
    hist.increment(-3); //bug seen in wild
    assertEquals(-3, hist.min());
    assertEquals(1, hist.getLength());
    assertEquals("[-3..-3]" + LS + "1 " + LS, hist.toString());
    assertEquals(1, hist.getValue(-3));
  }

}
