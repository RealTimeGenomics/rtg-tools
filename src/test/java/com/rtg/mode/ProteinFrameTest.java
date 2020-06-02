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
package com.rtg.mode;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class ProteinFrameTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame}.
   */
  public final void test() {
    TestUtils.testPseudoEnum(ProteinFrame.class, "[PROTEIN]");
    try {
      ProteinFrame.PROTEIN.getReverse();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported", e.getMessage());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#display()}.
   */
  public final void testCode() {
    for (final ProteinFrame bi : ProteinFrame.values()) {
      assertEquals(bi, ProteinFrame.frameFromCode(bi.ordinal()));
    }
  }

  public final void testMode() {
    ProteinFrame.values();
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#phase()}.
   */
  public final void testPhase() {
    for (final ProteinFrame pr : ProteinFrame.values()) {
      assertEquals(0, pr.phase());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#display()}.
   */
  public final void testDisplay() {
    assertEquals("", ProteinFrame.PROTEIN.display());
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#isForward()}.
   */
  public final void testIsForward() {
    assertTrue(ProteinFrame.PROTEIN.isForward());
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#frameFromCode(int)}.
   */
  public final void testFrameFromCodeBad() {
    try {
      ProteinFrame.frameFromCode(-1);
      fail("IllegalArgumentException expected");
    } catch (final IllegalArgumentException e) {
      assertEquals("-1", e.getMessage());
    }
    try {
      ProteinFrame.frameFromCode(1);
      fail("IllegalArgumentException expected");
    } catch (final IllegalArgumentException e) {
      assertEquals("1", e.getMessage());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#code(byte[], int, int)}.
   */
  public final void testCode1() {
    final byte[] codes = {0, 1, 2, 3};
    final Frame f = ProteinFrame.PROTEIN;
    assertEquals(0, f.code(codes, 3, 0));
    assertEquals(1, f.code(codes, 3, 1));
    assertEquals(2, f.code(codes, 3, 2));
    assertEquals(0, f.code(codes, 3, 3));
    try {
      f.code(codes, 3, -1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }
}

