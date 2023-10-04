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
import java.util.List;

import com.rtg.mode.DnaUtils;
import com.rtg.util.ByteUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.VcfUtils;


/**
 * Holds information about a single variant that has not yet been oriented in a haplotype.
 * A Variant can be asked for alleles using original GT-style allele IDs, including -1 for missing value.
 */
public class Variant implements Comparable<Variant>, VariantId {

  static final SequenceNameLocusComparator NATURAL_COMPARATOR = new SequenceNameLocusComparator();
  static final Comparator<VariantId> ID_COMPARATOR = new Comparator<VariantId>() {
    @Override
    public int compare(VariantId o1, VariantId o2) {
      return Integer.compare(o1.getId(), o2.getId());
    }
  };

  private SequenceNameLocus mLocus;
  private final int mId;
  private final Allele[] mAlleles;
  private final boolean mPhased;
  private byte mStatus = 0;

  /**
   * Construct the variant
   * @param id the ID of the variant when read from the original input
   * @param seq chromosome name
   * @param alleles array of alleles where each entry corresponds to the allele for GT ID + 1
   * @param phased true if the variant call was phased
   */
  public Variant(int id, String seq, Allele[] alleles, boolean phased) {
    this(id, seq, Allele.getAlleleBounds(alleles), alleles, phased);
  }

  /**
   * Construct the variant
   * @param id the ID of the variant when read from the original input
   * @param seq chromosome name
   * @param bounds bounds of the alleles
   * @param alleles array of alleles where each entry corresponds to the allele for GT ID + 1
   * @param phased true if the variant call was phased
   */
  private Variant(int id, String seq, Range bounds, Allele[] alleles, boolean phased) {
    //super(seq, bounds.getStart(), bounds.getEnd());
    mLocus = new SequenceNameLocusSimple(seq, bounds.getStart(), bounds.getEnd());
    mId = id;
    mPhased = phased;
    mAlleles = alleles;
  }

  void trimAlleles() {
    trimAlleles(true);
  }

  void trimAlleles(boolean leftFirst) {
    // Element 0 is missing value token
    final byte[] ref = mAlleles[1].nt();
    mAlleles[1] = null;
    for (int i = 2; i < mAlleles.length; i++) {
      final Allele a = mAlleles[i];
      if (a != null && !a.unknown()) {
        final byte[] alt = a.nt();
        final int stripLeading;
        final int stripTrailing;
        if (leftFirst) {
          stripLeading = ByteUtils.longestPrefix(0, ref, alt);
          stripTrailing = ByteUtils.longestSuffix(stripLeading, ref, alt);
        } else {
          stripTrailing = ByteUtils.longestSuffix(0, ref, alt);
          stripLeading = ByteUtils.longestPrefix(stripTrailing, ref, alt);
        }
        if (stripLeading > 0 || stripTrailing > 0) {
          mAlleles[i] = new Allele(a.getSequenceName(), a.getStart() + stripLeading, a.getEnd() - stripTrailing,
            ByteUtils.clip(alt, stripLeading, stripTrailing));
        }
      }
    }
    final Range newBounds = Allele.getAlleleBounds(mAlleles);
    mLocus = new SequenceNameLocusSimple(mLocus.getSequenceName(), newBounds.getStart(), newBounds.getEnd());
  }

  static void trimAlleles(List<Variant> vars) {
    int firstOverlap = 0;
    int lastOverlap = 0;
    for (int v = 0; v < vars.size(); v++) {
      final Variant current = vars.get(v);

      while (lastOverlap < vars.size() && (lastOverlap <= v || current.overlaps(vars.get(lastOverlap)))) {
        lastOverlap++;
      }
      while (firstOverlap < v && !current.overlaps(vars.get(firstOverlap))) {
        firstOverlap++;
      }
      if (firstOverlap == v && lastOverlap == v + 1) { // No other variant overlaps with us
        current.trimAlleles();
        continue;
      }

      // Determine whether this variant has flexibility in the trimming
      int stripLeading = 0;
      int stripTrailing = 0;
      final byte[] ref = current.mAlleles[1].nt();
      for (int i = 2; i < current.mAlleles.length; i++) {
        final Allele a = current.mAlleles[i];
        if (a != null && !a.unknown()) {
          final byte[] alt = a.nt();
          stripLeading = Math.max(stripLeading, ByteUtils.longestPrefix(0, ref, alt));
          stripTrailing = Math.max(stripTrailing, ByteUtils.longestSuffix(0, ref, alt));
        }
      }
      if (stripLeading == 0 || stripTrailing == 0) { // No ability to choose which side
        current.trimAlleles(); // Still need to call when both are 0 in order to trim the ref allele
        continue;
      }

      Diagnostic.developerLog("Overlap between " + current + " (trimmable " + stripLeading + "," + stripTrailing + ") and " + (lastOverlap - firstOverlap - 1) + " variants. At " + v + ", [" + firstOverlap + "," + lastOverlap + ") " + vars.size());

      // Simple heuristic, prefer trimming on the side that resolves the most overlaps.
      int lCount = 0;
      int rCount = 0;
      int overlapped = 0;
      for (int i = firstOverlap; i < lastOverlap; i++) {
        if (i == v) {
          continue;
        }
        final int leftOverlap = vars.get(i).getEnd() - current.getStart();
        final int rightOverlap = current.getEnd() - vars.get(i).getStart();
        if (leftOverlap <= 0 && rightOverlap <= 0) {
          Diagnostic.developerLog("No overlap between " + current + " and " + vars.get(i));
          continue;
        }
        overlapped++;
        if (leftOverlap > 0 && leftOverlap <= stripLeading) {
          Diagnostic.developerLog("Overlap between " + current + " and " + vars.get(i) + " can be avoided by left trim");
          lCount++;
        }
        if (rightOverlap > 0 && rightOverlap < stripTrailing) {
          Diagnostic.developerLog("Overlap between " + current + " and " + vars.get(i) + " can be avoided by right trim");
          rCount++;
        }
      }
      current.trimAlleles(lCount > rCount);
      final int remaining = overlapped - Math.max(lCount, rCount);
      if (remaining > 0) {
        Diagnostic.developerLog("After overlap trimming " + current + ", " + remaining + " remains (" + Math.max(lCount, rCount) + " resolved)");
      }
    }
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
  public void setStatus(byte status) {
    mStatus |= status;
  }

  @Override
  public boolean hasStatus(byte status) {
    return (mStatus & status) != 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    for (int i = 0; i < numAlleles(); ++i) {
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
    return pos + (a.unknown() ? "?" : DnaUtils.bytesToSequenceIncCG(a.nt()));
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

  @Override
  public String getSequenceName() {
    return mLocus.getSequenceName();
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    return mLocus.overlaps(other);
  }

  @Override
  public boolean contains(String sequence, int pos) {
    return mLocus.contains(sequence, pos);
  }

  @Override
  public int getStart() {
    return mLocus.getStart();
  }

  @Override
  public int getEnd() {
    return mLocus.getEnd();
  }

  @Override
  public int getLength() {
    return mLocus.getLength();
  }
}
