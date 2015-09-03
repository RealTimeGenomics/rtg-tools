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
 */
public class ZygosityAnnotationTest extends TestCase {

  public void testName() {
    final ZygosityAnnotation zyAnn = new ZygosityAnnotation();
    assertEquals("ZY", zyAnn.getName());
    assertEquals("Zygosity of sample. 'e'=>heterozygous, 'o'=>homozygous", zyAnn.getDescription());
    assertEquals(AnnotationDataType.STRING, zyAnn.getType());
  }

  public void test() {
    final ZygosityAnnotation zyAnn = new ZygosityAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(3);
    rec.addAltCall("G");
    rec.addFormatAndSample("GT", "0/1");
    rec.addFormatAndSample("GT", ".");
    rec.addFormatAndSample("GT", "1/1");
    assertEquals("e", zyAnn.getValue(rec, 0));
    assertNull(zyAnn.getValue(rec, 1));
    assertEquals("o", zyAnn.getValue(rec, 2));
    assertNull(zyAnn.getValue(rec, 3));
    assertEquals("Derived annotation ZY missing required fields in VCF header (FORMAT fields: GT)", zyAnn.checkHeader(null));
  }
}
