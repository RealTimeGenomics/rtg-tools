/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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

package com.rtg.vcf.eval;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import junit.framework.TestCase;

/**
 */
public class OrientorTest extends TestCase {

  static final String SNP_LINE2 = "chr 23 . A T,C,G . PASS . GT 1/2";
  static final String SNP_LINE3 = "chr 23 . A T     . PASS . GT 0/1";
  static final String SNP_LINE4 = "chr 23 . A T     . PASS . GT ./1";

  public void testSquashGt() throws Exception {
    final VariantFactory.SampleVariants fact = new VariantFactory.SampleVariants(0, true);

    // Test squashing ploidy  1/2 -> 1, 2
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE2), 0);
    OrientedVariant[] pos = Orientor.SQUASH_GT.orientations(variant);
    assertEquals(2, pos.length);
    assertFalse(pos[0].isOriginal());
    assertFalse(pos[1].isOriginal());
    assertFalse(pos[0].isHeterozygous());
    assertFalse(pos[1].isHeterozygous());
    assertEquals(1, pos[0].alleleId());
    assertEquals(2, pos[1].alleleId());

    // Test squashing ploidy  0/1 -> 1
    variant = fact.variant(VariantTest.createRecord(SNP_LINE3), 0);
    pos = Orientor.SQUASH_GT.orientations(variant);
    assertEquals(1, pos.length);
    assertFalse(pos[0].isOriginal());
    assertFalse(pos[0].isHeterozygous());
    assertEquals(1, pos[0].alleleId());

    // Test squashing ploidy  ./1 -> 1
    variant = fact.variant(VariantTest.createRecord(SNP_LINE4), 0);
    pos = Orientor.SQUASH_GT.orientations(variant);
    assertEquals(1, pos.length);
    assertFalse(pos[0].isOriginal());
    assertFalse(pos[0].isHeterozygous());
    assertEquals(1, pos[0].alleleId());
  }

  public void testAlleleGt() throws Exception {
    final VariantFactory fact = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.SampleVariants(0, true));

    // Test 1/2 -> 0/1, 0/2, 1/0, 2/0, 1/2, 2/1
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE2), 0);
    OrientedVariant[] pos = Orientor.ALLELE_GT.orientations(variant);
    String[] exp = {
      "chr:23-24 (*^:Tv:C:*)",
      "chr:23-24 (*^:T:Cv:*)",
      "chr:23-24 (*v:T^:C:*)",
      "chr:23-24 (*v:T:C^:*)",
      "chr:23-24 (*:T^:Cv:*)",
      "chr:23-24 (*:Tv:C^:*)",
    };
    assertEquals(exp.length, pos.length);
    for (int i = 0; i < pos.length; i++) {
      assertEquals(exp[i], pos[i].toString());
    }

    // Test 0/1 -> 0/1, 1/0, 1/1
    variant = fact.variant(VariantTest.createRecord(SNP_LINE3), 0);
    pos = Orientor.ALLELE_GT.orientations(variant);
    exp = new String[] {
      "chr:23-24 (*^:Tv)",
      "chr:23-24 (*v:T^)",
      "chr:23-24 (*:Tx)",
    };
    assertEquals(exp.length, pos.length);
    for (int i = 0; i < pos.length; i++) {
      assertEquals(exp[i], pos[i].toString());
    }
  }

  static final String SNP_LINE5 = "chr 23 . AA T,ATTA . PASS . GT 0/2";
  static final String SNP_LINE6 = "chr 23 . T A,TAAA . PASS . GT 2|1";
  static final String SNP_LINE7 = "chr 23 . T A,TAAA . PASS . GT .|2";

  public void testUnphasedGt() throws Exception {
    // Test trimming ploidy  AA:ATTA -> :TT
    final VariantFactory fact = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.SampleVariants(0, false));
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE5), 0);
    final Orientor unphased = new Orientor.UnphasedOrientor(2);
    OrientedVariant[] pos = unphased.orientations(variant);
    assertEquals(2, pos.length);

    variant = fact.variant(VariantTest.createRecord(SNP_LINE6), 0);
    pos = unphased.orientations(variant);
    assertEquals(2, pos.length);
    assertEquals("chr:23-24 (*:Av:<24-24>AAA^)", pos[0].toString());

    variant = fact.variant(VariantTest.createRecord(SNP_LINE7), 0);
    pos = unphased.orientations(variant);
    assertEquals(2, pos.length);
    assertEquals("chr:24-24 (.^:*:*:AAAv)", pos[0].toString());
  }

  public void testPhasedGt() throws Exception {
    // Test trimming ploidy  AA:ATTA -> :TT
    final VariantFactory fact = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.SampleVariants(0, false));
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE5), 0);
    final Orientor phased = new Orientor.PhasedOrientor(false, 2);
    final Orientor inverted = new Orientor.PhasedOrientor(true, 2);
    OrientedVariant[] pos = phased.orientations(variant);
    assertEquals(2, pos.length);

    variant = fact.variant(VariantTest.createRecord(SNP_LINE6), 0);
    pos = phased.orientations(variant);
    assertEquals(1, pos.length);
    assertTrue(pos[0].isOriginal());
    assertTrue(pos[0].isHeterozygous());
    assertEquals(2, pos[0].alleleId());
    assertEquals("chr:23-24 (*:Av:<24-24>AAA^)", pos[0].toString());

    pos = inverted.orientations(variant);
    assertEquals(1, pos.length);
    assertFalse(pos[0].isOriginal());
    assertTrue(pos[0].isHeterozygous());
    assertEquals(1, pos[0].alleleId());
    assertEquals("chr:23-24 (*:A^:<24-24>AAAv)", pos[0].toString());

    variant = fact.variant(VariantTest.createRecord(SNP_LINE7), 0);
    pos = phased.orientations(variant);
    assertEquals(1, pos.length);
    assertTrue(pos[0].isOriginal());
    assertTrue(pos[0].isHeterozygous());
    assertEquals(-1, pos[0].alleleId());
    assertEquals(2, pos[0].alleleIds()[1]);
    assertEquals("chr:24-24 (.^:*:*:AAAv)", pos[0].toString());

    pos = inverted.orientations(variant);
    assertEquals(1, pos.length);
    assertFalse(pos[0].isOriginal());
    assertTrue(pos[0].isHeterozygous());
    assertEquals(2, pos[0].alleleId());
    assertEquals(-1, pos[0].alleleIds()[1]);
    assertEquals("chr:24-24 (.v:*:*:AAA^)", pos[0].toString());

  }

  public void testSquashAlts() throws Exception {
    final Variant variant = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.AllAlts(false)).variant(VariantTest.createRecord(SNP_LINE2), 0);
    // Test squashing ploidy  A T,C,G -> T, C, G
    final OrientedVariant[] pos = Orientor.HAPLOID_POP.orientations(variant);
    assertEquals(3, pos.length);
    for (OrientedVariant ov : pos) {
      assertFalse(ov.isOriginal());
      assertFalse(ov.isHeterozygous());
    }
    assertEquals(1, pos[0].alleleId()); // REF allele doesn't get included in orientations, so first is T.
    assertEquals(2, pos[1].alleleId());
    assertEquals(3, pos[2].alleleId());

    // Check generic version is equivalent
    final OrientedVariant[] pos2 = new Orientor.AltOrientor(1).orientations(variant);
    assertEquals(
      Arrays.stream(pos).map(OrientedVariant::toString).sorted().collect(Collectors.toList()),
      Arrays.stream(pos2).map(OrientedVariant::toString).sorted().collect(Collectors.toList())
    );
  }

  public void testDiploidAlts() throws Exception {
    final Variant variant = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.AllAlts(false)).variant(VariantTest.createRecord(SNP_LINE2), 0);

    // Test diploid combos  A T,C,G -> .:T, T:., T:T, .:C, C:., C:C, .:G, G:., G:G, T:C, C:T, T:G, G:T, C:G, G:C
    final OrientedVariant[] pos = Orientor.DIPLOID_POP.orientations(variant);
    assertEquals(15, pos.length);
    assertEquals("chr:23-24 (.v:*:T^:C:G)", pos[0].toString()); // Het uses missing rather than REF
    assertEquals("chr:23-24 (.^:*:Tv:C:G)", pos[1].toString());
    assertEquals("chr:23-24 (*:Tx:C:G)", pos[2].toString());
    assertEquals("chr:23-24 (.v:*:T:C^:G)", pos[3].toString());
    assertEquals("chr:23-24 (*:T:C^:Gv)", pos[13].toString());
    assertEquals("chr:23-24 (*:T:C:Gx)", pos[14].toString());

    // Check generic version is equivalent (order may vary)
    final OrientedVariant[] pos2 = new Orientor.AltOrientor(2).orientations(variant);
    assertEquals(
      Arrays.stream(pos).map(OrientedVariant::toString).sorted().collect(Collectors.toList()),
      Arrays.stream(pos2).map(OrientedVariant::toString).sorted().collect(Collectors.toList())
    );
  }
  
  public void testTriploidAlts() throws Exception {
    final Variant variant = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.AllAlts(false)).variant(VariantTest.createRecord(SNP_LINE2), 0);

    final OrientedVariant[] pos = new Orientor.AltOrientor(3).orientations(variant);
    assertEquals(63, pos.length);
    List<String> names = Arrays.stream(pos).map(OrientedVariant::toString).sorted().collect(Collectors.toList());
    // System.err.println(names);
    SortedSet<String> unames = new TreeSet<>(names);
    assertEquals(names.size(), unames.size());
  }

}
