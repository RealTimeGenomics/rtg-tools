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
package com.rtg.util.intervals;

import junit.framework.TestCase;

/**
 */
public class StatusIntervalTest extends TestCase {

  private static final byte FOO = 1;
  private static final byte BAR = 2;

  public void test() {
    try {
      new StatusInterval(5, 5);
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    final StatusInterval i = new StatusInterval(5, 10);
    for (int k = 5; k < 10; ++k) {
      assertFalse(i.contains(k));
    }
    try {
      i.contains(4);
    } catch (ArrayIndexOutOfBoundsException e) {
      // Expected
    }
    try {
      i.contains(10);
    } catch (ArrayIndexOutOfBoundsException e) {
      // Expected
    }
    i.add(2, 3, FOO);
    i.add(7, 8, FOO);
    for (int k = 5; k < 10; ++k) {
      assertTrue((k == 7) ^ !i.contains(k));
    }
    i.add(0, 100, FOO);
    for (int k = 5; k < 10; ++k) {
      assertTrue(i.contains(k));
      assertTrue(i.get(k) == FOO);
    }
    i.add(9, 10, BAR);
    assertTrue(i.contains(9));
    assertTrue(i.get(9) == BAR);

  }
}
