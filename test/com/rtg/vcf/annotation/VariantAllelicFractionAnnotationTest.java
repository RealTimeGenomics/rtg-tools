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
public class VariantAllelicFractionAnnotationTest extends TestCase {

  public void testName() {
    final VariantAllelicFractionAnnotation annotation = new VariantAllelicFractionAnnotation();
    assertEquals("VAF", annotation.getName());
    assertEquals("Variant Allelic Fraction", annotation.getDescription());
    assertEquals(MetaType.FLOAT, annotation.getField().getType());
  }

  public void test() {
    final VariantAllelicFractionAnnotation annotation = new VariantAllelicFractionAnnotation();
    VcfRecord rec = new VcfRecord("seq", 0, "AAA")
      .addAltCall("A")
      .setNumberOfSamples(1)
      .addFormatAndSample("ADE", "6.0,2.0");
    assertEquals(0.25, ((double[]) annotation.getValue(rec, 0))[0], 0.0001);

    rec = new VcfRecord("seq", 0, "AAA")
      .setNumberOfSamples(1)
      .addAltCall("A")
      .addFormatAndSample("AD", "6,2");
    assertEquals(0.25, ((double[]) annotation.getValue(rec, 0))[0], 0.0001);
  }

  public void testMissingPrerequisites() {
    final VariantAllelicFractionAnnotation annotation = new VariantAllelicFractionAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "AAA");
    rec.addAltCall("A");
    assertNull(annotation.getValue(rec, 0));
  }

  public void testHeader() {
    final VariantAllelicFractionAnnotation annotation = new VariantAllelicFractionAnnotation();
    final String s = annotation.checkHeader(new VcfHeader());
    assertEquals("Derived annotation VAF missing required fields in VCF header (FORMAT fields: AD or ADE)", s);
  }
}
