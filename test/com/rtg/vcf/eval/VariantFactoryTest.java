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

import static com.rtg.mode.DNA.C;
import static com.rtg.mode.DNA.T;
import static com.rtg.util.StringUtils.TAB;

import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 */
public class VariantFactoryTest extends TestCase {

  private Variant createVariant(VariantFactory fact, String var) {
    final String vartab = var.replaceAll(" +", TAB);
    final VcfRecord rec = VcfReader.vcfLineToRecord(vartab);
    return fact.variant(rec, 0);
  }

  private VariantFactory factory(String name) {
    switch (name) {
      case VariantFactory.DiploidAltsFactory.NAME:
        return new VariantFactory.DiploidAltsFactory();
      case VariantFactory.HaploidAltsFactory.NAME:
        return new VariantFactory.HaploidAltsFactory();
      case VariantFactory.HaploidGtAltFactory.NAME:
        return new VariantFactory.HaploidGtAltFactory(0);
      case VariantFactory.TrimmedGtFactory.NAME:
        return new VariantFactory.TrimmedGtFactory(0);
      case VariantFactory.Default.NAME:
        return new VariantFactory.Default(0);
      default:
        throw new RuntimeException("Unknown variant factory: " + name);
    }
  }

  static final String SNP_LINE2 = "chr 23 . A T,C,G . PASS . GT 1/2";
  static final String SNP_LINE3 = "chr 23 . A T     . PASS . GT 0/1";
  static final String SNP_LINE4 = "chr 23 . A T     . PASS . GT ./1";

  public void testSquashPloidy() throws Exception {
    // Test squashing ploidy  1/2 -> 1, 2
    VariantFactory fact = factory(VariantFactory.HaploidGtAltFactory.NAME);
    Variant variant = createVariant(fact, SNP_LINE2);
    assertEquals(1, variant.nt(0).length);
    assertEquals(T.ordinal(), variant.nt(0)[0]);
    assertEquals(C.ordinal(), variant.nt(1)[0]);
    final OrientedVariant[] pos = variant.orientations();
    assertEquals(2, pos.length);
    assertTrue(pos[0].isAlleleA());
    assertTrue(pos[1].isAlleleA());
    assertFalse(pos[0].isHeterozygous());
    assertFalse(pos[1].isHeterozygous());
    assertEquals(0, pos[0].alleleId()); // REF allele doesn't get counted in ids
    assertEquals(1, pos[1].alleleId());

    // Test squashing ploidy  0/1 -> 1
    variant = createVariant(fact, SNP_LINE3);
    assertEquals(1, variant.nt(0).length);
    assertEquals(T.ordinal(), variant.nt(0)[0]);
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }

    // Test squashing ploidy  ./1 -> 1
    variant = createVariant(fact, SNP_LINE4);
    assertEquals(1, variant.nt(0).length);
    assertEquals(T.ordinal(), variant.nt(0)[0]);
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }
  }

  static final String SNP_LINE5 = "chr 23 . AA T,ATTA . PASS . GT 0/2";

  public void testTrimmedGtPloidy() throws Exception {
    // Test trimming ploidy  AA:ATTA -> :TT
    VariantFactory fact = factory(VariantFactory.TrimmedGtFactory.NAME);
    Variant variant = createVariant(fact, SNP_LINE5);
    assertEquals(23, variant.getStart());
    assertEquals(23, variant.getStart());
    assertEquals(2, variant.numAlleles());
    assertEquals(0, variant.nt(0).length);
    assertEquals(2, variant.nt(1).length);
    assertEquals(T.ordinal(), variant.nt(1)[0]);
    assertEquals(T.ordinal(), variant.nt(1)[1]);
    final OrientedVariant[] pos = variant.orientations();
    assertEquals(2, pos.length);
  }

}
