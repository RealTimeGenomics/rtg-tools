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

import java.util.ArrayList;
import java.util.Objects;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.IllegalBaseException;
import com.rtg.util.intervals.Range;
import com.rtg.vcf.VcfRecord;

/**
 * A Variant that stores the alleles corresponding to two parent genotypes.
 */
@TestClass("com.rtg.vcf.eval.TrioEvalSynchronizerTest")
public class ParentalVariant extends Variant {

  /**
   * Construct Variants corresponding to the genotypes from two parents.
   */
  static class Factory implements VariantFactory {

    /** Name used to select the parental transmissions factory */
    public static final String NAME = "parents";

    private final int mPSampleNo;
    private final int mMSampleNo;
    private final boolean mTrim;
    private final boolean mExplicitUnknown;

    /**
     * Constructor
     * @param patSample the paternal sample column number (starting from 0)
     * @param matSample the maternal sample column number (starting from 0)
     * @param trimming if true, trim all leading/trailing bases that match REF from alleles
     * @param explicitUnknown if true, treat half call allele as a separate token
     */
    Factory(int patSample, int matSample, boolean trimming, boolean explicitUnknown) {
      mPSampleNo = patSample;
      mMSampleNo = matSample;
      mTrim = trimming;
      mExplicitUnknown = explicitUnknown;
    }

