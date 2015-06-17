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
 */
public class HistogramTest extends TestCase {

  public void test() {
    final Histogram hist = new Histogram();
    assertEquals(0, hist.getLength());
    assertEquals("", hist.toString());
    hist.increment(3);
    assertEquals(4, hist.getLength());
    assertEquals("0\t0\t0\t1", hist.toString());
    for (int i = 0; i < 3; i++) {
      assertEquals(0, hist.getValue(i));
    }
    assertEquals(1, hist.getValue(3));
    hist.increment(2, 10);
    assertEquals(4, hist.getLength());
    assertEquals(10, hist.getValue(2));
    hist.increment(0);
    assertEquals("1\t0\t10\t1", hist.toString());
    hist.increment(9, 9);
    assertEquals(10, hist.getLength());
    assertEquals("1\t0\t10\t1\t0\t0\t0\t0\t0\t9", hist.toString());
    final Histogram hist2 = new Histogram();
    hist2.addHistogram(hist.toString());
    assertEquals(10, hist2.getLength());
    assertEquals("1\t0\t10\t1\t0\t0\t0\t0\t0\t9", hist2.toString());
    final double[] dist = hist.toDistribution();
    assertEquals(10, dist.length);
    assertEquals(0.0476, dist[0], 0.00005);
    assertEquals(0.0, dist[1], 0.00005);
    assertEquals(0.4762, dist[2], 0.00005);
    assertEquals(0.0476, dist[3], 0.00005);
    assertEquals(0.0, dist[4], 0.00005);
    assertEquals(0.0, dist[5], 0.00005);
    assertEquals(0.0, dist[6], 0.00005);
    assertEquals(0.0, dist[7], 0.00005);
    assertEquals(0.0, dist[8], 0.00005);
    assertEquals(0.4286, dist[9], 0.00005);
  }

  public void testEmptyParse() {
    final Histogram hist = new Histogram();
    hist.addHistogram("");
    assertEquals(0, hist.getLength());
    assertEquals("", hist.toString());
  }
}
