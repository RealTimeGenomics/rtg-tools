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

import com.rtg.util.TestUtils;
import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 */
public class RocScoreFieldTest extends TestCase {

  public void test() {
    TestUtils.testEnum(RocScoreField.class, "[QUAL, INFO, FORMAT, DERIVED]");
    final VcfRecord rec = new VcfRecord("chr1", 1209, "a");
    rec.setId(".")
    .setQuality("12.8")
    .addAltCall("c")
    .addAltCall("t")
    .addFilter("TEST1")
    .addFilter("TEST2")
    .setInfo("DP", "23")
    .setInfo("TEST", "45", "46", "47", "48")
    .setNumberOfSamples(2)
    .addFormatAndSample("GT", "0/0")
    .addFormatAndSample("GT", "0/1")
    .addFormatAndSample("GQ", "100")
    .addFormatAndSample("GQ", "95");
    assertEquals(100.0, RocScoreField.FORMAT.getExtractor("GQ", RocSortOrder.DESCENDING).getSortValue(rec, 0));
    assertEquals(95.0, RocScoreField.FORMAT.getExtractor("GQ", RocSortOrder.ASCENDING).getSortValue(rec, 1));
    assertEquals(12.8, RocScoreField.QUAL.getExtractor("GQ", RocSortOrder.DESCENDING).getSortValue(rec, 1));
    assertEquals(23.0, RocScoreField.INFO.getExtractor("DP", RocSortOrder.DESCENDING).getSortValue(rec, 0));
    assertEquals(0.271, RocScoreField.DERIVED.getExtractor("EP", RocSortOrder.DESCENDING).getSortValue(rec, 1), 0.01);
    assertEquals(12.8 / 23.0, RocScoreField.DERIVED.getExtractor("QD", RocSortOrder.DESCENDING).getSortValue(rec, 0), 0.01);
    assertEquals(0.0, RocSortValueExtractor.NULL_EXTRACTOR.getSortValue(rec, 0));
  }
}
