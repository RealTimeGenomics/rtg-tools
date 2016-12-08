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

import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Common code for inbreeding coefficient and Hardy Weinberg equilibrium probability
 * @see <a href="http://en.wikipedia.org/wiki/Hardy-Weinberg_principle#Inbreeding_coefficient">Inbreeding coefficient</a>
 */
@TestClass(value = {"com.rtg.vcf.annotation.InbreedingCoefficientAnnotationTest", "com.rtg.vcf.annotation.EquilibriumProbabilityAnnotationTest"})
public abstract class AbstractInbreedingCoefficientAnnotation extends AbstractDerivedInfoAnnotation {

  /**
   * @param name the name for this annotation
   * @param description the description for this annotation
   */
  public AbstractInbreedingCoefficientAnnotation(String name, String description) {
    super(new InfoField(name, MetaType.FLOAT, VcfNumber.ONE, description));
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final List<String> gtList = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (gtList == null) {
      return null;
    }
    final int numAlleles = record.getAltCalls().size() + 1; //+1 for Reference allele
    final int[] alleleFreqCount = new int[numAlleles];
    int hetCount = 0;
    int total = 0;
    boolean diploid = false;
    for (String aGtList : gtList) {
      final int[] gts = VcfUtils.splitGt(aGtList);
      if (gts.length == 2) {
        diploid = true;
        if (gts[0] != gts[1]) {
          ++hetCount;
        }
        for (int gt : gts) {
          if (gt < 0) {
            gt = 0;
          }
          alleleFreqCount[gt]++;
        }
        ++total;
      }
    }
    if (!diploid) {
      return null; //Undefined for Haploid
    }
    return getValue(total, hetCount, getExpectedHetProb(total, alleleFreqCount));
  }

  protected double getExpectedHetProb(int total, int... haploidAlleleCount) {
    final int numAlleles = haploidAlleleCount.length;
    final double[] alleleFreqs = new double[numAlleles];
    for (int i = 0; i < numAlleles; ++i) {
      alleleFreqs[i] = haploidAlleleCount[i] / (2.0 * total);
    }
    double expectedHetProb = 0;
    for (int i = 0; i < numAlleles; ++i) {
      for (int j = i + 1; j < numAlleles; ++j) {
        expectedHetProb += 2 * alleleFreqs[i] * alleleFreqs[j];
      }
    }
    return expectedHetProb;
  }


  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_GENOTYPE});
  }

  protected abstract Double getValue(int total, int hetCount, double expectedHetProbability);

}
