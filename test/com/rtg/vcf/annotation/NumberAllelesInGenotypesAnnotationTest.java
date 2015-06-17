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
public class NumberAllelesInGenotypesAnnotationTest extends TestCase {

  public void testName() {
    final NumberAllelesInGenotypesAnnotation ann = new NumberAllelesInGenotypesAnnotation();
    assertEquals("AN", ann.getName());
    assertEquals("Total number of alleles in called genotypes", ann.getDescription());
    assertEquals(AnnotationDataType.INTEGER, ann.getType());
  }

  public void test() {
    final NumberAllelesInGenotypesAnnotation ann = new NumberAllelesInGenotypesAnnotation();
    VcfRecord record = new VcfRecord();
    record.setNumberOfSamples(3);
    record.setRefCall("G");
    record.addAltCall("A");
    record.addAltCall("C");
    assertEquals(null, ann.getValue(record, -1));

    record.addFormatAndSample("GT", "1/0");
    record.addFormatAndSample("GT", ".");
    record.addFormatAndSample("GT", "2");
    assertEquals(3, ((Integer) ann.getValue(record, -1)).intValue());
    assertEquals("Derived annotation AN missing required fields in VCF header (FORMAT fields: GT)", ann.checkHeader(null));
  }

}
