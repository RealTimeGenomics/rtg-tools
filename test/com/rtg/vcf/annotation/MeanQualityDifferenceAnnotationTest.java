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

package com.rtg.vcf.annotation;

import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 *
 */
public class MeanQualityDifferenceAnnotationTest extends TestCase {

  public void test() {
    check("0/0", "1,1", "60,60", null);
    check("1/1", "1,1", "60,60", null);
    check("0/1", "0,1", "60,60", null);
    check("0/1", "1,0", "60,60", null);
    check("./1", "1,1", "60,60", null);
    check("1/.", "1,1", "60,60", null);
    check("0/1", "1,1", "60,60", 0.0);
    check("0/1", "1,1", "70,60", 10.0);
    check("0/1", "1,2", "70,60", 40.0);
    check("0/2", "1,0,2", "70,50,60", 40.0);
    check("1/2", "1,1,2", "70,50,60", 20.0);
    check("1/2", "1,1,2", "70,20,60", 10.0);
    check("2/1", "1,1,2", "70,20,60", 10.0);
  }

  public void check(String gt, String ad, String aq, Double expQad) {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addFormatAndSample("GT", gt);
    rec.addFormatAndSample("AD", ad);
    rec.addFormatAndSample("AQ", aq);
    assertEquals(expQad, new MeanQualityDifferenceAnnotation().getValue(rec, 0));
  }

  public void testNoAd() {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addFormatAndSample("GT", "1/0");
    assertNull(new MeanQualityDifferenceAnnotation().getValue(rec, 0));
  }
}
