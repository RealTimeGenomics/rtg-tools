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
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class QualOverDepthAnnotationTest extends TestCase {
  public void testName() {
    final QualOverDepthAnnotation ann = new QualOverDepthAnnotation();
    assertEquals("QD", ann.getName());
    assertEquals("QUAL / DP", ann.getDescription());
    assertEquals(MetaType.FLOAT, ann.getField().getType());
  }

  public void test() {
    final QualOverDepthAnnotation ann = new QualOverDepthAnnotation();
    VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.addInfo("DP", "123");
    assertEquals(8.029, ann.getValue(rec, -1), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.addInfo("DP", "0");
    assertEquals(Double.POSITIVE_INFINITY, ann.getValue(rec, -1), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.addInfo("DP", "123");
    assertNull(ann.getValue(rec, 23));

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    assertNull(ann.getValue(rec, 0));

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.addFormatAndSample("DP", "123");
    assertEquals(8.029, ann.getValue(rec, 0), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.addFormatAndSample("DP", "0");
    assertEquals(Double.POSITIVE_INFINITY, ann.getValue(rec, -1), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample("DP", "63");
    rec.addFormatAndSample("DP", "20");
    assertEquals(11.899, ann.getValue(rec, 0), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.setNumberOfSamples(3);
    rec.addFormatAndSample("DP", "63");
    rec.addFormatAndSample("DP", "20");
    assertEquals(11.899, ann.getValue(rec, 0), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.setNumberOfSamples(3);
    rec.addFormatAndSample("DP", "63");
    rec.addFormatAndSample("DP", ".");
    rec.addFormatAndSample("DP", "20");
    assertEquals(11.899, ann.getValue(rec, 0), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.setNumberOfSamples(3);
    rec.addFormatAndSample("DP", "63");
    rec.addFormatAndSample("DP", "0");
    rec.addFormatAndSample("DP", "20");
    assertEquals(11.899, ann.getValue(rec, 0), 0.001);

    rec = new VcfRecord("seq", 0, "A");
    rec.setQuality("987.6");
    rec.addInfo("DP", "100");
    rec.setNumberOfSamples(3);
    rec.addFormatAndSample("DP", "63");
    rec.addFormatAndSample("DP", "0");
    rec.addFormatAndSample("DP", "20");
    assertEquals(9.876, ann.getValue(rec, 0), 0.001);

    assertEquals("Derived annotation QD missing required fields in VCF header (INFO fields: DP)", ann.checkHeader(new VcfHeader()));
    final VcfHeader header = new VcfHeader();
    header.addFormatField("DP", MetaType.INTEGER, VcfNumber.ONE, "Depth");
    assertNull(ann.checkHeader(header));
  }

}
