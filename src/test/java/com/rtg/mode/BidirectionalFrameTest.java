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
public class BidirectionalFrameTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.BidirectionalFrame}.
   */
  public final void test() {
    TestUtils.testPseudoEnum(BidirectionalFrame.class, "[FORWARD, REVERSE]");
    assertEquals(BidirectionalFrame.FORWARD, BidirectionalFrame.REVERSE.getReverse());
    assertEquals(BidirectionalFrame.REVERSE, BidirectionalFrame.FORWARD.getReverse());
  }

  /**
   * Test method for {@link com.rtg.mode.BidirectionalFrame}.
   */
  public final void testValues() {
    for (final BidirectionalFrame bi : BidirectionalFrame.values()) {
      assertEquals(bi, BidirectionalFrame.frameFromOrdinal(bi.ordinal()));
    }
  }

  /**
   * Test method for {@link com.rtg.mode.BidirectionalFrame#display()}.
   */
  public final void testDisplay() {
    assertEquals("F", BidirectionalFrame.FORWARD.display());
    assertEquals("R", BidirectionalFrame.REVERSE.display());
  }

  /**
   * Test method for {@link com.rtg.mode.BidirectionalFrame#display()}.
   */
  public final void testIsForward() {
    assertTrue(BidirectionalFrame.FORWARD.isForward());
    assertFalse(BidirectionalFrame.REVERSE.isForward());
  }

  /**
   * Test method for {@link com.rtg.mode.BidirectionalFrame#phase()}.
   */
  public final void testPhase() {
    for (final BidirectionalFrame bi : BidirectionalFrame.values()) {
      assertEquals(0, bi.phase());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#frameFromCode(int)}.
   */
  public final void testFrameFromCodeBad() {
    try {
      BidirectionalFrame.frameFromOrdinal(-1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      BidirectionalFrame.frameFromOrdinal(2);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeF() {
    final byte[] codes = {0, 1, 2, 3};
    final Frame f = BidirectionalFrame.FORWARD;
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

  public final void testCodeR() {
    final byte[] codes = {0, 1, 2, 3, 4};
    final Frame f = BidirectionalFrame.REVERSE;
    assertEquals(1, f.code(codes, 5, 0));
    assertEquals(2, f.code(codes, 4, 0));
    assertEquals(3, f.code(codes, 3, 0));
    assertEquals(4, f.code(codes, 3, 1));
    assertEquals(0, f.code(codes, 3, 2));
    try {
      f.code(codes, 3, 3);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      f.code(codes, 3, -1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      assertEquals("length=3 index=-1", e.getMessage());
    }
  }

  public final void testComplement() {
    for (final DNA dna : DNA.values()) {
      assertEquals(dna.complement(), DNA.values()[BidirectionalFrame.complement((byte) dna.ordinal())]);
    }
  }
}

