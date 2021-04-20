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

import static com.rtg.mode.DNA.T;

import com.rtg.mode.DnaUtils;
import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 */
public class VariantFactoryTest extends TestCase {

  static class SimpleRefTrimmer implements VariantFactory {
    private final VariantFactory mFactory;
    SimpleRefTrimmer(VariantFactory f) {
      mFactory = f;
    }
    @Override
    public Variant variant(VcfRecord rec, int id) throws SkippedVariantException {
      final Variant v = mFactory.variant(rec, id);
      if (v != null) {
        v.trimAlleles();
      }
      return v;
    }
  }

  static final String SNP_LINE2 = "chr 23 . A T,C,G . PASS . GT 1/2";
  static final String SNP_LINE2_F = "chr 23 . A T,C,G . FAIL . GT 1/2";
  static final String SNP_LINE2_FT = "chr 23 . A T,C,G . PASS . GT:FT 1/2:FAIL";
  static final String SNP_LINE3 = "chr 23 . A T     . PASS . GT 0/1";
  static final String SNP_LINE4 = "chr 23 . A T     . PASS . GT ./1";
  static final String SNP_LINE2_3 = "chr 23 . A T,C,G . PASS . GT 1/2/3";

  public void testFiltered() throws Exception {
    VariantFactory.SampleVariants fact = new VariantFactory.SampleVariants(0, true);
    assertNotNull(fact.variant(VariantTest.createRecord(SNP_LINE2), 0));
    assertNull(fact.variant(VariantTest.createRecord(SNP_LINE2_F), 0));
    assertNull(fact.variant(VariantTest.createRecord(SNP_LINE2_FT), 0));
    fact = new VariantFactory.SampleVariants(0, true, 2, false);
    assertNotNull(fact.variant(VariantTest.createRecord(SNP_LINE2_FT), 0));

    VariantFactory.AllAlts afact = new VariantFactory.AllAlts(true, true);
    assertNotNull(afact.variant(VariantTest.createRecord(SNP_LINE2), 0));
    assertNull(afact.variant(VariantTest.createRecord(SNP_LINE2_F), 0));
    assertNotNull(afact.variant(VariantTest.createRecord(SNP_LINE2_FT), 0));
  }
  
