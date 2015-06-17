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
public class NumberOfAltAllelesAnnotationTest extends TestCase {

  public void testName() {
    final NumberOfAltAllelesAnnotation ann = new NumberOfAltAllelesAnnotation();
    assertEquals("NAA", ann.getName());
    assertEquals("Number of alternative alleles", ann.getDescription());
    assertEquals(AnnotationDataType.INTEGER, ann.getType());
  }

  public void test() {
    final NumberOfAltAllelesAnnotation ann = new NumberOfAltAllelesAnnotation();
    VcfRecord rec = new VcfRecord();
    rec.setRefCall("A");
    rec.addAltCall(".");
    assertEquals(0, ann.getValue(rec, 0).intValue());
    rec.setRefCall("A");
    rec.addAltCall("T");
    assertEquals(1, ann.getValue(rec, -2).intValue());
    rec = new VcfRecord();
    rec.setRefCall("A");
    rec.addAltCall("AAAA");
    assertEquals(1, ann.getValue(rec, 1).intValue());
    rec = new VcfRecord();
    rec.setRefCall("AA");
    rec.addAltCall("AAAA");
    rec.addAltCall("AAAAA");
    rec.addAltCall("A");
    assertEquals(3, ann.getValue(rec, 1).intValue());
    rec = new VcfRecord();
    rec.setRefCall(".");
    rec.addAltCall("AAAA");
    rec.addAltCall("AAAAA");
    rec.addAltCall("A");
    assertEquals(3, ann.getValue(rec, 0).intValue());
    assertNull(ann.checkHeader(null));
  }

}
