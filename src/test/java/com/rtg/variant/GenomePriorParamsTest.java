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

package com.rtg.variant;

import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;

import junit.framework.TestCase;

/**
 */
public class GenomePriorParamsTest extends TestCase {

  private GenomePriorParams mParams;

  @Override
  public void setUp() throws Exception {
    mParams = GenomePriorParams.builder()
    .genomePriors("testhumanpriorparams")
    .create();
  }

  @Override
  public void tearDown() {
    mParams = null;
  }

  public void testGlobalIntegrity() {
    assertTrue(mParams.globalIntegrity());
  }

  /** Check that Illumina error priors parse okay. */
  public void testIllumina() throws InvalidParamsException, IOException {
    mParams = GenomePriorParams.builder()
    .genomePriors("testhumanpriorparams")
    .create();
  }

  public void testDefaultPriors() {
    final GenomePriorParams priors = GenomePriorParams.builder().create();
    assertEquals(0.00053, priors.genomeSnpRate(false));
    assertEquals(0.00071, priors.genomeSnpRate(true));
    final double homo = priors.genomeIndelEventFraction();
    final double hetero = 1.0 - homo;
    assertEquals(0.68, homo);
    assertEquals(0.00015 * homo,   priors.genomeIndelEventRate(false));
    assertEquals(0.00015 * hetero, priors.genomeIndelEventRate(true));
    final double[] indelDistrib = priors.genomeIndelDistribution();
    assertEquals(11, indelDistrib.length);
    assertEquals(0.566, indelDistrib[0]);
    assertEquals(0.226, indelDistrib[1]);
    assertEquals(0.083, indelDistrib[2]);
    final double[] mnpDistrib = priors.genomeMnpDistribution();
    assertEquals(13, mnpDistrib.length);
    assertEquals(0.0, mnpDistrib[0]);
    assertEquals(0.0, mnpDistrib[1]);
    assertEquals(0.40, mnpDistrib[2]);
    assertEquals(0.20, mnpDistrib[3]);
    assertEquals(2.3E-8, priors.denovoRef());
//    assertEquals(0.0, priors.denovoRef());
    assertEquals(Math.log(2.3E-8), priors.logDenovoRef());
//    assertEquals(Double.NEGATIVE_INFINITY, priors.logDenovoRef());
    assertEquals(2.3E-11, priors.denovoNonRef());
//    assertEquals(0.0, priors.denovoNonRef());
    assertEquals(Math.log(2.3E-11), priors.logDenovoNonRef());
//    assertEquals(Double.NEGATIVE_INFINITY, priors.logDenovoNonRef());
  }

  /** Check that the testhumanpriorparams.properties values are read correctly. */
  public void testDefaults() {
    final double homo = 0.66;
    final double hetero = 1 - homo;
    assertEquals(0.0003, mParams.genomeSnpRate(true));
    assertEquals(0.00031, mParams.genomeSnpRate(false));
    assertEquals(0.00008, mParams.genomeMnpBaseRate(true));
    assertEquals(0.00002, mParams.genomeMnpBaseRate(false));
    assertEquals(0.00008 / GenomePriorParams.AVERAGE_HETERO_MNP_LENGTH, mParams.genomeMnpEventRate(true));
    assertEquals(0.00002 / GenomePriorParams.AVERAGE_HOMO_MNP_LENGTH, mParams.genomeMnpEventRate(false));
    assertEquals(homo, mParams.genomeIndelEventFraction());
    assertEquals(0.000042 * hetero, mParams.genomeIndelEventRate(true));
    assertEquals(0.000042 * homo,   mParams.genomeIndelEventRate(false));
    final double[] indelDist = mParams.genomeIndelDistribution();
    assertEquals(11, indelDist.length);
    assertEquals(0.566, indelDist[0]);
    assertEquals(0.226, indelDist[1]);
    assertEquals(0.083, indelDist[2]);
    assertEquals(0.007, indelDist[10]);
    final double[] mnpDist = mParams.genomeMnpDistribution();
    assertEquals(8, mnpDist.length);
    assertEquals(0.00, mnpDist[0]);
    assertEquals(0.00, mnpDist[1]);
    assertEquals(0.40, mnpDist[2]);
    assertEquals(0.20, mnpDist[3]);
    assertEquals(0.10, mnpDist[4]);
    assertEquals(0.10, mnpDist[5]);
    assertEquals(0.10, mnpDist[6]);
    assertEquals(0.10, mnpDist[7]);
  }

