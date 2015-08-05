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
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.VcfRecord;


/**
 * Holds information about a single variant that has not yet been oriented in a haplotype
 */
public abstract class Variant implements Comparable<Variant>, VariantId {

  static final SequenceNameLocusComparator NATURAL_COMPARATOR = new SequenceNameLocusComparator();
  static final Comparator<VariantId> ID_COMPARATOR = new Comparator<VariantId>() {
    @Override
    public int compare(VariantId o1, VariantId o2) {
      return Integer.compare(o1.getId(), o2.getId());
    }
  };

  private final int mId;
  private final String mSequenceName;
  private final int mStart;
  private final int mEnd;
  private final byte[][] mAlleles;
  private final boolean mPhased;

  Variant(int id, String seq, int start, int end, byte[][] alleles, boolean phased) {
    mId = id;
    mSequenceName = new String(seq.toCharArray());
    mStart = start;
    mEnd = end;
    mPhased = phased;
    mAlleles = alleles;
  }

  protected static String getAllele(VcfRecord rec, int allele, boolean hasPreviousNt) {
    if (allele == -1) {
      return "N"; // Missing allele
    }
    final String localAllele = rec.getAllele(allele);
    if (hasPreviousNt) {
      if (localAllele.length() == 1) {
        return "";
      }
      return localAllele.substring(1);
    } else {
      return localAllele;
    }
  }

  @Override
  public int compareTo(final Variant o2) {
    return NATURAL_COMPARATOR.compare(this, o2);
  }

  @Override
  public boolean equals(final Object o2) {
    if (this == o2) {
      return true;
    }
    if (o2 == null) {
      return false;
    }
    if (!(o2 instanceof Variant)) {
      return false;
    }
    return NATURAL_COMPARATOR.compare(this, (Variant) o2) == 0;
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
  public String getSequenceName() {
    return mSequenceName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    for (int i = 0; i < numAlleles(); i++) {
      if (i > 0) {
        sb.append(":");
      }
      sb.append(DnaUtils.bytesToSequenceIncCG(nt(i)));
    }
    sb.append(")");
    return sb.toString();
  }

  @Override
  public int getStart() {
    return mStart;
  }

  @Override
  public int getEnd() {
    return mEnd;
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(String sequence, int pos) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLength() {
    return getEnd() - getStart();
  }

  /**
   * One allele of the variant as determined by <code>alleleId</code> parameter.
   * @param alleleId the index of the allele.
   * @return the bases of the allele.  May be null (no substitution) or zero length (deletion)
   */
  public byte[] nt(int alleleId) {
    return alleleId < 0 ? null : mAlleles[alleleId];
  }

  int numAlleles() {
    return mAlleles.length;
  }

  /**
   * @return true if the call has (external) phasing information
   */
  public boolean isPhased() {
    return mPhased;
  }

  /**
   * @return the possible oriented variants for this variant
   */
  public abstract OrientedVariant[] orientations();

}
