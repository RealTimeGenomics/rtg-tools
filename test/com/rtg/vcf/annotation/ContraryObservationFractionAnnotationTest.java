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
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class ContraryObservationFractionAnnotationTest extends TestCase {

  public void testName() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    assertEquals("COF", an.getName());
    assertEquals("Contrary observation fraction", an.getDescription());
    assertEquals(AnnotationDataType.DOUBLE, an.getType());
    assertEquals("Derived annotation COF missing required fields in VCF header (FORMAT fields: SS AD GT)", an.checkHeader(new VcfHeader()));
  }

  static VcfHeader makeHeader() {
    final VcfHeader header = new VcfHeader();
    header.addSampleName("normal");
    header.addSampleName("cancer");
    header.addLine(VcfHeader.PEDIGREE_STRING + "=<Derived=cancer,Original=normal>");
    header.addFormatField(new FormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, new VcfNumber("1"), "Genotype"));
    header.addFormatField(new FormatField(VcfUtils.FORMAT_SOMATIC_STATUS, MetaType.INTEGER, new VcfNumber("1"), "Somatic status"));
    header.addFormatField(new FormatField(VcfUtils.FORMAT_ALLELIC_DEPTH, MetaType.INTEGER, VcfNumber.DOT, "Allelic depth"));
    return header;
  }

  public void testSomaticCase() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    an.checkHeader(makeHeader());
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
    assertEquals(0.1, (Double) an.getValue(rec, 1), 1e-12);
    assertNull(an.getValue(rec, 2));
  }

  public void testGainOfReference() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    an.checkHeader(makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addAltCall("G");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "9,1");
    assertNull(an.getValue(rec, 0));
    assertEquals(0.1, (Double) an.getValue(rec, 1), 1e-12);
    assertNull(an.getValue(rec, 2));
  }

  public void testNonsomaticCase() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    an.checkHeader(makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addAltCall("G");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "0");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "9,1");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");
    assertNull(an.getValue(rec, 0));
    assertNull(an.getValue(rec, 1));
  }

  public void testNoCoverageInNormal() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    an.checkHeader(makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addAltCall("G");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "0,0");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");
    assertNull(an.getValue(rec, 0));
    assertNull(an.getValue(rec, 1));
  }

  public void testTriallelic() {
    final ContraryObservationFractionAnnotation an = new ContraryObservationFractionAnnotation();
    an.checkHeader(makeHeader());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    rec.addAltCall("G");
    rec.addAltCall("C");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/2");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "5,5,2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,1,5");
    assertNull(an.getValue(rec, 0));
    assertEquals(3.0 / 12.0, (Double) an.getValue(rec, 1), 1e-10);
  }

}
