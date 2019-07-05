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

import java.util.EnumSet;
import java.util.stream.Collectors;

import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfNumber;

/**
 * Enumeration of the derived annotations.
 */
public enum DerivedAnnotations {

  // INFO annotations

  /** Inbreeding Coefficient */
  IC(new InbreedingCoefficientAnnotation()),
  /** Hardy-Weinberg Equilibrium Probability */
  EP(new EquilibriumProbabilityAnnotation()),
  /** Length of the longest allele */
  LAL(new LongestAlleleAnnotation()),
  /** QUAL / DP */
  QD(new QualOverDepthAnnotation()),
  /** Number of alternative alleles */
  NAA(new NumberOfAltAllelesAnnotation()),
  /**
   * Allele count in genotypes, for each alternative allele
   * Note: Is a multiple value annotation (can not be used for AVR ML)
   */
  AC(new AlleleCountInGenotypesAnnotation()),
  /** Total number of alleles in called genotypes */
  AN(new NumberAllelesInGenotypesAnnotation()),

  // FORMAT annotations

  /** GQ / DP for a single sample */
  GQD(new GenotypeQualityOverDepthAnnotation()),
  /** Allelic fraction of each ALT allele */
  VAF(new VariantAllelicFractionAnnotation()),
  /** Allelic fraction of most frequent ALT allele */
  VAF1(new VariantMinorAllelicFractionAnnotation()),
  /** Zygosity */
  ZY(new ZygosityAnnotation()),
  /** Ploidy */
  PD(new PloidyAnnotation()),
  /** Difference in mean quality between called alleles */
  MEANQAD(new MeanQualityDifferenceAnnotation()),
  /** Difference in mean quality between called alleles */
  QA(new AltAlleleQualityAnnotation()),
  /** Ref-Alt type */
  RA(new RefAltAnnotation()),
  /** Forward allele depth */
  ADF(new AlleleDepthForwardAnnotation()),
  /** Reverse allele depth */
  ADR(new AlleleDepthReverseAnnotation()),
  ;

  private final transient AbstractDerivedAnnotation<?> mAnnotation;

  DerivedAnnotations(AbstractDerivedAnnotation<?> annotation) {
    assert name().equals(annotation.getName());
    mAnnotation = annotation;
  }

  /**
   * Get the annotation associated with this enum value.
   * @return the annotation associated with this enum value.
   */
  public AbstractDerivedAnnotation<?> getAnnotation() {
    return mAnnotation;
  }

  /**
   * Get the set of derived annotations that produce a single value.
   * @return the set of derived annotations that produce a single value.
   */
  public static EnumSet<DerivedAnnotations> singleValueAnnotations() {
    return EnumSet.allOf(DerivedAnnotations.class).stream()
      .filter(a -> a.getAnnotation().getField().getNumber() == VcfNumber.ONE)
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(DerivedAnnotations.class)));
  }

  /**
   * Get the set of derived annotations that produce a single numeric value.
   * @return the set of derived annotations that produce a single numeric value.
   */
  public static EnumSet<DerivedAnnotations> singleValueNumericAnnotations() {
    return singleValueAnnotations().stream()
      .filter(a -> {
        final MetaType aType = a.getAnnotation().getField().getType();
        return aType == MetaType.INTEGER || aType == MetaType.FLOAT;
      })
      .collect(Collectors.toCollection(() -> EnumSet.noneOf(DerivedAnnotations.class)));
  }
}