  public void testUntrimmedGt() throws Exception {
    // Test 1/2 -> 1, 2
    VariantFactory.SampleVariants fact = new VariantFactory.SampleVariants(0, true);
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE2), 0);
    assertEquals(4, variant.numAlleles());
    assertNull(variant.allele(-1));
    assertNotNull(variant.allele(0)); // Ref allele just kept for potential trimming
    assertNull(variant.allele(3));
    assertEquals(1, variant.nt(1).length);
    assertEquals(1, variant.nt(2).length);
    assertEquals("T", variant.alleleStr(1));
    assertEquals("C", variant.alleleStr(2));
    assertEquals("*", variant.alleleStr(3));

    // Test 0/1 -> 1
    variant = fact.variant(VariantTest.createRecord(SNP_LINE3), 0);
    assertNull(variant.allele(-1));
    assertNotNull(variant.allele(0));
    assertEquals(1, variant.nt(1).length);
    assertEquals("T", variant.alleleStr(1));
    try {
      variant.nt(2);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }

    // Test ./1 -> ., 1
    variant = fact.variant(VariantTest.createRecord(SNP_LINE4), 0);
    assertNotNull(variant.allele(-1));
    assertNotNull(variant.allele(0)); // Ref allele just kept for potential trimming
    assertEquals(1, variant.nt(1).length);
    assertEquals(T.ordinal(), variant.nt(1)[0]);

    // Test 1/2/3 -> 1, 2, 3
    fact = new VariantFactory.SampleVariants(0, true, 3, true);
    variant = fact.variant(VariantTest.createRecord(SNP_LINE2_3), 0);
    assertEquals(4, variant.numAlleles());
    assertNull(variant.allele(-1));
    assertNotNull(variant.allele(0)); // Ref allele just kept for potential trimming
    assertNotNull(variant.allele(3));
    assertEquals("T", variant.alleleStr(1));
    assertEquals("C", variant.alleleStr(2));
    assertEquals("G", variant.alleleStr(3));
  }

  static final String SNP_LINE5 = "chr 23 . AA T,ATTA . PASS . GT 0/2";
  static final String SNP_LINE6 = "chr 23 . T A,TAAA . PASS . GT 2|1";
  static final String SNP_LINE7 = "chr 23 . T A,TAAA . PASS . GT 2|.";

  public void testTrimmedGt() throws Exception {
    // Test  AA:ATTA -> :TT
    final VariantFactory fact = new SimpleRefTrimmer(new VariantFactory.SampleVariants(0, false));
    Variant variant = fact.variant(VariantTest.createRecord(SNP_LINE5), 0);
    assertEquals(23, variant.getStart());
    assertEquals(23, variant.getStart());
    assertEquals(3, variant.numAlleles());
    assertEquals(null, variant.allele(0));
    assertEquals(null, variant.allele(1));
    assertEquals("TT", variant.alleleStr(2));
    assertTrue(variant instanceof GtIdVariant);
    GtIdVariant av = (GtIdVariant) variant;
    assertEquals(0, av.alleleA());
    assertEquals(2, av.alleleB());

    variant = fact.variant(VariantTest.createRecord(SNP_LINE6), 0);
    assertEquals(22, variant.getStart());
    assertEquals(23, variant.getEnd());
    assertEquals(3, variant.numAlleles());
    assertEquals(null, variant.allele(0));
    Allele allele = variant.allele(1);
    assertEquals("A", DnaUtils.bytesToSequenceIncCG(allele.nt()));
    assertEquals(22, allele.getStart());
    assertEquals(23, allele.getEnd());
    allele = variant.allele(2);
    assertEquals("AAA", DnaUtils.bytesToSequenceIncCG(allele.nt()));
    assertEquals(23, allele.getStart());
    assertEquals(23, allele.getEnd());
    assertEquals("chr:23-24 (<24-24>AAA:A)", variant.toString());
    assertTrue(variant instanceof GtIdVariant);
    av = (GtIdVariant) variant;
    assertEquals(2, av.alleleA());
    assertEquals(1, av.alleleB());

    variant = fact.variant(VariantTest.createRecord(SNP_LINE7), 0);
    assertEquals("chr:24-24 (AAA:.)", variant.toString());
    assertTrue(variant instanceof GtIdVariant);
    av = (GtIdVariant) variant;
    assertEquals(2, av.alleleA());
    assertEquals(-1, av.alleleB());
  }

  static final String SNP_LINE8 = "chr 23 . T A,*,TAAA . PASS . GT 2|.";
  static final String SNP_LINE9 = "chr 23 . T A,TAAA,* . PASS . GT 2|.";
  static final String SNP_LINE10 = "chr 23 . T <DEL>,* . PASS . GT 2|.";
  static final String SNP_LINE11 = "chr 23 . T * . PASS . GT 0|1";
  static final String SNP_LINE12 = "chr 23 . TAAAA TA,*AAAA . PASS . GT 2|1";

  public void testAlts() throws Exception {
    final VariantFactory f = new VariantFactoryTest.SimpleRefTrimmer(new VariantFactory.AllAlts(false, false));
    Variant variant = f.variant(VariantTest.createRecord(SNP_LINE2), 0);
    assertEquals(4, variant.numAlleles());
    assertEquals(null, variant.allele(-1)); // missing allele is ignored
    assertEquals(null, variant.allele(0)); // REF allele is trimmed away entirely
    assertEquals("T", variant.alleleStr(1));
    assertEquals("C", variant.alleleStr(2));
    assertEquals("G", variant.alleleStr(3));

    variant = f.variant(VariantTest.createRecord(SNP_LINE8), 0);
    assertEquals(3, variant.numAlleles()); // spanning deletion allele is not included in allele set
    assertEquals(null, variant.allele(-1)); // missing allele is ignored
    assertEquals(null, variant.allele(0)); // REF allele is trimmed away entirely
    assertEquals("A", variant.alleleStr(1));
    assertEquals("<24-24>AAA", variant.alleleStr(2));

    variant = f.variant(VariantTest.createRecord(SNP_LINE9), 0);
    assertEquals(3, variant.numAlleles()); // spanning deletion allele is not included in allele set

    assertNull(f.variant(VariantTest.createRecord(SNP_LINE10), 0)); // No alleles remaining

    assertNull(VariantFactory.getDefinedVariantGt(VariantTest.createRecord(SNP_LINE11), 0, 2));
    assertNull(f.variant(VariantTest.createRecord(SNP_LINE11), 0)); // No alleles remaining
    assertNotNull(VariantFactory.getDefinedVariantGt(VariantTest.createRecord(SNP_LINE12), 0, 2));
    variant = f.variant(VariantTest.createRecord(SNP_LINE12), 0);
    assertEquals(2, variant.numAlleles()); // partial spanning deletion allele is not included in allele set
  }

  static final String BADALT_LINE1 = "chr 23 . T G--- . PASS . GT 1|1";

  public void testBadAlts() {
    try {
      new VariantFactory.SampleVariants(0, false).variant(VariantTest.createRecord(BADALT_LINE1), 0);
      fail();
    } catch (SkippedVariantException e) {
      // Expected
    }
    try {
      new VariantFactory.AllAlts(false, false).variant(VariantTest.createRecord(BADALT_LINE1), 0);
      fail();
    } catch (SkippedVariantException e) {
      // Expected
    }
  }

}
