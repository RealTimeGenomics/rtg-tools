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

package com.rtg.util.integrity;

import com.rtg.util.integrity.Exam.ExamException;

import junit.framework.TestCase;

/**
 */
public class IntegerRangeTest extends TestCase {

  protected IntegerRange getRange() {
    return new IntegerRange(1, 10);
  }

  public void test1() {
    final IntegerRange ir = new IntegerRange(1, 10);
    ir.integrity();
    assertTrue(ir.inRange(5));
    assertEquals(1, ir.low());
    assertEquals(10, ir.high());
    assertFalse(ir.hasInvalid());
    assertEquals("[1...10]", ir.toString());
    try {
      ir.checkValid(0);
      fail();
    } catch (final ExamException e) {
      // expected
      assertEquals("0:[1...10]", e.getMessage());
    }

  }

  public void test2() {
    final IntegerRange ir = new IntegerRange(0, 1, 10);
    ir.integrity();
    assertTrue(ir.inRange(5));
    assertEquals(1, ir.low());
    assertEquals(10, ir.high());
    assertTrue(ir.hasInvalid());
    assertEquals("0i[1...10]", ir.toString());
    check(ir);
  }

  public void testAll() {
    final IntegerRange ir = getRange();
    check(ir);
  }

  //WARNING: dangerous for large ranges - override as necessary
  private void check(final IntegerRange ir) {
    ir.integrity();
    for (int i = ir.low(); i < ir.high(); ++i) {
      if (ir.hasInvalid() && i == ir.invalid()) {
        check(ir, i);
      } else {
        checkValid(ir, i);
      }
    }

    final int lowm = ir.low() - 1 - (ir.hasInvalid() ? 1 : 0);
    if (lowm < ir.low()) {
      checkOutRange(ir, lowm);
    }
    final int hip = ir.high() + 1;
    if (hip > ir.high()) {
      checkOutRange(ir, hip);
    }

    if (ir.hasInvalid()) {
      assertTrue(ir.inRange(ir.invalid()));
      assertFalse(ir.valid(ir.invalid()));
    } else {
      try {
        ir.invalid();
        fail();
      } catch (final RuntimeException e) {
        // Expected
      }
    }
  }

  private void checkOutRange(final IntegerRange ir, final int i) {
    assertFalse(ir.valid(i));
    assertFalse(ir.inRange(i));
    try {
      ir.check(i);
      fail();
    } catch (final ExamException e) {
      // expected
    }
    try {
      ir.checkValid(i);
      fail();
    } catch (final ExamException e) {
      // expected
    }
  }

  public void testInvalid() {
    final IntegerRange ir = getRange();
    if (!ir.hasInvalid()) {
      try {
        ir.invalid();
        fail();
      } catch (final RuntimeException e) {
        assertEquals("No invalid value defined", e.getMessage());
      }
    } else {
      check(ir, ir.invalid());
    }
  }

  private void check(final IntegerRange ir, final int i) {
    assertTrue(ir.check(i));
    assertTrue(ir.inRange(i));
  }

  private void checkValid(final IntegerRange ir, final int i) {
    assertTrue(ir.checkValid(i));
    assertTrue(ir.valid(i));
    assertEquals(i, ir.valueOf(ir.toString(i)));
    check(ir, i);
  }
}
