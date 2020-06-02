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

package com.rtg.vcf.eval;

import java.util.Arrays;

import com.rtg.util.Utils;
import com.rtg.util.intervals.SequenceNameLocus;

/**
 * Represents a variant that has a defined phasing with respect to the haplotypes.
 */
public class OrientedVariant implements Comparable<OrientedVariant>, SequenceNameLocus, VariantId {

  private final Variant mVariant;

  private final int[] mAlleleIds;
  private final boolean mIsOriginal;
  private final boolean mHomozygous;

  private double mWeight;

  /**
   * Create a diploid homozygous variant
   * @param variant the variant
   * @param alleleId the allele selected
   */
  public OrientedVariant(Variant variant, int alleleId) {
    this(variant, false, alleleId);
  }

  /**
   * Create a variant
   * @param variant the variant
   * @param isOriginal are the alleles in original GT ordering
   * @param alleleIds the allele ids selected for each haplotype
   */
  public OrientedVariant(Variant variant, boolean isOriginal, int... alleleIds) {
    assert alleleIds != null;
    assert alleleIds.length > 0;
    mVariant = variant;
    mIsOriginal = isOriginal;
    mAlleleIds = alleleIds;
    mHomozygous = isAllEqual(alleleIds);
    final Allele a0 = mVariant.allele(mAlleleIds[0]);
    assert !mHomozygous || a0 == null || a0.getLength() > 0 || a0.nt().length > 0
      : "Homozygous no-op variants should have been ignored during loading";
  }

  static boolean isAllEqual(int[] a) {
    for (int i = 1; i < a.length; i++) {
      if (a[0] != a[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return the parent variant.
   */
  Variant variant() {
    return mVariant;
  }

  @Override
  public int compareTo(OrientedVariant that) {
    int id = Integer.compare(this.variant().getId(), that.variant().getId());
    if (id != 0) {
      return id;
    }
    id = Boolean.compare(that.mIsOriginal, this.mIsOriginal);
    if (id != 0) {
      return id;
    }
    assert mAlleleIds.length == that.mAlleleIds.length;
    for (int i = 0; i < mAlleleIds.length; i++) {
      id = Integer.compare(mAlleleIds[i], that.mAlleleIds[i]);
      if (id != 0) {
        return id;
      }
    }
    return 0;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mVariant.hashCode(), Utils.pairHashContinuous(mAlleleIds));
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OrientedVariant && compareTo((OrientedVariant) obj) == 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (getSequenceName().length() > 0) {
      sb.append(getSequenceName()).append(":");
    }
    sb.append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    final int firstAllele = Math.min(0, Arrays.stream(mAlleleIds).min().getAsInt());
    for (int i = firstAllele; i < mVariant.numAlleles(); ++i) {
      if (i > firstAllele) {
        sb.append(":");
      }
      sb.append(mVariant.alleleStr(i));
      sb.append(haplotypeIndicator(i));
    }
    sb.append(")");
    return sb.toString();
  }

  // Add allele haplotype usage indicator
  // Haploid: empty - allele unused, x - selected
  // Diploid: empty - allele unused, ^ - selected for 0, v - selected for 1, x - selected for both (homozygous)
  // >Diploid: empty - allele unused, else "_" plus concatenation of haplotype ids using the allele
  private String haplotypeIndicator(int a) {
    switch (mAlleleIds.length) {
      case 1:
        return mAlleleIds[0] == a ? "x" : "";
      case 2:
        final int a0 = mAlleleIds[0];
        final int a1 = mAlleleIds[1];
        return (a == a0)
          ? a == a1 ? "x" : "^"
          : a == a1 ? "v" : "";
      default:
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mAlleleIds.length; i++) {
          if (mAlleleIds[i] == a) {
            sb.append(i);
          }
        }
        return sb.length() > 0 ? "_" + sb : "";
    }
  }

  /**
   * @return the id of the allele this variant is oriented on
   */
  public int alleleId() {
    return mAlleleIds[0];
  }

  /**
   * @return the id of the allele for each haplotype
   */
  public int[] alleleIds() {
    return mAlleleIds;
  }

  /**
   * @return true if this is oriented on the A GT haplotype
   */
  public boolean isOriginal() {
    return mIsOriginal;
  }

  /**
   * The allele selected for a particular haplotype
   * @param hap the haplotype of interest
   * @return the allele (may be null (skip) or zero length)
   */
  public Allele allele(int hap) {
    return mVariant.allele(mAlleleIds[hap]);
  }

  /**
   * @return true if the genotype is heterozygous (i.e. diploid with different alleles)
   */
  public boolean isHeterozygous() {
    return !mHomozygous;
  }

  @Override
  public String getSequenceName() {
    return mVariant.getSequenceName();
  }

  @Override
  public int getStart() {
    return mVariant.getStart();
  }

  @Override
  public int getEnd() {
    return mVariant.getEnd();
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    return mVariant.overlaps(other);
  }

  @Override
  public boolean contains(String sequence, int pos) {
    return mVariant.contains(sequence, pos);
  }

  @Override
  public int getLength() {
    return mVariant.getLength();
  }

  @Override
  public void setStatus(byte status) {
    mVariant.setStatus(status);
  }

  @Override
  public boolean hasStatus(byte status) {
    return mVariant.hasStatus(status);
  }

  /**
   * @param weight set the weight
   */
  public void setWeight(double weight) {
    mWeight = weight;
  }

  /**
   * @return calculated weight
   */
  public double getWeight() {
    return mWeight;
  }

  @Override
  public int getId() {
    return mVariant.getId();
  }
}