    @Override
    public ParentalVariant variant(VcfRecord rec, int id) throws SkippedVariantException {
      // Check we have a GT that refers to a non-SV variant in at least one allele in at least one parent
      final int[] patGtArr = VariantFactory.getDefinedVariantGt(rec, mPSampleNo);
      final int[] matGtArr = VariantFactory.getDefinedVariantGt(rec, mMSampleNo);
      if (patGtArr == null && matGtArr == null) {
        return null;
      }

      final Allele[] alleles;
      try {
        alleles = Allele.getTrimmedAlleles(rec, null, mTrim, mExplicitUnknown);
      } catch (IllegalBaseException e) {
        throw new SkippedVariantException("Invalid VCF allele. " + e.getMessage());
      }
      final Range bounds = Allele.getAlleleBounds(alleles);

      final int patAlleleA = patGtArr == null ? 0 : patGtArr[0];
      final int patAlleleB = patGtArr == null ? 0 : patGtArr.length == 1 ? patAlleleA : patGtArr[1];
      final int matAlleleA = matGtArr == null ? 0 : matGtArr[0];
      final int matAlleleB = matGtArr == null ? 0 : matGtArr.length == 1 ? matAlleleA : matGtArr[1];

      return new ParentalVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles,
        patAlleleA, patAlleleB, matAlleleA, matAlleleB);
    }
  }

  /**
   * Produces orientations corresponding to possible transmitted haplotypes from two parents to their child.
   * Path finding will require matching both alleles.
   */
  public static final Orientor PARENTAL_TRANSMISSION_DIP = new Orientor() {
    @Override
    public String toString() {
      return "transmitted-dip";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final ParentalVariant pv = (ParentalVariant) variant;
      if (pv.patAlleleA() == pv.patAlleleB()) {
        if (pv.matAlleleA() == pv.matAlleleB()) {
          // Father homozygous, Mother homozygous, one orientation to consider
          return new OrientedVariant[]{
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleA())
          };
        } else {
          // Father homozygous, Mother heterozygous, two orientations to consider
          return new OrientedVariant[]{
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleA()),
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleB())
          };
        }
      } else {
        if (pv.matAlleleA() == pv.matAlleleB()) {
          // Father heterozygous, Mother homozygous, two orientations to consider
          return new OrientedVariant[]{
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleA()),
            new OrientedVariant(variant, false, pv.patAlleleB(), pv.matAlleleA())
          };
        } else {
          // Father heterozygous, Mother heterozygous, four orientations to consider
          return new OrientedVariant[]{
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleA()),
            new OrientedVariant(variant, false, pv.patAlleleB(), pv.matAlleleA()),
            new OrientedVariant(variant, false, pv.patAlleleA(), pv.matAlleleB()),
            new OrientedVariant(variant, false, pv.patAlleleB(), pv.matAlleleB())

          };
        }
      }
    }
  };

  /**
   * Produces orientations corresponding to possible transmitted haploid haplotypes from two parents to their child.
   * Path finding will require matching a non-reference allele from either parent.
   */
  public static final Orientor PARENTAL_TRANSMISSION_HAP = new Orientor() {
    @Override
    public String toString() {
      return "transmitted-hap";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final ParentalVariant pv = (ParentalVariant) variant;
      final ArrayList<OrientedVariant> pos = new ArrayList<>();
      final boolean patHet = pv.patAlleleA() != pv.patAlleleB();
      final boolean matHet = pv.matAlleleA() != pv.matAlleleB();
      if (pv.patAlleleA() > 0) {
        pos.add(new OrientedVariant(variant, pv.patAlleleA()));
      }
      if (pv.patAlleleB() > 0 && patHet) {
        pos.add(new OrientedVariant(variant, pv.patAlleleB()));
      }
      if (pv.matAlleleA() > 0 && pv.matAlleleA() != pv.patAlleleA() && pv.matAlleleA() != pv.patAlleleB()) {
        pos.add(new OrientedVariant(variant, pv.matAlleleA()));
      }
      if (pv.matAlleleB() > 0 && matHet && pv.matAlleleB() != pv.patAlleleA() && pv.matAlleleB() != pv.patAlleleB()) {
        pos.add(new OrientedVariant(variant, pv.matAlleleB()));
      }
      return pos.toArray(new OrientedVariant[pos.size()]);
    }
  };


  private final int mPAlleleA; // First allele in paternal GT
  private final int mPAlleleB; // Second allele in paternal GT

  private final int mMAlleleA; // First allele in maternal GT
  private final int mMAlleleB; // Second allele in maternal GT

  ParentalVariant(int id, String seq, int start, int end, Allele[] alleles,
                  int patAlleleA, int patAlleleB,
                  int matAlleleA, int matAlleleB
                  ) {
    super(id, seq, start, end, alleles, false);
    mPAlleleA = patAlleleA;
    mPAlleleB = patAlleleB;
    mMAlleleA = matAlleleA;
    mMAlleleB = matAlleleB;
  }

  /** @return the allele ID of the first allele in the paternal GT */
  public int patAlleleA() {
    return mPAlleleA;
  }

  /** @return the allele ID of the second allele in the paternal GT */
  public int patAlleleB() {
    return mPAlleleB;
  }

  /** @return the allele ID of the first allele in the maternal GT */
  public int matAlleleA() {
    return mMAlleleA;
  }

  /** @return the allele ID of the second allele in the maternal GT */
  public int matAlleleB() {
    return mMAlleleB;
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
    final ParentalVariant that = (ParentalVariant) o;
    return Objects.equals(mPAlleleA, that.mPAlleleA) && Objects.equals(mPAlleleB, that.mPAlleleB)
      && Objects.equals(mMAlleleA, that.mMAlleleA) && Objects.equals(mMAlleleB, that.mMAlleleB);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), mPAlleleA, mPAlleleB, mMAlleleA, mMAlleleB);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName()).append(":").append(getStart() + 1).append("-").append(getEnd() + 1).append(" (");
    sb.append(alleleStr(mPAlleleA));
    if (mPAlleleB != mPAlleleA) {
      sb.append(":");
      sb.append(alleleStr(mPAlleleB));
    }
    sb.append("x");
    sb.append(alleleStr(mMAlleleA));
    if (mMAlleleB != mMAlleleA) {
      sb.append(":");
      sb.append(alleleStr(mMAlleleB));
    }
    sb.append(")");
    return sb.toString();
  }
}
