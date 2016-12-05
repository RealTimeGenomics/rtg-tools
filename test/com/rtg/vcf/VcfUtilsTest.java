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


import java.io.File;
import java.util.Arrays;

import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 */
public class VcfUtilsTest extends TestCase {

  public void test() {
    assertEquals("CIPOS", VcfUtils.CONFIDENCE_INTERVAL_POS);
    assertEquals(".", VcfRecord.MISSING);
    assertEquals("PASS", VcfUtils.FILTER_PASS);
    assertEquals("GT", VcfUtils.FORMAT_GENOTYPE);
    assertEquals("GQ", VcfUtils.FORMAT_GENOTYPE_QUALITY);
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

  public void testIsNonVariantGt() {
    assertFalse(VcfUtils.isNonVariantGt("1"));
    assertFalse(VcfUtils.isNonVariantGt("0/1"));
    assertFalse(VcfUtils.isNonVariantGt("0|1"));
    assertFalse(VcfUtils.isNonVariantGt("./1"));
    assertTrue(VcfUtils.isNonVariantGt("."));
    assertTrue(VcfUtils.isNonVariantGt("./."));
    assertTrue(VcfUtils.isNonVariantGt("./0"));
    assertTrue(VcfUtils.isNonVariantGt("0"));
    assertTrue(VcfUtils.isNonVariantGt("0|0|0"));
  }

  public void testIsNonMissingGt() {
    assertTrue(VcfUtils.isNonMissingGt("1"));
    assertTrue(VcfUtils.isNonMissingGt("0/1"));
    assertTrue(VcfUtils.isNonMissingGt("0|1"));
    assertFalse(VcfUtils.isNonMissingGt("."));
    assertFalse(VcfUtils.isNonMissingGt("./."));
    assertTrue(VcfUtils.isNonMissingGt("./0"));
    assertTrue(VcfUtils.isNonMissingGt("./1"));
    assertTrue(VcfUtils.isNonMissingGt("0"));
    assertTrue(VcfUtils.isNonMissingGt("0|0|0"));
  }

  private VcfRecord makeRecord(String gt, String ref, String... alts) {
    final VcfRecord record = new VcfRecord("foo", 42, ref);
    for (String alt : alts) {
      record.addAltCall(alt);
    }
    record.setNumberOfSamples(1);
    record.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gt);
    return record;
  }

  public void testHasRedundantFirstNucleotide() {
    assertFalse(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "T")));
    assertTrue(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/1", "A", "AT")));
    assertFalse(VcfUtils.hasRedundantFirstNucleotide(makeRecord("0/0", "A")));
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
  }

}
