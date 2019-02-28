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

package com.rtg.vcf;


import static com.rtg.vcf.VcfRecord.MISSING;
import static com.rtg.vcf.VcfUtils.FILTER_PASS;
import static com.rtg.vcf.VcfUtils.FORMAT_GENOTYPE;
import static com.rtg.vcf.VcfUtils.FORMAT_GENOTYPE_QUALITY;
import static com.rtg.vcf.VcfUtils.INFO_CIEND;
import static com.rtg.vcf.VcfUtils.INFO_CIPOS;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 */
public class VcfUtilsTest extends TestCase {

  public void test() {
    assertEquals("CIPOS", INFO_CIPOS);
    assertEquals("CIEND", INFO_CIEND);
    assertEquals(".", MISSING);
    assertEquals("PASS", FILTER_PASS);
    assertEquals("GT", FORMAT_GENOTYPE);
    assertEquals("GQ", FORMAT_GENOTYPE_QUALITY);
    assertTrue(VcfUtils.isVcfExtension(new File(".vcf")));
    assertTrue(VcfUtils.isVcfExtension(new File(".vcf.gz")));
    assertFalse(VcfUtils.isVcfExtension(new File(".blah")));
  }

  public void checkSplit(int[] expected, int[] actual) {
    assertTrue(Arrays.toString(actual), Arrays.equals(expected, actual));
  }
  public void testSplitGT() {
    checkSplit(new int[]{-1}, VcfUtils.splitGt("."));
    checkSplit(new int[]{1}, VcfUtils.splitGt("1"));
    checkSplit(new int[]{0, 1}, VcfUtils.splitGt("0|1"));
    checkSplit(new int[]{1, -1}, VcfUtils.splitGt("1|."));
    checkSplit(new int[]{-1, -1}, VcfUtils.splitGt(".|."));
    checkSplit(new int[]{123}, VcfUtils.splitGt("123"));
    checkSplit(new int[]{123, 321}, VcfUtils.splitGt("123/321"));
    checkSplit(new int[]{123, 321, 369}, VcfUtils.splitGt("123/321|369"));
    checkSplit(new int[]{3, 2, 9, 8}, VcfUtils.splitGt("3/2|9/8"));

    try {
      checkSplit(new int[] {}, VcfUtils.splitGt("|"));
      fail();
    } catch (VcfFormatException e) {
      // Expected
    }
    try {
      checkSplit(new int[] {}, VcfUtils.splitGt(""));
      fail();
    } catch (VcfFormatException e) {
      // Expected
    }
    try {
      checkSplit(new int[] {}, VcfUtils.splitGt("a"));
      fail();
    } catch (VcfFormatException e) {
      // Expected
    }
  }

  public void checkIsNonVariantGt(String gt, boolean expect) {
    assertEquals(expect, VcfUtils.isNonVariantGt(gt));
    assertEquals(VcfUtils.isNonVariantGt(gt), VcfUtils.isNonVariantGt(VcfUtils.splitGt(gt)));
  }

  public void testIsNonVariantGt() {
    checkIsNonVariantGt("1", false);
    checkIsNonVariantGt("0/1", false);
    checkIsNonVariantGt("0|1", false);
    checkIsNonVariantGt("./1", false);
    checkIsNonVariantGt(".", true);
    checkIsNonVariantGt("./.", true);
    checkIsNonVariantGt("./0", true);
    checkIsNonVariantGt("0", true);
    checkIsNonVariantGt("0|0|0", true);
  }

  public void checkIsNonMissingGt(String gt, boolean expect) {
    assertEquals(expect, VcfUtils.isNonMissingGt(gt));
    assertEquals(VcfUtils.isNonMissingGt(gt), VcfUtils.isNonMissingGt(VcfUtils.splitGt(gt)));
  }

  public void testIsNonMissingGt() {
    checkIsNonMissingGt("1", true);
    checkIsNonMissingGt("0/1", true);
    checkIsNonMissingGt("0|1", true);
    checkIsNonMissingGt(".", false);
    checkIsNonMissingGt("./.", false);
    checkIsNonMissingGt("./0", true);
    checkIsNonMissingGt("./1", true);
    checkIsNonMissingGt("0", true);
    checkIsNonMissingGt("0|0|0", true);
  }

  private VcfRecord makeRecord(String gt, String ref, String... alts) {
    final VcfRecord record = new VcfRecord("foo", 42, ref);
    for (String alt : alts) {
      record.addAltCall(alt);
    }
    record.setNumberOfSamples(1);
    record.addFormatAndSample(FORMAT_GENOTYPE, gt);
    return record;
  }

