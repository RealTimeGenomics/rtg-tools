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
import java.util.Objects;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Utils;

/**
 * A Variant that offers orientations where the allele IDs correspond to original GT allele indexing.
 */
@TestClass("com.rtg.vcf.eval.VariantFactoryTest")
public class GtIdVariant extends Variant {

  private final int[] mAlleleIds;

  GtIdVariant(int id, String seq, Allele[] alleles,
              boolean phased, int[] gtArr) {
    super(id, seq, alleles, phased);
    mAlleleIds = gtArr;
  }

  /** @return the ploidy of the GT */
  public int ploidy() {
    return mAlleleIds.length;
  }

  /** @return the allele IDs assigned to each haplotype */
  public int[] alleleIds() {
    return mAlleleIds;
  }

  /**
   * @param hap index of the haplotype
   * @return the allele ID of the specified haplotype in the GT
   */
  public int alleleId(int hap) {
    return mAlleleIds[hap];
  }

  /** @return the allele ID of the first allele in the GT */
  public int alleleA() {
    return alleleId(0);
  }

  /** @return the allele ID of the second allele in the GT */
  public int alleleB() {
    return alleleId(1);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final GtIdVariant that = (GtIdVariant) o;
    return Arrays.equals(mAlleleIds, that.mAlleleIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), Utils.pairHashContinuous(mAlleleIds));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    for (int i = 0; i < mAlleleIds.length; i++) {
      if (i > 0) {
        sb.append(":");
      }
      sb.append(alleleStr(mAlleleIds[i]));
    }
    sb.append(")");
    return sb.toString();
  }
}
