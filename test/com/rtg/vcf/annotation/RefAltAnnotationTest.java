/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import com.rtg.util.TestUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class RefAltAnnotationTest extends TestCase {

  public void testName() {
    final RefAltAnnotation ann = new RefAltAnnotation();
    assertEquals("RA", ann.getName());
    TestUtils.containsAll(ann.getDescription(), "Reference-alternate", "RR", "AB");
    assertEquals(MetaType.STRING, ann.getField().getType());
  }

  public void test() {
    final RefAltAnnotation ann = new RefAltAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(3);
    rec.addAltCall("G");
    rec.addFormatAndSample("GT", "0/1");
    rec.addFormatAndSample("GT", ".");
    rec.addFormatAndSample("GT", "1/1");
    assertEquals("RA", ann.getValue(rec, 0));
    assertNull(ann.getValue(rec, 1));
    assertEquals("AA", ann.getValue(rec, 2));
    assertNull(ann.getValue(rec, 3));
    assertEquals("Derived annotation RA missing required fields in VCF header (FORMAT fields: GT)", ann.checkHeader(new VcfHeader()));
  }

  public void testMore() {
    assertEquals("RR", RefAltAnnotation.getCode(-1, 0));
    assertEquals("RR", RefAltAnnotation.getCode(0, 0));

    assertEquals("RA", RefAltAnnotation.getCode(1, 0));
    assertEquals("RA", RefAltAnnotation.getCode(0, 2));

    assertEquals("AA", RefAltAnnotation.getCode(-1, 2));
    assertEquals("AA", RefAltAnnotation.getCode(2, 2));

    assertEquals("AB", RefAltAnnotation.getCode(2, 3));
  }
}
