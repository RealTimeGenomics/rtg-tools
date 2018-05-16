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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.simulation.variants.PopulationVariantGenerator.PopulationVariant;
import com.rtg.util.Counter;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.SequenceIdLocusComparator;
import com.rtg.util.intervals.SequenceIdLocusSimple;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 */
public class PopulationVariantGeneratorTest extends AbstractNanoTest {

  private static final String REF = ">ref" + StringUtils.LS
          + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca";

  public void testFixedStepXVcfWriting() throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("PGVT")) {
      final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
      final int seed = 10;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File outFile = new File(tempDir, "out.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(outFile, variants, sr, seed);
      final String act = TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(outFile));
      mNano.check("population_variant_gen_X.vcf", act, false);
    }
  }

  public void testFixedStepHetXVcfWriting() throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("PGVT")) {
      final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
      final int seed = 118;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X_X"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File outFile = new File(tempDir, "out.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(outFile, variants, sr, seed);
      final String act = TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(outFile));
      mNano.check("population_variant_gen_X_X.vcf", act, false);
    }
  }

  public void testFixedStepIVcfWriting() throws IOException {
    try (final TestDirectory tempDir = new TestDirectory("PGVT")) {
      final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);
      final int seed = 10;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("I"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File outFile = new File(tempDir, "out.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(outFile, variants, sr, seed);
      final String outStr = FileHelper.gzFileToString(outFile);
      final String act = TestUtils.sanitizeVcfHeader(outStr);
      mNano.check("population_variant_gen_I.vcf", act, false);
    }
  }

  public void testGeneratePopulationRetry() throws IOException {
    final Counter count = new Counter();
    final PopulationVariantGenerator dummyGenerator = new PopulationVariantGenerator(null) {
      @Override
      PopulationVariant nextPopulationVariant() {
        count.increment();
        return new PopulationVariant(new SequenceIdLocusSimple(0, 10, 20));
      }
      @Override
      protected boolean checkValid(PopulationVariant var, NavigableSet<PopulationVariant> set) {
        return false;
      }
    };
    try {
      dummyGenerator.generatePopulation();
      fail();
    } catch (RuntimeException e) {
      // expected
    }
    assertEquals(100, count.count());
  }

  public void testGeneratePopulationOrdering() throws IOException {
    final Random rand = new Random(123);
    final Counter count = new Counter();
    final PopulationVariantGenerator dummyGenerator = new PopulationVariantGenerator(null) {
      @Override
      PopulationVariant nextPopulationVariant() {
        count.increment();
        return new PopulationVariant(new SequenceIdLocusSimple(rand.nextInt(20), rand.nextInt(9000)));
      }
      @Override
      protected boolean checkValid(PopulationVariant var, NavigableSet<PopulationVariant> set) {
        return true;
      }
      @Override
      protected boolean needMoreVariants() {
        return count.count() < 100;
      }
    };
    final List<PopulationVariant> list = dummyGenerator.generatePopulation();
    assertEquals(100, list.size());
    for (int i = 0; i < 99; ++i) {
      assertTrue(list.get(i).getSequenceId() <= list.get(i + 1).getSequenceId());
      if (list.get(i).getSequenceId() == list.get(i + 1).getSequenceId()) {
        assertTrue(list.get(i).getStart() <= list.get(i + 1).getStart());
      }
    }
  }

  public void testCollapsePopulationVariant() {
    final PopulationVariant variant = new PopulationVariant(new SequenceIdLocusSimple(0, 1));
    variant.mRef = DnaUtils.encodeString("ACTGAATTCGAGG");
    variant.mAlleles = new byte[8][];
    variant.mAlleles[0] = DnaUtils.encodeString("AT");
    variant.mAlleles[1] = Arrays.copyOf(variant.mRef, variant.mRef.length);
    variant.mAlleles[2] = DnaUtils.encodeString("ATTTTTTT");
    variant.mAlleles[3] = Arrays.copyOf(variant.mRef, variant.mRef.length);
    variant.mAlleles[4] = DnaUtils.encodeString("AT");
    variant.mAlleles[5] = DnaUtils.encodeString("ACCTGAATTCGAGG");
    variant.mAlleles[6] = DnaUtils.encodeString("A");
    variant.mAlleles[7] = DnaUtils.encodeString("A");
    variant.mDistribution = new double[] {0.11, 0.12, 0.13, 0.14, 0.15, 0.16, 0.17, 0.18};
    FixedStepPopulationVariantGenerator.collapsePopulationVariant(variant);
    assertEquals(4, variant.mAlleles.length);
    assertTrue(Arrays.equals(DnaUtils.encodeString("A"), variant.mAlleles[0]));
    assertTrue(Arrays.equals(DnaUtils.encodeString("AT"), variant.mAlleles[1]));
    assertTrue(Arrays.equals(DnaUtils.encodeString("ATTTTTTT"), variant.mAlleles[2]));
    assertTrue(Arrays.equals(DnaUtils.encodeString("ACCTGAATTCGAGG"), variant.mAlleles[3]));
    assertTrue(Arrays.equals(new double[] {0.35, 0.26, 0.13, 0.16}, variant.mDistribution));
  }

  private PopulationVariant makeVariant(int seq, int start, String ref, String...alleles) {
    final PopulationVariant variantA = new PopulationVariant(new SequenceIdLocusSimple(seq, start));
    variantA.mRef = DnaUtils.encodeString(ref);
    variantA.mAlleles = new byte[alleles.length][];
    for (int i = 0; i < alleles.length; ++i) {
      variantA.mAlleles[i] = DnaUtils.encodeString(alleles[i]);
    }
    return variantA;
  }

  public void testCheckValid() {
    final PopulationVariantGenerator dummyGenerator = new PopulationVariantGenerator(null) {
      @Override
      PopulationVariant nextPopulationVariant() {
        return null;
      }
    };
    final NavigableSet<PopulationVariant> vSet = new TreeSet<>(new SequenceIdLocusComparator());

    final PopulationVariant variantA = makeVariant(1, 0, "", "A", "T");
    assertFalse(dummyGenerator.checkValid(variantA, vSet));
    variantA.mRef = DnaUtils.encodeString("NN");
    assertFalse(dummyGenerator.checkValid(variantA, vSet));
    variantA.mRef = DnaUtils.encodeString("C");
    assertTrue(dummyGenerator.checkValid(variantA, vSet));

    final PopulationVariant variantB = makeVariant(1, 132, "CTTTTTT", "A", "T");
    vSet.add(variantB);
    assertTrue(dummyGenerator.checkValid(makeVariant(1, 100, "C", "A"), vSet));

    final PopulationVariant variantC = makeVariant(1, 103, "CTTTTTT", "A", "T");
    vSet.add(variantC);
    assertFalse(dummyGenerator.checkValid(makeVariant(1, 100, "CCCCC", "A"), vSet));

    assertTrue(dummyGenerator.checkValid(makeVariant(0, 100, "CCCCC", "A"), vSet));
    assertTrue(dummyGenerator.checkValid(makeVariant(2, 100, "CCCCC", "A"), vSet));

    assertTrue(dummyGenerator.checkValid(makeVariant(1, 131, "C", "A"), vSet));
    for (int i = 132; i < 139; ++i) {
      assertFalse(dummyGenerator.checkValid(makeVariant(1, i, "C", "A"), vSet));
    }
    assertTrue(dummyGenerator.checkValid(makeVariant(1, 139, "C", "A"), vSet));
  }
}
