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

import com.rtg.reader.CompressedMemorySequencesReader;
import com.rtg.reader.SdfId;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorParamsBuilder;

/**
 * Test Class
 */
public class CompleteGenomicsV1MachineTest extends AbstractMachineTest {

  @Override
  protected Machine getMachine(final long seed) throws IOException, InvalidParamsException {
    return new CompleteGenomicsV1Machine(getPriors(), seed);
  }

  @Override
  protected AbstractMachineErrorParams getPriors() throws IOException, InvalidParamsException {
    return new MachineErrorParamsBuilder().errors("complete").create();
  }

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
      mNano.check("cg-results.fa", out.toString(), false);
    }
  }

  void checkQualities(byte[] quals) {
    for (final byte b : quals) {
      assertTrue("byte outside phred limits: " + b, b >= 0 && b <= CompressedMemorySequencesReader.MAX_QUAL_VALUE);
    }
  }

  class StatsReadWriter implements ReadWriter {
    private final int[] mBackstep = new int[10];
    private final int[] mSmallSkip = new int[10];
    private final int[] mBigSkip = new int[20];
    int mTotal = 0;

    private int whatIsTheSkip(final String name, final int pos) {
      int v = 0;
      int m = 1;
      char c;
      int p = pos;
      while (Character.isDigit(c = name.charAt(--p))) {
        v += (c - '0') * m;
        m *= 10;
      }
      return v;
    }

    void augment(final String name) {
      final int b = name.indexOf('B');
      if (b == -1) {
        mBackstep[0]++;
      } else {
        mBackstep[name.charAt(b - 1) - '0']++;
      }
      final int n1 = name.indexOf('N');
      if (n1 != -1) {
        final int n2 = name.indexOf('N', n1 + 1);
        final int s1 = whatIsTheSkip(name, n1);
        final int s2 = n2 == -1 ? 0 : whatIsTheSkip(name, n2);
        final int smallSkip = Math.min(s1, s2);
        final int bigSkip = Math.max(s1, s2);
        mBigSkip[bigSkip]++;
        mSmallSkip[smallSkip]++;
      }
      ++mTotal;
    }

    @Override
    public void writeRead(final String name, final byte[] data, final byte[] qual, final int length) {
      augment(name);
    }

    @Override
    public void writeLeftRead(final String name, final byte[] data, final byte[] qual, final int length) {
      augment(name);
    }

    @Override
    public void writeRightRead(final String name, final byte[] data, final byte[] qual, final int length) {
      augment(name);
    }

    @Override
    public void identifyTemplateSet(SdfId... templateIds) { }

    @Override
    public void identifyOriginalReference(SdfId referenceId) { }

    void performStatisticalTests() throws IOException, InvalidParamsException {
      assertEquals(0, mBigSkip[0]);
      assertEquals(0, mBigSkip[1]);
      assertEquals(0, mBigSkip[2]);
      assertEquals(0, mBigSkip[3]);
      assertEquals(0, mBigSkip[9]);
      assertEquals(0, mSmallSkip[4]);
      assertEquals(0, mBackstep[5]);
      final AbstractMachineErrorParams errors = getPriors();
      checkDiscreteDistribution("Overlap", errors.overlapDistribution(), new int[] {mBackstep[4], mBackstep[3], mBackstep[2], mBackstep[1], mBackstep[0]}, 1);
      checkDiscreteDistribution("Gap", errors.gapDistribution(), new int[] {mBigSkip[4], mBigSkip[5], mBigSkip[6], mBigSkip[7], mBigSkip[8]}, 1);
      checkDiscreteDistribution("SmallGap", errors.smallGapDistribution(), mSmallSkip, 1);
    }

    @Override
    public void close() {
    }

    @Override
    public int readsWritten() {
      return mTotal;
    }
  }

  public void testOverlapDistributions() throws Exception {
    try (StatsReadWriter w = new StatsReadWriter()) {
      final Machine m = getMachine(42);
      m.setReadWriter(w);
      final byte[] frag = new byte[FRAGMENT_LENGTH];
      Arrays.fill(frag, (byte) 1);
      for (int k = 0; k < 10000; ++k) {
        m.processFragment("b/", 0, frag, frag.length);
      }
      w.performStatisticalTests();
    }
  }

  /**
   * Slop permitted in the statistical test.  Should be 1 if the machine is
   * well modelled, but may need to be higher if the modelling is less
   * accurate.
   *
   * Complete Genomics modelling is particularly difficult due to the small
   * fragements which make up an entire read.  One source of approximation is that
   * longer errors are truncated at each boundary, this introduces a bias towards
   * short errors.  It might be possible to internally adjust the probability
   * distribution to bias the generation of longer errors towards regions where
   * it is possible to make such errors.  If this was done, I would hope that
   * a smaller slop could be used.
   *
   * @return an <code>int</code> value
   */
  @Override
  protected double getSlop() {
    return 5;
  }
}
