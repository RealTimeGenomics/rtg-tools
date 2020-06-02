/*
 * Copyright (c) 2021. Real Time Genomics Limited.
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
package com.rtg.vcf;

import static com.rtg.vcf.VariantType.DELETION;
import static com.rtg.vcf.VariantType.INDEL;
import static com.rtg.vcf.VariantType.INSERTION;
import static com.rtg.vcf.VariantType.MNP;
import static com.rtg.vcf.VariantType.SNP;
import static com.rtg.vcf.VariantType.SV_BREAKEND;
import static com.rtg.vcf.VariantType.SV_MISSING;
import static com.rtg.vcf.VariantType.SV_SYMBOLIC;
import static com.rtg.vcf.VariantType.UNCHANGED;

import com.rtg.AbstractTest;

public class VariantTypeTest extends AbstractTest {

  public void testGetVariantType() {
    assertEquals(UNCHANGED, VariantType.getType("C", "C"));
    assertEquals(SNP, VariantType.getType("C", "T"));
    assertEquals(MNP, VariantType.getType("CG", "TT"));
    assertEquals(DELETION, VariantType.getType("CG", ""));
    assertEquals(DELETION, VariantType.getType("CG", "C"));
    assertEquals(DELETION, VariantType.getType("CG", "G"));
    assertEquals(INSERTION, VariantType.getType("", "TT"));
    assertEquals(INSERTION, VariantType.getType("C", "CTT"));
    assertEquals(INSERTION, VariantType.getType("T", "CTT"));
    assertEquals(INDEL, VariantType.getType("CG", "TTA"));
    assertEquals(SV_SYMBOLIC, VariantType.getType("C", "C<ctg1>"));
    assertEquals(SV_BREAKEND, VariantType.getType("C", "C[2:123["));
    assertEquals(SV_BREAKEND, VariantType.getType("C", "CTAT[<ctg1>["));
    assertEquals(SV_BREAKEND, VariantType.getType("C", "]<ctg1:123>]CTAT"));
    assertEquals(SV_MISSING, VariantType.getType("C", "*"));
  }

  public void testGetVariantTypeRec() {
    final VcfRecord r = VcfReaderTest.vcfLineToRecord("1 1300068 . TCTGCGGGGGCAGCACAGGTGAGGCCCAAGCACACCCGGTCCAGCCCCCAACATGCAGCCTGTGCTCAGGGGCAGCCCCCACGCACTCAC T,TTCAC 1959.58 . AC=27,1;AF=0.900,0.033;AN=30 GT 0/0/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/1/2".replaceAll(" ", "\t"));
    final String gt = r.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0);
    final int[] gtSplit = VcfUtils.splitGt(gt);
    final VariantType t = VariantType.getType(r, gtSplit);
    assertEquals(DELETION, t);

    final VcfRecord r2 = VcfReaderTest.vcfLineToRecord("1 1300068 . T TTCAC 1959.58 . . GT .".replaceAll(" ", "\t"));
    assertEquals(VariantType.NO_CALL, VariantType.getType(r2, VcfUtils.splitGt(r2.getFormat(VcfUtils.FORMAT_GENOTYPE).get(0))));
  }

  public void testGetPrecedence() {
    assertEquals(SNP, VariantType.getPrecedence(SNP));
    assertEquals(INSERTION, VariantType.getPrecedence(SNP, INSERTION));
    assertEquals(INSERTION, VariantType.getPrecedence(SNP, MNP, INSERTION));
    assertEquals(MNP, VariantType.getPrecedence(SNP, SNP, SNP, MNP, SNP));
    assertEquals(INDEL, VariantType.getPrecedence(DELETION, MNP, INSERTION));
    assertEquals(SV_BREAKEND, VariantType.getPrecedence(SNP, SV_BREAKEND, SNP));
  }
}
