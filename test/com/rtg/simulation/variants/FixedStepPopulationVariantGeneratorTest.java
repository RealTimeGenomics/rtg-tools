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

package com.rtg.simulation.variants;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 */
public class FixedStepPopulationVariantGeneratorTest extends TestCase {

  @Override
  protected void setUp() {
    Diagnostic.setLogStream();
  }

  private static final String REF = ">ref" + StringUtils.LS
          + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca";

  public void testFixedStepX() throws IOException {
    final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
    final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X"), new PortableRandom(10), 0.5);
    final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
    assertEquals(12, variants.size());
    int e = 0;
    int i = REF.indexOf("c");
    for (; i < REF.length(); i += 10) {
      final PopulationVariantGenerator.PopulationVariant var = variants.get(e);
      assertTrue(Arrays.equals(DnaUtils.encodeString(REF.substring(i, i + 1)), var.mRef));
      assertFalse(Arrays.equals(var.mAlleles[0], var.mRef));
      assertEquals(0, var.getSequenceId());
      assertEquals(e * 10, var.getStart());
      ++e;
    }
  }
  public void testFixedStepHetX() throws IOException {
    final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
    final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X_Y"), new PortableRandom(118), 0.5);
    final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
    assertEquals(12, variants.size());
    int e = 0;
    int i = REF.indexOf("c");
    for (; i < REF.length(); i += 10) {
      final PopulationVariantGenerator.PopulationVariant var = variants.get(e);
      assertTrue(Arrays.equals(DnaUtils.encodeString(REF.substring(i, i + 1)), var.mRef));
      assertTrue(!Arrays.equals(var.mAlleles[0], var.mAlleles[1]));
      assertEquals(0, var.getSequenceId());
      assertEquals(e * 10, var.getStart());
      ++e;
    }
  }
  public void testFixedStepI() throws IOException {
    final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
    final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("I"), new PortableRandom(10), 0.5);
    final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
    assertEquals(11, variants.size());
    int e = 0;
    int i = REF.indexOf("c") + 10;
    for (; i < REF.length(); i += 10) {
      final PopulationVariantGenerator.PopulationVariant var = variants.get(e);
      assertTrue(Arrays.equals(DnaUtils.encodeString(REF.substring(i - 1, i)), var.mRef));
      assertFalse(Arrays.equals(var.mAlleles[0], var.mRef));
      assertEquals(0, var.getSequenceId());
      assertEquals((e + 1) * 10 - 1, var.getStart());
      ++e;
    }
  }
}
