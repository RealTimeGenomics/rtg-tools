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
 * @author Dave Ware
 */
public class MergedIntervalsTest extends TestCase {


  public void test() {
    MergedIntervals mi = new MergedIntervals();
    mi.add(5, 10);
    mi.add(20, 30);
    mi.add(31, 40);
    mi.add(200, 300);
    mi.add(310, 390);
    mi.add(400, 410);
    mi.add(300, 400);

    assertFalse(mi.overlapped(0, 4));
    assertFalse(mi.overlapped(0, 5));
    assertFalse(mi.overlapped(10, 20));
    assertFalse(mi.overlapped(30, 31));
    assertTrue(mi.overlapped(0, 10));
    assertTrue(mi.overlapped(0, 20));
    assertTrue(mi.overlapped(0, 30));
    assertTrue(mi.overlapped(0, 500));
//
    assertTrue(mi.enclosed(6, 9));
    assertTrue(mi.enclosed(6, 10));
    assertTrue(mi.enclosed(5, 10));
    assertFalse(mi.enclosed(4, 10));
    assertFalse(mi.enclosed(5, 11));
    assertFalse(mi.enclosed(5, 30));
    assertFalse(mi.enclosed(30, 31));
    assertFalse(mi.enclosed(30));
    assertTrue(mi.enclosed(29));
    assertTrue(mi.enclosed(29, 30));
    assertTrue(mi.enclosed(31));
    assertTrue(mi.enclosed(31, 32));

    assertTrue(mi.enclosed(200, 300));
    assertTrue(mi.enclosed(300));
    assertTrue(mi.enclosed(350));
    assertTrue(mi.enclosed(300, 310));
    assertTrue(mi.enclosed(390, 400));
  }

}