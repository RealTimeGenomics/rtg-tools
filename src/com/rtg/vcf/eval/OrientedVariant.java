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

import java.util.Comparator;

import com.rtg.util.Utils;
import com.rtg.util.intervals.SequenceNameLocus;

/**
 * Reference to a variant that has a defined phasing with respect to a haplotype.
 */
public class OrientedVariant implements Comparable<OrientedVariant>, SequenceNameLocus {

  static final Comparator<OrientedVariant> ID_COMPARATOR = new Comparator<OrientedVariant>() {
    @Override
    public int compare(OrientedVariant o1, OrientedVariant o2) {
      return Integer.compare(o1.variant().getId(), o2.variant().getId());
    }
  };

  private final Variant mVariant;

  private final boolean mHeterozygous;
  private final boolean mIsAlleleA;
  private final int mAlleleId;
  private final int mOtherAlleleId;

  private double mWeight;

  /**
   * Create a homozygous variant
   * @param variant the variant
   * @param alleleId the allele selected for the A haplotype
   */
  public OrientedVariant(Variant variant, int alleleId) {
    this(variant, true, alleleId, alleleId);
  }

  /**
   * Create a variant
   * @param variant the variant
   * @param isAlleleA are we taking the A allele from the original GT
   * @param alleleId the allele selected for the A haplotype
   * @param otherAlleleId the allele selected for the B haplotype (for a haploid comparison
   * this should be the same as the primary allele id)
   */
  public OrientedVariant(Variant variant, boolean isAlleleA, int alleleId, int otherAlleleId) {
    mVariant = variant;
    mAlleleId = alleleId;
    mHeterozygous = otherAlleleId != alleleId;
    mOtherAlleleId = otherAlleleId;
    mIsAlleleA = isAlleleA;
    //assert mVariant != null;
    //assert nt(mAlleleId) != null;
  }

  // Get this variant oriented on the other haplotype.
  OrientedVariant other() {
    return isHeterozygous() ? new OrientedVariant(mVariant, !mIsAlleleA, mOtherAlleleId, mAlleleId) : this;
  }

  /**
   * @return the parent variant.
   */
  Variant variant() {
    return mVariant;
  }

  @Override
  public int compareTo(OrientedVariant that) {
    final int varPos = Integer.compare(this.getStart(), that.getStart());
    if (varPos != 0) {
      return varPos;
    }
    final int id = Boolean.compare(mIsAlleleA, that.mIsAlleleA);
    if (id != 0) {
      return id;
    }
    return Integer.compare(mAlleleId, that.mAlleleId);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(Boolean.valueOf(mIsAlleleA).hashCode(), mAlleleId, mVariant.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
      if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }
    return this.compareTo((OrientedVariant) obj) == 0;
  }

  @Override
  public String toString() {
    return mVariant.toString() + (isAlleleA() ? "+" : "-");
  }

  /**
   * @return the id of the allele this variant is oriented on
   */
  public int alleleId() {
    return mAlleleId;
  }

  /**
   * @return true if this is oriented on the A haplotype
   */
  public boolean isAlleleA() {
    return mIsAlleleA;
  }

  /**
   * The allele of the oriented genotype.
   * @return the allele (may be null (skip) or zero length)
   */
  public byte[] nt() {
    return mVariant.nt(mAlleleId);
  }

  /**
   * @return true if the genotype is heterozygous (i.e. diploid with different alleles)
   */
  public boolean isHeterozygous() {
    return mHeterozygous;
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

  /**
   * @param weight  set the weight
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

}