  public void testSums() {
    double totalA = 0.0;
    double totalC = 0.0;
    double totalG = 0.0;
    double totalT = 0.0;
    final String[] x = {"A", "C", "G", "T", "A:C", "A:T", "A:G", "C:G", "C:T", "G:T"};
    for (String aX : x) {
      final double[] prior = mParams.getPriorDistr(aX);
      totalA += prior[0];
      totalC += prior[1];
      totalG += prior[2];
      totalT += prior[3];
    }

    assertTrue("" + Math.abs(totalA - 1.0), Math.abs(totalA - 1.0) < 0.0000001);
    assertTrue("" + Math.abs(totalC - 1.0), Math.abs(totalC - 1.0) < 0.0000001);
    assertTrue("" + Math.abs(totalG - 1.0), Math.abs(totalG - 1.0) < 0.0000001);
    assertTrue("" + Math.abs(totalT - 1.0), Math.abs(totalT - 1.0) < 0.0000001);
  }

  public void testAltPriorsA() {
    final double[] prior = mParams.getPriorDistr("A");
    assertEquals("[0.99897133, 0.00009541, 0.00037624, 0.00005571]", Utils.realFormat(prior, 8));
  }


  public void testAltPriorsC() {
    final double[] prior = mParams.getPriorDistr("C");
    assertEquals("[0.00007084, 0.99841451, 0.00010356, 0.00029994]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsG() {
    final double[] prior = mParams.getPriorDistr("G");
    assertEquals("[0.00030100, 0.00010390, 0.99841501, 0.00007121]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsT() {
    final double[] prior = mParams.getPriorDistr("T");
    assertEquals("[0.00005627, 0.00037643, 0.00009555, 0.99897329]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsAC() {
    final double[] prior = mParams.getPriorDistr("A:C");
    assertEquals("[0.00010258, 0.00016267, 0.00000034, 0.00000024]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsAT() {
    final double[] prior = mParams.getPriorDistr("A:T");
    assertEquals("[0.00008642, 0.00000034, 0.00000034, 0.00008612]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsAG() {
    final double[] prior = mParams.getPriorDistr("A:G");
    assertEquals("[0.00041085, 0.00000034, 0.00068298, 0.00000024]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsCG() {
    final double[] prior = mParams.getPriorDistr("C:G");
    assertEquals("[0.00000024, 0.00016264, 0.00016329, 0.00000024]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsCT() {
    final double[] prior = mParams.getPriorDistr("C:T");
    assertEquals("[0.00000024, 0.00068341, 0.00000034, 0.00041098]", Utils.realFormat(prior, 8));
  }

  public void testAltPriorsGT() {
    final double[] prior = mParams.getPriorDistr("G:T");
    assertEquals("[0.00000024, 0.00000034, 0.00016235, 0.00010206]", Utils.realFormat(prior, 8));
  }

  public void testBuilder() {
    final GenomePriorParamsBuilder builder = GenomePriorParams.builder();
    assertNotNull(builder);
    assertTrue(builder == builder.genomeMnpBaseRateHetero(0.03));
    assertTrue(builder == builder.genomeSnpRateHetero(0.04));
    assertTrue(builder == builder.genomeMnpBaseRateHomo(0.05));
    assertTrue(builder == builder.genomeSnpRateHomo(0.06));
    assertTrue(builder == builder.genomeIndelDistribution(new double[] {0.07, 0.08, 0.85}));
    final double homo = 0.11;
    final double hetero = 1.0 - homo;
    assertTrue(builder == builder.genomeIndelEventFraction(homo));
    assertTrue(builder == builder.genomeIndelEventRate(0.12));
    assertTrue(builder == builder.genomeMnpDistribution(new double[] {0.13, 0.14, 0.73}));
    assertTrue(builder == builder.denovoRef(0.0005));
    assertTrue(builder == builder.denovoNonRef(0.0000005));
    mParams = builder.create();

    assertEquals(0.12 * hetero, mParams.genomeIndelEventRate(true));
    assertEquals(0.12 * homo,   mParams.genomeIndelEventRate(false));
    assertEquals(0.04, mParams.genomeSnpRate(true));
    assertEquals(0.06, mParams.genomeSnpRate(false));
    assertEquals(0.03, mParams.genomeMnpBaseRate(true));
    assertEquals(0.05, mParams.genomeMnpBaseRate(false));
    assertEquals(homo, mParams.genomeIndelEventFraction());
    assertEquals(0.12 * hetero, mParams.genomeIndelEventRate(true));
    assertEquals(0.12 * homo,   mParams.genomeIndelEventRate(false));
    // all the insert/delete distributions are the same at the moment.
    final double[] indelDist = mParams.genomeIndelDistribution();
    assertEquals(3, indelDist.length);
    assertEquals(0.07, indelDist[0]);
    assertEquals(0.08, indelDist[1]);
    assertEquals(0.85, indelDist[2]);
    final double[] mnpDist = mParams.genomeMnpDistribution();
    assertEquals(5, mnpDist.length);
    assertEquals(0.00, mnpDist[0]);
    assertEquals(0.00, mnpDist[1]);
    assertEquals(0.13, mnpDist[2]);
    assertEquals(0.14, mnpDist[3]);
    assertEquals(0.73, mnpDist[4]);
    assertEquals(0.0005, mParams.denovoRef());
    assertEquals(0.0000005, mParams.denovoNonRef());
  }

  public void testSetIndelPrior() {
    final GenomePriorParamsBuilder builder = GenomePriorParams.builder();
    mParams = builder.genomeIndelEventRate(0.45).create();
    assertEquals(0.45, mParams.genomeIndelEventRate(false) + mParams.genomeIndelEventRate(true), 1E-8);
  }

  public void testDistribSum() {
    try {
      GenomePriorParams.builder().genomeIndelDistribution(new double[] {0.6, 0.5}).create();
      fail("expected bad probability exception");
    } catch (final IllegalArgumentException e) {
      assertEquals("distribution must sum to 1.0, not 1.1", e.getMessage());
    }
  }

  public void testDistribSum1() {
    GenomePriorParams.builder().genomeIndelDistribution(new double[] {0.0, 1.0, 0.0}).create();
  }

  public void testDistribValue() {
    try {
      GenomePriorParams.builder().genomeIndelDistribution(new double[] {1.01, 0.5}).create();
      fail("expected bad probability exception");
    } catch (final IllegalArgumentException e) {
      assertEquals("rate must be 0.0 .. 1.0, not 1.010000", e.getMessage());
    }
  }

  public void testDistribNeg() {
    try {
      GenomePriorParams.builder().genomeIndelDistribution(new double[] {-0.01, 0.5}).create();
      fail("expected bad probability exception");
    } catch (final IllegalArgumentException e) {
      assertEquals("rate must be 0.0 .. 1.0, not -0.010000", e.getMessage());
    }
  }

  public void testToString() {
    assertEquals("    heterozygous prior=0.0003000 homozygous prior=0.0003100"
        + StringUtils.LS,
        mParams.toString());
  }

  public void testAlleleFreq() throws IOException, InvalidParamsException {
    mParams = GenomePriorParams.builder()
      .genomePriors("human")
      .create();
    assertEquals(-0.001307, mParams.getAlleleFrequencyLnProbability(1), 1E-5);
    assertEquals(-6.641090, mParams.getAlleleFrequencyLnProbability(2), 1E-5);
    assertEquals(-13.954772, mParams.getAlleleFrequencyLnProbability(3), 1E-5);
    assertEquals(-20.862527, mParams.getAlleleFrequencyLnProbability(4), 1E-5);
  }
}
