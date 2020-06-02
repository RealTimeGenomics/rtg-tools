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

import java.io.IOException;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;

import junit.framework.TestCase;

/**
 */
public class DistributionSamplerTest extends TestCase {
  static final double E = 0.1E-9;
  static final double[] PROB = {0.1, 0.3, 0.1, 0.2, 0.2, 0.1};
  public void testDistribution() {
    final DistributionSampler dist = new DistributionSampler(PROB);
    check(dist);
  }
  private void check(DistributionSampler dist) {
    assertEquals(0, dist.next(0.1 - E));
    assertEquals(1, dist.next(0.4 - E));
    assertEquals(2, dist.next(0.5 - E));
    assertEquals(3, dist.next(0.7 - E));
    assertEquals(4, dist.next(0.9 - E));
    assertEquals(5, dist.next(1.0 - E));

    assertEquals(1, dist.next(0.1 + E));
    assertEquals(2, dist.next(0.4 + E));
    assertEquals(3, dist.next(0.5 + E));
    assertEquals(4, dist.next(0.7 + E));
    assertEquals(5, dist.next(0.9 + E));
  }

  public void testDefaults() throws IOException {
    final SequencesReader readerDnaMemory = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("A", "AAA", "A", "AA", "AA", "A"));
    check(SimulationUtils.createDistribution(readerDnaMemory, null));
  }

  public void testOverrideDefaults() throws IOException {
    final SequencesReader readerDnaMemory = ReaderTestUtils.getReaderDnaMemory(ReaderTestUtils.fasta("AA", "AA", "AA", "AA", "AA", "AA"));
    check(SimulationUtils.createDistribution(readerDnaMemory, PROB));
  }
}
