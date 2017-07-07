/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.simulation;

import com.rtg.util.ChiSquared;
import com.rtg.util.HistogramWithNegatives;

import junit.framework.TestCase;

/**
 * Test class
 */
public class GaussianSamplerTest extends TestCase {

  private static final double CONFIDENCE_LEVEL = 0.999;

  public void checkNormalityOfLengths(final int min, final int max) throws Exception {
    final int testSize = 1000;
    final double mean = (max + min) * 0.5;
    final double variance = (max - min) / 4.0;
    final IntSampler s = new GaussianSampler(mean, variance);
    final HistogramWithNegatives h = new HistogramWithNegatives();
    for (int k = 0; k < testSize; ++k) {
      final int length = s.next();
      h.increment(length);
    }

    // Perform chi-squared test for uniformity
    double sum = 0;
    assertEquals(0.5, ChiSquared.normal(0), 1e-10);
    assertEquals(1, ChiSquared.normal(1e300), 1e-10);
    for (int k = h.min(); k < h.max(); ++k) {
      final long c = h.getValue(k);
      final double left = (k - 0.5 - mean) / variance;
      final double right = (k + 0.5 - mean) / variance;
      final double e = (ChiSquared.normal(right) - ChiSquared.normal(left)) * testSize;
      //System.out.println(k + " " + c + " " + e);
      final double g = c - e;
      sum += g * g / e;
    }
    assertTrue(sum < ChiSquared.chi(h.max() - h.min(), CONFIDENCE_LEVEL));
    assertTrue(sum > ChiSquared.chi(h.max() - h.min(), 1 - CONFIDENCE_LEVEL));
  }

  public void testNormalityOfLengths() throws Exception {
    checkNormalityOfLengths(50, 85);
    checkNormalityOfLengths(5, 60);
    checkNormalityOfLengths(0, 30);
  }

}
