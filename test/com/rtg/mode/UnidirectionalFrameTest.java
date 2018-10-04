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
public class UnidirectionalFrameTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame}.
   */
  public final void test() {
    TestUtils.testPseudoEnum(UnidirectionalFrame.class, "[FORWARD]");
    try {
      UnidirectionalFrame.FORWARD.getReverse();
      fail();
    } catch (UnsupportedOperationException e) {
      assertEquals("Not supported", e.getMessage());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame#display()}.
   */
  public final void testCode() {
    for (final UnidirectionalFrame bi : UnidirectionalFrame.values()) {
      assertEquals(bi, UnidirectionalFrame.frameFromCode(bi.ordinal()));
    }
  }

  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame#phase()}.
   */
  public final void testPhase() {
    for (final UnidirectionalFrame un : UnidirectionalFrame.values()) {
      assertEquals(0, un.phase());
    }
  }


  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame#display()}.
   */
  public final void testDisplay() {
    assertEquals("", UnidirectionalFrame.FORWARD.display());
  }

  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame#isForward()}.
   */
  public final void testIsForward() {
    assertTrue(UnidirectionalFrame.FORWARD.isForward());
  }

  /**
   * Test method for {@link com.rtg.mode.UnidirectionalFrame#frameFromCode(int)}.
   */
  public final void testFrameFromCodeBad() {
    try {
      UnidirectionalFrame.frameFromCode(-1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      // Expected
    }
    try {
      UnidirectionalFrame.frameFromCode(1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      // Expected
    }
  }

  /**
   * Test method for {@link com.rtg.mode.ProteinFrame#code(byte[], int, int)}.
   */
  public final void testCode1() {
    final byte[] codes = {0, 1, 2, 3};
    final Frame f = UnidirectionalFrame.FORWARD;
    assertEquals(0, f.code(codes, 3, 0));
    assertEquals(1, f.code(codes, 3, 1));
    assertEquals(2, f.code(codes, 3, 2));
    try {
      f.code(codes, 3, 3);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      assertEquals("length=3 index=3", e.getMessage());
    }
    try {
      f.code(codes, 3, -1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }
}

