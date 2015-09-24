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

import com.rtg.mode.DnaUtils;
import com.rtg.util.Utils;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.VcfUtils;


/**
 * Holds information about a single variant that has not yet been oriented in a haplotype.
 * A Variant can be asked for alleles using original GT-style allele IDs, including -1 for missing value.
 */
public class Variant extends SequenceNameLocusSimple implements Comparable<Variant>, VariantId {

  static final SequenceNameLocusComparator NATURAL_COMPARATOR = new SequenceNameLocusComparator();
  static final Comparator<VariantId> ID_COMPARATOR = new Comparator<VariantId>() {
    @Override
    public int compare(VariantId o1, VariantId o2) {
      return Integer.compare(o1.getId(), o2.getId());
    }
  };

  private final int mId;
  private final Allele[] mAlleles;
  private final boolean mPhased;

  Variant(int id, String seq, int start, int end) {
    super(seq, start, end);
    mId = id;
    mAlleles = null;
    mPhased = false;
  }

  /**
   * Construct the variant
   * @param id the ID of the variant when read from the original input
   * @param seq chromosome name
   * @param start start position
   * @param end end position
   * @param alleles array of alleles where each entry corresponds to the allele for GT ID + 1
   * @param phased true if the variant call was phased
   */
  public Variant(int id, String seq, int start, int end, Allele[] alleles, boolean phased) {
    super(seq, start, end);
    mId = id;
    mPhased = phased;
    mAlleles = alleles;
  }

  @Override
  public int compareTo(final Variant o2) {
    return NATURAL_COMPARATOR.compare(this, o2);
  }

  @Override
  public boolean equals(final Object o2) {
    return this == o2 || (o2 instanceof Variant && NATURAL_COMPARATOR.compare(this, (Variant) o2) == 0);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(getStart(), getSequenceName().hashCode());
  }

  @Override
  public int getId() {
    return mId;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    for (int i = 0; i < numAlleles(); i++) {
      if (i > 0) {
        sb.append(":");
      }
      sb.append(alleleStr(i));
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * One allele of the variant as determined by <code>alleleId</code> parameter.
   * @param alleleId the index of the allele.
   * @return the bases of the allele.  May be null (no substitution) or zero length (deletion)
   */
  public Allele allele(int alleleId) {
    return alleleId < -1 ? null : mAlleles[alleleId + 1];
  }

  /**
   * The bases of allele of the variant as determined by <code>alleleId</code> parameter.
   * @param alleleId the index of the allele.
   * @return the bases of the allele.  May be null (no substitution) or zero length (deletion)
   */
  public byte[] nt(int alleleId) {
    final Allele a = allele(alleleId);
    return a == null ? null : a.nt();
  }

  /**
   * Gets a human-readable representation of an allele. '.' indicates a no-call allele, '*' indicates
   * an allele that has been removed from the variant (and takes no part in haplotype replay), otherwise the DNA bases.
   * If an individual allele is anchored differently to the enclosing variant, it's genomic position is also listed.
   * @param alleleId the allele id within the variant
   * @return the String
   */
  public String alleleStr(int alleleId) {
    if (alleleId < 0) {
      return VcfUtils.MISSING_FIELD;
    }
    final Allele a = allele(alleleId);
    if (a == null) {
      return "*";
    }
    String pos = "";
    if (a.getStart() != getStart() || a.getEnd() != getEnd()) {
      pos = "<" + (a.getStart() + 1) + "-" + (a.getEnd() + 1) + ">";
    }
    return pos + DnaUtils.bytesToSequenceIncCG(a.nt());
  }

  int numAlleles() {
    return mAlleles.length - 1;
  }

  /**
   * @return true if the call has (external) phasing information
   */
  public boolean isPhased() {
    return mPhased;
  }
}
