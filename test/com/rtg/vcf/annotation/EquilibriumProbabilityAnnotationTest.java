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

import junit.framework.TestCase;

/**
 */
public class EquilibriumProbabilityAnnotationTest extends TestCase {

  public void testName() {
    final EquilibriumProbabilityAnnotation epAnn = new EquilibriumProbabilityAnnotation();
    assertEquals("EP", epAnn.getName());
    assertEquals("Phred scaled probability that site is not in Hardy-Weinberg equilibrium", epAnn.getDescription());
    assertEquals(AnnotationDataType.DOUBLE, epAnn.getType());
    assertEquals("Derived annotation EP missing required fields in VCF header (FORMAT fields: GT)", epAnn.checkHeader(null));
  }

  public void testHaploidNoCalculation() {
    final EquilibriumProbabilityAnnotation epAnn = new EquilibriumProbabilityAnnotation();
    final VcfRecord rec = new VcfRecord();
    rec.setRefCall("A")
        .addAltCall("C")
        .setNumberOfSamples(3)
        .addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0")
        .addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, ".")
        .addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1");
    assertNull(epAnn.getValue(rec, -2));
  }

  public void testCoefficientCalculation() {
    final EquilibriumProbabilityAnnotation epAnn = new EquilibriumProbabilityAnnotation();
    VcfRecord rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(1469, 138, 5);
    assertEquals(0.0554, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(0, 11, 0);
    assertEquals(23.886, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(1, 0, 1);
    assertEquals(4.343, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(5, 11, 5);
    assertEquals(0.103, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(1, 1, 1);
    assertEquals(0.724, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(0, 0, 10);
    assertEquals(0.0, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeTwoAlleleRecord(10, 0, 0);
    assertEquals(0.0, (Double) epAnn.getValue(rec, 0), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeThreeAlleleRecord(1208, 222, 1146, 10, 110, 288);
    assertEquals(0.639, (Double) epAnn.getValue(rec, -1), 0.001);
    rec = InbreedingCoefficientAnnotationTest.makeThreeAlleleRecord(0, 0, 0, 1469, 138, 5);
    assertEquals(0.0554, (Double) epAnn.getValue(rec, 123), 0.001);
  }
}
