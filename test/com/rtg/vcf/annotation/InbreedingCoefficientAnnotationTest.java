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
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 * Produces an info field containing the inbreeding coefficient for a set of samples.
 * @see <a href="http://en.wikipedia.org/wiki/Hardy-Weinberg_principle#Inbreeding_coefficient">Inbreeding coefficient</a>
 */
public class InbreedingCoefficientAnnotationTest extends TestCase {

  public void testName() {
    final InbreedingCoefficientAnnotation icAnn = new InbreedingCoefficientAnnotation();
    assertEquals("IC", icAnn.getName());
    assertEquals("Inbreeding Coefficient", icAnn.getDescription());
    assertEquals(MetaType.FLOAT, icAnn.getField().getType());
    assertEquals("Derived annotation IC missing required fields in VCF header (FORMAT fields: GT)", icAnn.checkHeader(new VcfHeader()));
  }

  public void testHaploidNoCalculation() {
    final InbreedingCoefficientAnnotation icAnn = new InbreedingCoefficientAnnotation();
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("C");
    rec.setNumberOfSamples(3);
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, ".");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1");
    assertNull(icAnn.getValue(rec, 0));
  }

  public void testCoefficientCalculation() {
    final InbreedingCoefficientAnnotation icAnn = new InbreedingCoefficientAnnotation();
    VcfRecord rec = makeTwoAlleleRecord(1469, 138, 5);
    assertEquals(0.023, (Double) icAnn.getValue(rec, 1), 0.001);
    rec = makeTwoAlleleRecord(0, 11, 0);
    assertEquals(-1.000, (Double) icAnn.getValue(rec, 23), 0.001);
    rec = makeTwoAlleleRecord(1, 0, 1);
    assertEquals(1.000, (Double) icAnn.getValue(rec, 0), 0.001);
    rec = makeTwoAlleleRecord(5, 11, 5);
    assertEquals(-0.048, (Double) icAnn.getValue(rec, -1), 0.001);
    rec = makeTwoAlleleRecord(1, 1, 1);
    assertEquals(0.333, (Double) icAnn.getValue(rec, 0), 0.001);
    rec = makeTwoAlleleRecord(0, 0, 10);
    assertNull(icAnn.getValue(rec, 0));
    rec = makeTwoAlleleRecord(10, 0, 0);
    assertNull(icAnn.getValue(rec, 0));
    rec = makeThreeAlleleRecord(1208, 222, 1146, 10, 110, 288);
    assertEquals(0.010, (Double) icAnn.getValue(rec, 0), 0.001);
    rec = makeThreeAlleleRecord(0, 0, 0, 1469, 138, 5);
    assertEquals(0.023, (Double) icAnn.getValue(rec, 0), 0.001);
    rec = makeOneAlleleRecord(11);
    assertNull(icAnn.getValue(rec, 22));
  }

  protected static VcfRecord makeOneAlleleRecord(int aa) {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(aa);
    for (int i = 0; i < aa / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    }
    for (int i = aa / 2; i < aa; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "./.");
    }
    return rec;
  }

  protected static VcfRecord makeTwoAlleleRecord(int aa, int ac, int cc) {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("C");
    rec.setNumberOfSamples(aa + ac + cc);
    for (int i = 0; i < aa / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    }
    for (int i = aa / 2; i < aa; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "./.");
    }
    for (int i = 0; i < ac / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    }
    for (int i = ac / 2; i < ac; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1/0");
    }
    for (int i = 0; i < cc; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1/1");
    }
    return rec;
  }

  protected static VcfRecord makeThreeAlleleRecord(int aa, int ac, int ag, int cc, int cg, int gg) {
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("C");
    rec.addAltCall("G");
    rec.setNumberOfSamples(aa + ac + ag + cc + cg + gg);
    for (int i = 0; i < aa; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    }
    for (int i = 0; i < ac / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    }
    for (int i = ac / 2; i < ac; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1/0");
    }
    for (int i = 0; i < ag / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/2");
    }
    for (int i = ag / 2; i < ag; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "2/0");
    }
    for (int i = 0; i < cc; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1/1");
    }
    for (int i = 0; i < cg / 2; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1/2");
    }
    for (int i = cg / 2; i < cg; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "2/1");
    }
    for (int i = 0; i < gg; ++i) {
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "2/2");
    }
    return rec;
  }
}
