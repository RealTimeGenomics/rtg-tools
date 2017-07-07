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

package com.rtg.simulation.reads;

import com.rtg.simulation.IntSampler;
import com.rtg.util.ChiSquared;

import junit.framework.TestCase;

/**
 * Test class
 */
public class MinMaxGaussianSamplerTest extends TestCase {

  // 0.999 corresponds to expected failure of 1 in 1000 runs
  private static final double CONFIDENCE_LEVEL = 0.999;

  public void testBogusParameters() throws Exception {
    try {
      new MinMaxGaussianSampler(20, 10);
      fail(); // should not get here
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void checkNormalityOfLengths(final int min, final int max) throws Exception {
    final int testSize = 1000;
    final int[] counts0 = new int[max + 1];
    final IntSampler s = new MinMaxGaussianSampler(min, max);
    for (long k = 0; k < testSize; ++k) {
      final int length = s.next();
      assertTrue(length >= min);
      assertTrue(length <= max);
      counts0[length]++;
    }

    // Perform chi-squared test for uniformity
    final double mean = (max + min) * 0.5;
    final double variance = (max - min) / 4.0;
    double sum = 0;
    assertEquals(0.5, ChiSquared.normal(0), 1e-10);
    assertEquals(1, ChiSquared.normal(1e300), 1e-10);
    for (int k = min; k < counts0.length; ++k) {
      final int c = counts0[k];
      final double left = (k - 0.5 - mean) / variance;
      final double right = (k + 0.5 - mean) / variance;
      final double e = (ChiSquared.normal(right) - ChiSquared.normal(left)) * testSize;
      //System.out.println(k + " " + c + " " + e);
      final double g = c - e;
      sum += g * g / e;
    }
    assertTrue(sum < ChiSquared.chi(max - min, CONFIDENCE_LEVEL));
    assertTrue(sum > ChiSquared.chi(max - min, 1 - CONFIDENCE_LEVEL));
  }

  public void testNormalityOfLengths() throws Exception {
    checkNormalityOfLengths(5, 55);
    checkNormalityOfLengths(5, 6);
    checkNormalityOfLengths(0, 3);
  }

}
