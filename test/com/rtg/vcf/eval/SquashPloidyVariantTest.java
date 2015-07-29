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

import static com.rtg.util.StringUtils.TAB;

import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class SquashPloidyVariantTest extends TestCase {

  private static final RocSortValueExtractor DEFAULT_EXTRACTOR = RocScoreField.FORMAT.getExtractor(VcfUtils.FORMAT_GENOTYPE_QUALITY, RocSortOrder.DESCENDING);

  private Variant createVariant(String var) {
    final String vartab = var.replaceAll(" ", TAB);
    final VcfRecord rec = VcfReader.vcfLineToRecord(vartab);
    return new SquashPloidyVariant.GtAltFactory(0, DEFAULT_EXTRACTOR).variant(rec, 0);
  }

  static final String SNP_LINE2 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T,C" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "1/2:4:0.02:12.8";
  static final String SNP_LINE3 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "0/1:4:0.02:12.8";
  static final String SNP_LINE4 = "someKindOfName" + TAB + "23" + TAB + "." + TAB + "A" + TAB + "T" + TAB + "12.8" + TAB + "PASS" + TAB + "." + TAB + "GT:DP:RE:GQ" + TAB + "./1:4:0.02:12.8";

  public void testHeterozygousSnpConstruction() throws Exception {
    // Test squashing ploidy  1/2 -> 1, 2
    Variant variant = createVariant(SNP_LINE2);
    assertEquals(1, variant.nt(0).length);
    assertEquals(4, variant.nt(0)[0]);
    assertEquals(2, variant.nt(1)[0]);
    final OrientedVariant[] pos = variant.orientations();
    assertEquals(2, pos.length);
    assertTrue(pos[0].isAlleleA());
    assertTrue(pos[1].isAlleleA());
    assertFalse(pos[0].isHeterozygous());
    assertFalse(pos[1].isHeterozygous());
    assertEquals(0, pos[0].alleleId()); // REF allele doesn't get counted in ids
    assertEquals(1, pos[1].alleleId());

    // Test squashing ploidy  0/1 -> 1
    variant = createVariant(SNP_LINE3);
    assertEquals(1, variant.nt(0).length);
    assertEquals(4, variant.nt(0)[0]);
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }

    // Test squashing ploidy  ./1 -> 1
    variant = createVariant(SNP_LINE4);
    assertEquals(1, variant.nt(0).length);
    assertEquals(4, variant.nt(0)[0]);
    try {
      variant.nt(1);
      fail();
    } catch (ArrayIndexOutOfBoundsException ignored) {
    }
  }
}
