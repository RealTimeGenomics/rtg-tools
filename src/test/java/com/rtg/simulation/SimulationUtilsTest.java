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

import junit.framework.TestCase;

/**
 */
public class SimulationUtilsTest extends TestCase {


  public void testDistUtils() {

    final double[] cumDist = SimulationUtils.cumulativeDistribution(0.1, 0.1, 0.1, 0.4, 0.1);

    final double[] expected = {0.125, 0.25, 0.375, 0.875, 1.0};
    assertEquals(expected.length, cumDist.length);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], cumDist[i], 0.000001);
    }

    assertEquals(0, SimulationUtils.chooseFromCumulative(cumDist, 0.1));
    assertEquals(1, SimulationUtils.chooseFromCumulative(cumDist, 0.25));
    assertEquals(2, SimulationUtils.chooseFromCumulative(cumDist, 0.3));
    assertEquals(3, SimulationUtils.chooseFromCumulative(cumDist, 0.8));
    assertEquals(4, SimulationUtils.chooseFromCumulative(cumDist, 1.0));
  }

}
