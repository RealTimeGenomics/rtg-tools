/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.sv.bndeval;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class IntervalTest extends TestCase {

  public void test1() {
    final Interval ii = new Interval(1, 10);
    ii.integrity();
    assertEquals(1, ii.getA());
    assertEquals(10, ii.getB());
    assertEquals(System.identityHashCode(ii), ii.hashCode());

    final Interval ni = ii.negative();
    ni.integrity();
    assertEquals(-1, ni.getA());
    assertEquals(-10, ni.getB());
  }

  public void test2() {
    final Interval ii = new Interval(-1, -10);
    ii.integrity();
    assertEquals(-1, ii.getA());
    assertEquals(-10, ii.getB());

    final Interval ni = ii.negative();
    ni.integrity();
    assertEquals(1, ni.getA());
    assertEquals(10, ni.getB());
  }

  public void testEquals() {
    TestUtils.equalsTest(new Interval[] {
        new Interval(-1, 1),
        new Interval(0, 1),
        new Interval(-1, 0)
    });
    assertEquals(new Interval(-1, 1), new Interval(-1, 1));
  }
}