  public void testHasRedundantFirstNucleotide() {
    assertFalse(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "T")));
    assertTrue(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "AT")));
    assertFalse(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/0", "A")));
    assertTrue(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "AT", "*")));
    assertFalse(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "*")));
  }

  public void testGtProperties() {
    VcfRecord rec = makeRecord("1/1", "A", "T");
    try {
      VcfUtils.getValidGt(rec, 1);
      fail();
    } catch (NoTalkbackSlimException ignored) {
    }
    int[] gt = VcfUtils.getValidGt(rec, 0);
    assertTrue(VcfUtils.isHomozygousAlt(rec, 0));
    assertFalse(VcfUtils.isHeterozygous(rec, 0));
    assertTrue(VcfUtils.isHomozygous(gt));

    rec = makeRecord("0/1", "A", "T");
    gt = VcfUtils.getValidGt(rec, 0);
    assertFalse(VcfUtils.isHomozygousAlt(rec, 0));
    assertFalse(VcfUtils.isHomozygous(gt));
  }

  public void testHasDefinedVariantGt() {
    assertTrue(VcfUtils.hasDefinedVariantGt(makeRecord("0/1", "A", "T"), 0));
    assertFalse(VcfUtils.hasDefinedVariantGt(makeRecord("0/0", "A", "T"), 0));
    assertFalse(VcfUtils.hasDefinedVariantGt(makeRecord(".", "A", "T"), 0));
    assertFalse(VcfUtils.hasDefinedVariantGt(makeRecord("./0", "A", "T"), 0));
    assertTrue(VcfUtils.hasDefinedVariantGt(makeRecord("./1", "A", "T"), 0));
    assertFalse(VcfUtils.hasDefinedVariantGt(makeRecord("0/1", "A", "<DEL>"), 0));
    assertFalse(VcfUtils.hasDefinedVariantGt(makeRecord("0/1", "A", "T]bar:198982]"), 0));
  }

  public void testZippedVcfFileName() {
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test")).getName());
    assertEquals("test.vcf", VcfUtils.getZippedVcfFileName(false, new File("test")).getName());
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test.vcf")).getName());
    assertEquals("test.vcf", VcfUtils.getZippedVcfFileName(false, new File("test.vcf")).getName());
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test.vcf.gz")).getName());
    assertEquals("test.vcf", VcfUtils.getZippedVcfFileName(false, new File("test.vcf.gz")).getName());
    assertEquals("-", VcfUtils.getZippedVcfFileName(false, new File("-")).getName());
    assertEquals("-", VcfUtils.getZippedVcfFileName(true, new File("-")).getName());
  }

  public void testConfidenceIntervalRetrieval() {
    final VcfRecord rec = makeRecord("1/1", "A", "T");
    assertNull(VcfUtils.getConfidenceInterval(rec, INFO_CIPOS));
    rec.addInfo(INFO_CIPOS, "-10");
    rec.addInfo(INFO_CIPOS, "42");
    final int[] ci = VcfUtils.getConfidenceInterval(rec, INFO_CIPOS);
    assertEquals(2, ci.length);
    assertEquals(-10, ci[0]);
    assertEquals(42, ci[1]);
  }

  public void testNormalizeAllele() {
    assertEquals(".", ".");
    assertEquals("AGCT", VcfUtils.normalizeAllele("agct"));
    assertEquals("*", VcfUtils.normalizeAllele("*"));
    assertEquals("C<ctg1>", VcfUtils.normalizeAllele("c<ctg1>"));
    assertEquals("A]chr2:13454]ATN", VcfUtils.normalizeAllele("a]chr2:13454]atn"));
    assertEquals("A]<ctg1>:7]ATN", VcfUtils.normalizeAllele("a]<ctg1>:7]atn"));
  }

  public void testReplayAllele() {
    final Map<String, String> refs = new HashMap<>();
    //               0123 4 567890123456789 -- 0-based
    //               1234 5 678901234567890 -- 1-based
    refs.put("bar", "atcg a tcgatcgatcgatcg".replaceAll(" ", ""));
    refs.put("foo", "ATCG A TCGATCGATCGATCG".replaceAll(" ", ""));

    // INS_bar is equivalent to the haplotype that would be generated by the VCF record: "bar 5 . a GCGCGCGCGCGC . . . GT 1"
    //                     1234 567890123456 789012345678901 -- 1-based
    refs.put("<INS_bar>", "atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""));
    refs.put("<INS>", "CGCGCGCGCGC".replaceAll(" ", "")); // Just the inserted bases

    final VcfRecord rec = new VcfRecord("bar", 4, "a").addAltCall("g").addAltCall("ac");
    assertEquals("atcg g tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(rec, refs)); // SNP
    assertEquals("atcg ac tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(rec, 2, refs)); // INS

    /* Breakends - from the VCF Spec:
    REF ALT Meaning
    s t[p[ piece extending to the right of p is joined after t
    s t]p] reverse comp piece extending left of p is joined after t
    s ]p]t piece extending to the left of p is joined before t
    s [p[t reverse comp piece extending right of p is joined before t
    */
    assertEquals("atcg A TCGATCGATCGATCG".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("A[foo:5["), refs));
    assertEquals("ATCG A tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("]foo:5]A"), refs));
    assertEquals("atcg A CGAT".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("A]foo:5]"), refs));
    assertEquals("CGATCGATCGATCGA A tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("[foo:5[A"), refs));

    // Check a couple of ways of representing the same insertion (where the first base is also changed a->G)
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("GCGCGCGCGCGC"), refs)); // Non-SV insertion
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("G<INS>"), refs)); // SV contig insertion
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("G[<INS_bar>:5["), refs));
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("GCGCGCGCGCGC[<INS_bar>:16["), refs));
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("]<INS_bar>:16]C"), refs)); // Other breakend version of above
    assertEquals("atcg GCGCGCGCGCGC tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("bar", 4, "a").addAltCall("]<INS_bar>:5]GCGCGCGCGCGC"), refs)); // Other breakend version of above

    assertEquals("atcg a tcgatcgatcgatcg".replaceAll(" ", ""), VcfUtils.replayAllele(new VcfRecord("<INS_bar>", 4, "GCGCGCGCGCGC").addAltCall("a"), refs)); // Non-SV deletion, opposite of above insertion
  }
}
