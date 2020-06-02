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
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class GenotypeQualityOverDepthAnnotationTest extends TestCase {

  public void testName() {
    final GenotypeQualityOverDepthAnnotation gqdAnn = new GenotypeQualityOverDepthAnnotation();
    assertEquals("GQD", gqdAnn.getName());
    assertEquals("GQ / DP for a single sample", gqdAnn.getDescription());
    assertEquals(MetaType.FLOAT, gqdAnn.getField().getType());
  }

  public void test() {
    final GenotypeQualityOverDepthAnnotation gqdAnn = new GenotypeQualityOverDepthAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(6);
    rec.addFormatAndSample("GQ", "30");
    rec.addFormatAndSample("GQ", "40");
    rec.addFormatAndSample("GQ", "50");
    rec.addFormatAndSample("GQ", ".");
    rec.addFormatAndSample("GQ", "10");
    rec.addFormatAndSample("GQ", "10");
    rec.addFormatAndSample("DP", "50");
    rec.addFormatAndSample("DP", "40");
    rec.addFormatAndSample("DP", "30");
    rec.addFormatAndSample("DP", "10");
    rec.addFormatAndSample("DP", ".");
    rec.addFormatAndSample("DP", "0");
    assertEquals(0.6, (double) gqdAnn.getValue(rec, 0), 0.0001);
    assertEquals(1.0, (double) gqdAnn.getValue(rec, 1), 0.0001);
    assertEquals(1.6666, (double) gqdAnn.getValue(rec, 2), 0.0001);
    assertNull(gqdAnn.getValue(rec, 3));
    assertNull(gqdAnn.getValue(rec, 4));
    assertEquals(Double.POSITIVE_INFINITY, (double) gqdAnn.getValue(rec, 5), 0.01);
    assertNull(gqdAnn.getValue(rec, 6));
    rec.setNumberOfSamples(7);
    rec.addFormatAndSample("GQ", "11");
    assertNull(gqdAnn.getValue(rec, 0));
    assertEquals("Derived annotation GQD missing required fields in VCF header (FORMAT fields: DP GQ)", gqdAnn.checkHeader(new VcfHeader()));
  }
}
