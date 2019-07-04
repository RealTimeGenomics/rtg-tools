/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
 * Test the corresponding class.
 */
public class AlleleDepthForwardAnnotationTest extends TestCase {

  public void testName() {
    final AlleleDepthForwardAnnotation annotation = new AlleleDepthForwardAnnotation();
    assertEquals("ADF", annotation.getName());
    assertEquals("Total allelic depths on the forward strand", annotation.getDescription());
    assertEquals(MetaType.INTEGER, annotation.getField().getType());
  }

  public void test() {
    final AlleleDepthForwardAnnotation annotation = new AlleleDepthForwardAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "AAA");
    rec.addAltCall("A");
    assertNull(annotation.getValue(rec, 0));
    rec.addFormatAndSample("ADF2", "7,3");
    assertNull(annotation.getValue(rec, 0));
    rec.addFormatAndSample("ADF1", "6,2");
    // Now has required inputs
    assertEquals("13,5", annotation.getValue(rec, 0).toString());
  }

  public void testHeader() {
    final AlleleDepthForwardAnnotation annotation = new AlleleDepthForwardAnnotation();
    final String s = annotation.checkHeader(new VcfHeader());
    assertEquals("Derived annotation ADF missing required fields in VCF header (FORMAT fields: ADF1 ADF2)", s);
  }
}
