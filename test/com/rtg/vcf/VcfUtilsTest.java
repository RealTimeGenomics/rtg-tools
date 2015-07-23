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

import com.rtg.vcf.annotation.DerivedAnnotations;

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
    checkSplit(new int[] {-1}, VcfUtils.splitGt("."));
    checkSplit(new int[] {1}, VcfUtils.splitGt("1"));
    checkSplit(new int[] {0, 1}, VcfUtils.splitGt("0|1"));
    checkSplit(new int[] {1, -1}, VcfUtils.splitGt("1|."));
    checkSplit(new int[] {-1, -1}, VcfUtils.splitGt(".|."));
    checkSplit(new int[] {123}, VcfUtils.splitGt("123"));
    checkSplit(new int[] {123, 321}, VcfUtils.splitGt("123/321"));
    checkSplit(new int[] {123, 321, 369}, VcfUtils.splitGt("123/321|369"));
    checkSplit(new int[] {3, 2, 9, 8}, VcfUtils.splitGt("3/2|9/8"));

    try {
      checkSplit(new int[] {}, VcfUtils.splitGt("|"));
      fail();
    } catch (NumberFormatException e) {
      // Expected
    }
    try {
      checkSplit(new int[] {}, VcfUtils.splitGt(""));
      fail();
    } catch (NumberFormatException e) {
      // Expected
    }
    try {
      checkSplit(new int[] {}, VcfUtils.splitGt("a"));
      fail();
    } catch (NumberFormatException e) {
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
    final VcfRecord record = new VcfRecord();
    record.setSequence("foo");
    record.setStart(42);
    record.setRefCall(ref);
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

  public void testSkipRecordForSample() {
    assertFalse(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "T"), 0, true));
    assertFalse(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "T").addFilter("PASS"), 0, true));
    assertFalse(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "T").addFilter("FAIL"), 0, false));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "T").addFilter("FAIL"), 0, true));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord("0/0", "A", "T"), 0, true));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord(".", "A", "T"), 0, true));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord("./0", "A", "T"), 0, true));
    assertFalse(VcfUtils.skipRecordForSample(makeRecord("./1", "A", "T"), 0, false));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "<DEL>"), 0, true));
    assertTrue(VcfUtils.skipRecordForSample(makeRecord("0/1", "A", "T]bar:198982]"), 0, true));
  }

  public void testZippedVcfFileName() {
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test")).getName());
    assertEquals("test.vcf", VcfUtils.getZippedVcfFileName(false, new File("test")).getName());
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test.vcf")).getName());
    assertEquals("test.vcf", VcfUtils.getZippedVcfFileName(false, new File("test.vcf")).getName());
    assertEquals("test.vcf.gz", VcfUtils.getZippedVcfFileName(true, new File("test.vcf.gz")).getName());
    assertEquals("test.vcf.gz.vcf", VcfUtils.getZippedVcfFileName(false, new File("test.vcf.gz")).getName());
  }

  public void testGetAnnotator() {
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.IC) instanceof VcfInfoDoubleAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.EP) instanceof VcfInfoDoubleAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.LAL) instanceof VcfInfoIntegerAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.QD) instanceof VcfInfoDoubleAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.NAA) instanceof VcfInfoIntegerAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.AC) instanceof VcfInfoPerAltIntegerAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.AN) instanceof VcfInfoIntegerAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.GQD) instanceof VcfFormatDoubleAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.ZY) instanceof VcfFormatStringAnnotator);
    assertTrue(VcfUtils.getAnnotator(DerivedAnnotations.PD) instanceof VcfFormatStringAnnotator);
    try {
      VcfUtils.getAnnotator(null);
      fail("Worked with null annotation");
    } catch (NullPointerException e) {
      //expected
    }
  }

}
