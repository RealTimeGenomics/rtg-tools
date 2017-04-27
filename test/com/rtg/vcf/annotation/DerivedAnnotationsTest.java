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

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class DerivedAnnotationsTest extends TestCase {

  public void testEnum() {
    TestUtils.testEnum(DerivedAnnotations.class, "[IC, EP, LAL, QD, NAA, AC, AN, GQD, COC, COF, VAF, ZY, PD, MEANQAD, QA, RA]");
    assertTrue(DerivedAnnotations.IC.getAnnotation() instanceof InbreedingCoefficientAnnotation);
    assertTrue(DerivedAnnotations.EP.getAnnotation() instanceof EquilibriumProbabilityAnnotation);
    assertTrue(DerivedAnnotations.LAL.getAnnotation() instanceof LongestAlleleAnnotation);
    assertTrue(DerivedAnnotations.QD.getAnnotation() instanceof QualOverDepthAnnotation);
    assertTrue(DerivedAnnotations.NAA.getAnnotation() instanceof NumberOfAltAllelesAnnotation);
    assertTrue(DerivedAnnotations.AC.getAnnotation() instanceof AlleleCountInGenotypesAnnotation);
    assertTrue(DerivedAnnotations.AN.getAnnotation() instanceof NumberAllelesInGenotypesAnnotation);
    assertTrue(DerivedAnnotations.GQD.getAnnotation() instanceof GenotypeQualityOverDepthAnnotation);
    assertTrue(DerivedAnnotations.COC.getAnnotation() instanceof ContraryObservationCountAnnotation);
    assertTrue(DerivedAnnotations.COF.getAnnotation() instanceof ContraryObservationFractionAnnotation);
    assertTrue(DerivedAnnotations.ZY.getAnnotation() instanceof ZygosityAnnotation);
    assertTrue(DerivedAnnotations.PD.getAnnotation() instanceof PloidyAnnotation);
    assertTrue(DerivedAnnotations.MEANQAD.getAnnotation() instanceof MeanQualityDifferenceAnnotation);
    assertTrue(DerivedAnnotations.QA.getAnnotation() instanceof AltAlleleQualityAnnotation);
    assertTrue(DerivedAnnotations.RA.getAnnotation() instanceof RefAltAnnotation);
  }

}
