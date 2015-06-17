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

import junit.framework.TestCase;


/**
 * Test Integer Or Percentage
 *
 */
public class IntegerOrPercentageTest extends TestCase {
  public void testConstructor() {
    IntegerOrPercentage p = new IntegerOrPercentage("10%");
    assertTrue(p.isPercentage());
    assertEquals(10, p.getValue(100));
    assertEquals(200, p.getValue(2000));
    assertEquals("10%", p.toString());
    IntegerOrPercentage q = new IntegerOrPercentage("10");
    assertFalse(q.isPercentage());
    assertEquals(10, q.getValue(100));
    assertEquals(10, q.getValue(2000));
    assertEquals("10", q.toString());

    assertTrue(p.compareTo(q) < 0);
    assertTrue(q.compareTo(p) > 0);
    assertTrue(p.compareTo(new IntegerOrPercentage("15%")) < 0);
    assertEquals(0, q.compareTo(q));
    assertEquals(0, q.compareTo(q));

    IntegerOrPercentage r = new IntegerOrPercentage(15);
    assertFalse(r.isPercentage());
    assertEquals(15, r.getValue(100));
    assertEquals(15, r.getValue(2000));

    assertEquals(p, new IntegerOrPercentage("10%"));
    assertEquals(p, IntegerOrPercentage.valueOf("10%"));
    assertEquals(r, IntegerOrPercentage.valueOf(15));

    assertFalse(r.equals(p));
    assertFalse(r.equals("Monkey"));

    assertEquals(10, p.getRawValue());

    // Shuts jumble up doesn't really do much else
    assertEquals(7038, p.hashCode());
    assertEquals(7037, q.hashCode());
  }
}
