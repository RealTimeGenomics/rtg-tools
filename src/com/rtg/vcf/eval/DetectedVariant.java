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

import java.util.EnumSet;

import com.rtg.mode.DnaUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;


/**
 * Holds information about a single variant that has not yet been oriented in a haplotype
 */
public class DetectedVariant implements Comparable<DetectedVariant>, Variant {

  static final SequenceNameLocusComparator NATURAL_COMPARATOR = new SequenceNameLocusComparator();

  private final String mSequenceName;
  private final int mStart;
  private final int mEnd;
  private final byte[][] mPrediction;
  private final boolean mPhased;
  private final boolean mSquashPloidy;
  private final double mSortValue;
  private final EnumSet<RocFilter> mFilters = EnumSet.noneOf(RocFilter.class);

  /**
   * Construct a Detected Variant by inspecting a <code>VcfRecord</code> object. A string is supplied as the output representation of this object
   * @param rec VCF record to process
   * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
   * @param extractor ROC value extractor implementation to use
   * @param squashPloidy if true, convert heterozygous calls to homozygous alternative.
   */
  public DetectedVariant(VcfRecord rec, int sampleNo, RocSortValueExtractor extractor, boolean squashPloidy) {
    mSequenceName = new String(rec.getSequenceName().toCharArray()); // Prevent entire VCF record string from being kept
    mSortValue = extractor.getSortValue(rec, sampleNo);
    final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
    mStart = rec.getStart() + (hasPreviousNt ? 1 : 0);
    mEnd = rec.getEnd();
    mSquashPloidy = squashPloidy;

    final String gt = rec.getFormatAndSample().get(VcfUtils.FORMAT_GENOTYPE).get(sampleNo);
    mPhased = gt.contains("" + VcfUtils.PHASED_SEPARATOR);

    final int[] gtArray = VcfUtils.splitGt(gt);
    assert gtArray.length == 1 || gtArray.length == 2; //can only handle haploid or diploid

    if (mSquashPloidy) {

      int numAlts = 0;
      for (final int gtId : gtArray) {
        if (gtId > 0) {
          numAlts++;
        }
      }
      mPrediction = new byte[numAlts][];
      int j = 0;
      for (final int gtId : gtArray) {
        if (gtId > 0) {
          mPrediction[j++] = DnaUtils.encodeString(getAllele(rec, gtId, hasPreviousNt));
        }
      }

    } else {
      mPrediction = new byte[VcfUtils.isHomozygous(rec, sampleNo) ? 1 : 2][];
      for (int i = 0; i < mPrediction.length; i++) {
        mPrediction[i] = DnaUtils.encodeString(getAllele(rec, gtArray[i], hasPreviousNt));
        //System.err.println(rec.getPosition() + " " + gtArray[i] + " " + getAllele(gtArray[i]));
      }
    }

    for (final RocFilter filter : RocFilter.values()) {
      if (filter.accept(rec, sampleNo)) {
        mFilters.add(filter);
      }
    }
  }

  DetectedVariant(String seq, int start, int end, byte[][] alleles, boolean phased) {
    mSequenceName = seq;
    mStart = start;
    mEnd = end;
    mPhased = phased;
    mSortValue = Double.NaN;
    mPrediction = alleles;
    mSquashPloidy = false;
  }

  private static String getAllele(VcfRecord rec, int allele, boolean hasPreviousNt) {
    if (allele == -1) {
      return "N"; // Missing allele
    }
    if (allele == 0) {
      if (hasPreviousNt) {
        if (rec.getRefCall().length() == 1) {
          return "";
        }
        return rec.getRefCall().substring(1);
      } else {
        return rec.getRefCall();
      }
    } else {
      if (rec.getAltCalls().size() < allele) {
        throw new NoTalkbackSlimException("Invalid allele number in record " + rec.toString());
      } else {
        final String localAllele = rec.getAltCalls().get(allele - 1);
        if (hasPreviousNt) {
          if (localAllele.length() == 1) {
            return "";
          }
          return localAllele.substring(1);
        } else {
          return localAllele;
        }
      }
    }
  }

  @Override
  public int compareTo(final DetectedVariant o2) {
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
    if (!(o2 instanceof DetectedVariant)) {
      return false;
    }
    return NATURAL_COMPARATOR.compare(this, (DetectedVariant) o2) == 0;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(getStart(), getSequenceName().hashCode());
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

  @Override
  public byte[] nt(int alleleId) {
    return alleleId < 0 ? null : mPrediction[alleleId];
  }

  int numAlleles() {
    return mPrediction.length;
  }

  @Override
  public boolean isPhased() {
    return mPhased;
  }

  @Override
  public OrientedVariant[] orientations() {
    if (mSquashPloidy) {
      final OrientedVariant[] pos = new OrientedVariant[mPrediction.length];
      for (int i = 0 ; i < mPrediction.length; i++) {
        pos[i] = new OrientedVariant(this, i);
      }
      return pos;
    } else if (mPrediction.length == 2) {
      // If the variant is heterozygous we need both phases
      return new OrientedVariant[]{
        new OrientedVariant(this, true, 0, 1),
        new OrientedVariant(this, false, 1, 0)
      };
    } else {
      assert mPrediction.length == 1;
      // Homozygous / haploid
      return new OrientedVariant[] {
        new OrientedVariant(this, 0)
      };
    }
  }

  /**
   * Get if the detected variant is accepted by the given <code>RocFilter</code>
   * @param filter the filter to check
   * @return true if the variant is accepted by the given filter, false otherwise
   */
  public boolean filterAccept(RocFilter filter) {
    return mFilters.contains(filter);
  }

  /**
   * Get the sort value for ROC curves
   * @return the sort value
   */
  public double getSortValue() {
    return mSortValue;
  }
}
