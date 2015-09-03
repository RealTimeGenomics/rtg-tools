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
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class ContraryObservationCountAnnotationTest extends TestCase {

  public void testName() {
    final ContraryObservationCountAnnotation an = new ContraryObservationCountAnnotation();
    assertEquals("COC", an.getName());
    assertEquals("Contrary observation count", an.getDescription());
    assertEquals(AnnotationDataType.INTEGER, an.getType());
    assertEquals("Derived annotation COC missing required fields in VCF header (FORMAT fields: SS AD GT)", an.checkHeader(new VcfHeader()));
  }

  public void testSomaticCase() {
    final ContraryObservationCountAnnotation an = new ContraryObservationCountAnnotation();
    an.checkHeader(ContraryObservationFractionAnnotationTest.makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("G");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "9,1");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");
    assertNull(an.getValue(rec, 0));
    assertEquals(1, an.getValue(rec, 1));
    assertNull(an.getValue(rec, 2));
  }

  public void testGainOfReference() {
    final ContraryObservationCountAnnotation an = new ContraryObservationCountAnnotation();
    an.checkHeader(ContraryObservationFractionAnnotationTest.makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("G");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "9,2");
    assertNull(an.getValue(rec, 0));
    assertEquals(2, an.getValue(rec, 1));
    assertNull(an.getValue(rec, 2));
  }
}
