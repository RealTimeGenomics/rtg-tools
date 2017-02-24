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

package com.rtg.simulation.genome;

import com.rtg.util.PortableRandom;

import junit.framework.TestCase;

/**
 *
 */
public class RandomDistributionTest extends TestCase {

  /**
   */
  public RandomDistributionTest(final String name) {
    super(name);
  }

  public void testFrequencies() {
    final PortableRandom rand = new PortableRandom(2);
    final int[] distribution = {1, 2, 6, 4, 0, 9};
    final RandomDistribution dist = new RandomDistribution(distribution, rand);
    final int[] results = new int[distribution.length];
    for (int i = 0; i < results.length; ++i) {
      results[i] = 0;
    }
    for (int i = 0; i < 1000000; ++i) {
      results[dist.nextValue()]++;
    }
    assertEquals(2, (double) results[1] / results[0], 0.1);
    assertEquals(6, (double) results[2] / results[0], 0.1);
    assertEquals(4, (double) results[3] / results[0], 0.1);
    assertEquals(0, results[4]);
    assertEquals(9, (double) results[5] / results[0], 0.1);

  }

  public void testZeroDistribution() {
    final PortableRandom rand = new PortableRandom(2);
    final int[] distribution = {0, 0, 0, 1};
    final RandomDistribution dist = new RandomDistribution(distribution, rand);
    final int[] results = new int[distribution.length];
    for (int i = 0; i < results.length; ++i) {
      results[i] = 0;
    }
    for (int i = 0; i < 1000000; ++i) {
      results[dist.nextValue()]++;
    }
    assertEquals(0, results[0]);
    assertEquals(0, results[1]);
    assertEquals(0, results[2]);
    assertEquals(1000000, results[3]);

  }

  public void testValueCount() {
    final PortableRandom rand = new PortableRandom(1);
    final int[] distribution = {1, 2, 6, 4, 9};
    final RandomDistribution dist = new RandomDistribution(distribution, rand);
    assertEquals(5, dist.valueCount());
  }

}
