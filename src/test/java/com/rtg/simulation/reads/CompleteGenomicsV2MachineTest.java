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

import java.io.IOException;
import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.RandomDna;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Test Class
 */
public class CompleteGenomicsV2MachineTest extends CompleteGenomicsV1MachineTest {

  @Override
  protected Machine getMachine(final long seed) throws IOException, InvalidParamsException {
    return new CompleteGenomicsV2Machine(getPriors(), seed);
  }

  @Override
  public void test() throws IOException, InvalidParamsException {
    final CompleteGenomicsMachine m = (CompleteGenomicsMachine) getMachine(42);
    try (final MemoryPrintStream out = new MemoryPrintStream()) {
      final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
      m.setReadWriter(w);
      final byte[] frag = new byte[500];
      Arrays.fill(frag, (byte) 1);
      m.processFragment("name/", 30, frag, frag.length);
      checkQualities(m.mQualityBytes);
      m.processFragment("name/", 30, frag, frag.length);
      checkQualities(m.mQualityBytes);
      m.processFragment("name/", 30, frag, frag.length);
      checkQualities(m.mQualityBytes);
      m.processFragment("name/", 30, frag, frag.length);
      checkQualities(m.mQualityBytes);
      mNano.check("cg-v2-results.fa", out.toString(), false);
    }
  }

  public void test2() throws IOException, InvalidParamsException {
    final CompleteGenomicsMachine m = (CompleteGenomicsMachine) getMachine(10);
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
    m.setReadWriter(w);
    final String template = RandomDna.random(500, new PortableRandom(33));
    final byte[] frag = DnaUtils.encodeString(template);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    m.processFragment("name/", 30, frag, frag.length);
    checkQualities(m.mQualityBytes);
    mNano.check("cg-v2-results2.fa", out.toString(), false);
  }

  class StatsReadWriter extends CompleteGenomicsV1MachineTest.StatsReadWriter {
    private final int[] mBackstep = new int[10];

    @Override
    void augment(final String name) {
      final int b = name.indexOf('B');
      if (b == -1) {
        mBackstep[0]++;
      } else {
        mBackstep[name.charAt(b - 1) - '0']++;
      }
      final int n1 = name.indexOf('N');
      assertEquals(-1, n1);
      ++mTotal;
    }

    @Override
    void performStatisticalTests() throws IOException, InvalidParamsException {
      assertEquals(0, mBackstep[8]);
      final AbstractMachineErrorParams errors = getPriors();
      checkDiscreteDistribution("Overlap", errors.overlapDistribution2(), new int[] {mBackstep[7], mBackstep[6], mBackstep[5], mBackstep[4], mBackstep[3], mBackstep[2], mBackstep[1], mBackstep[0]}, 1);
    }
  }

  @Override
  public void testOverlapDistributions() throws Exception {
    try (StatsReadWriter w = new StatsReadWriter()) {
      final Machine m = getMachine(System.currentTimeMillis());
      m.setReadWriter(w);
      final byte[] frag = new byte[FRAGMENT_LENGTH];
      Arrays.fill(frag, (byte) 1);
      for (int k = 0; k < 10000; ++k) {
        m.processFragment("b/", 0, frag, frag.length);
      }
      w.performStatisticalTests();
    }
  }

}
