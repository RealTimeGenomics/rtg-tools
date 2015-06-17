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

import com.rtg.util.Utils;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;
import com.rtg.util.intervals.SequenceNameLocus;

/**
 * Reference to a variant that selects one side of heterozygous cases and
 * also records a position in the variant chosen.
 */
public class OrientedVariant extends IntegralAbstract implements Comparable<OrientedVariant>, Variant {

  private final Variant mVariant;

  private final boolean mIsAlleleA;

  private double mWeight;

  /**
   * @param variant the variant
   * @param isAlleleA are we taking the A allele
   */
  public OrientedVariant(Variant variant, boolean isAlleleA) {
    super();
    mVariant = variant;
    mIsAlleleA = isAlleleA;
  }

  /**
   * @return the variant.
   */
  Variant variant() {
    return mVariant;
  }


  @Override
  public int compareTo(OrientedVariant that) {
    final int varPos = this.getStart() - that.getStart();
    if (varPos != 0) {
      return varPos;
    }

    if (this.mIsAlleleA == that.mIsAlleleA) {
      return 0;
    }
    return mIsAlleleA ? +1 : -1;
  }

  @Override
  public int hashCode() {
    return Utils.hash(new Object[] {
        mIsAlleleA
        , mVariant
    });
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
  public void toString(StringBuilder sb) {
    sb.append(mVariant.toString());
    sb.append(mIsAlleleA ? "+" : "-");
  }

  @Override
  public boolean integrity() {
    Exam.assertNotNull(mVariant);
    Exam.assertNotNull(mVariant.nt(mIsAlleleA));
    return true;
  }

  /**
   * @return true if this is oriented on the A allele
   */
  public boolean isAlleleA() {
    return mIsAlleleA;
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
  public byte[] nt(boolean alleleA) {
    return mVariant.nt(alleleA);
  }

  @Override
  public byte[] ntAlleleA() {
    return mVariant.ntAlleleA();
  }

  @Override
  public byte[] ntAlleleB() {
    return mVariant.ntAlleleB();
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

  @Override
  public boolean isPhased() {
    return mVariant.isPhased();
  }

  @Override
  public String getSequenceName() {
    return mVariant.getSequenceName();
  }
}
