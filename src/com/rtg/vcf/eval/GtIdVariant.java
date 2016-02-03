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

import java.util.Objects;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * A Variant that offers orientations where the allele IDs correspond to original GT allele indexing.
 */
@TestClass("com.rtg.vcf.eval.VariantFactoryTest")
public class GtIdVariant extends Variant {

  private final int mAlleleA; // First allele in GT
  private final int mAlleleB; // Second allele in GT

  GtIdVariant(int id, String seq, int start, int end, Allele[] alleles,
              int alleleA, int alleleB, boolean phased) {
    super(id, seq, start, end, alleles, phased);
    mAlleleA = alleleA;
    mAlleleB = alleleB;
  }

  /** @return the allele ID of the first allele in the GT */
  public int alleleA() {
    return mAlleleA;
  }

  /** @return the allele ID of the second allele in the GT */
  public int alleleB() {
    return mAlleleB;
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
    return Objects.equals(mAlleleA, that.mAlleleA) && Objects.equals(mAlleleB, that.mAlleleB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), mAlleleA, mAlleleB);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    sb.append(alleleStr(mAlleleA));
    if (mAlleleB != mAlleleA) {
      sb.append(":");
      sb.append(alleleStr(mAlleleB));
    }
    sb.append(")");
    return sb.toString();
  }
}
