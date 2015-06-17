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

package com.rtg.util.arithcode;

import junit.framework.TestCase;

/**
 */
public class StaticModelTest extends TestCase {

  public void test() {
    final int[] counts = {0, 5, 2};

    final StaticModel mo = new StaticModel(counts);
    assertEquals(10, mo.totalCount());

    assertEquals(0, mo.pointToSymbol(0));

    assertEquals(1, mo.pointToSymbol(1));
    assertEquals(1, mo.pointToSymbol(2));
    assertEquals(1, mo.pointToSymbol(3));
    assertEquals(1, mo.pointToSymbol(4));
    assertEquals(1, mo.pointToSymbol(5));
    assertEquals(1, mo.pointToSymbol(6));

    assertEquals(2, mo.pointToSymbol(7));
    assertEquals(2, mo.pointToSymbol(8));
    assertEquals(2, mo.pointToSymbol(9));

    //assertEquals(3, mo.pointToSymbol(10));
    final int[] result = new int[3];
    mo.interval(0, result);
    assertEquals(0, result[0]);
    assertEquals(1, result[1]);
    assertEquals(10, result[2]);

    mo.interval(1, result);
    assertEquals(1, result[0]);
    assertEquals(7, result[1]);
    assertEquals(10, result[2]);

    mo.interval(2, result);
    assertEquals(7, result[0]);
    assertEquals(10, result[1]);
    assertEquals(10, result[2]);
  }
}
