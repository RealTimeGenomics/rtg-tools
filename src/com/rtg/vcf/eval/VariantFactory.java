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
import java.util.Arrays;

import com.rtg.launcher.GlobalFlags;
import com.rtg.util.intervals.Range;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Creates variants for evaluation from VCF records.
 */
public interface VariantFactory {

  /**
   * Construct a Variant by inspecting a <code>VcfRecord</code> object.
   * @param rec VCF record to convert to Variant
   * @param id the identifier to assign to this variant
   * @return the Variant or null if the record didn't contain data that could be converted according to the factory.
   */
  Variant variant(VcfRecord rec, int id);


  /**
   * Construct Variants corresponding to the GT of a specified sample.
   * The only allele trimming done is a single leading
   * padding base, and only if it is shared by all alleles.
   * Path finding will require full genotype to match.
   * Keep this for backward compatibility with previous versions of vcfeval.
   */
  class DefaultGt implements VariantFactory {

    static final String NAME = "default";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public DefaultGt(int sampleNo) {
      mSampleNo = sampleNo;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip both non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      assert gtArray.length == 1 || gtArray.length == 2;

      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final Allele[] alleles = new Allele[VcfUtils.isHomozygousAlt(gtArray) ? 1 : 2];
      for (int i = 0; i < alleles.length; i++) {
        final int gtId = gtArray[i];      // May be -1, we treat missing as distinct value
        alleles[i] = Allele.getAllele(rec, gtId, false, hasPreviousNt, true);
      }
      final Range bounds = Allele.getAlleleBounds(alleles);
      return new CompactVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, VcfUtils.isPhasedGt(gt));
    }
  }

  /**
   * Construct Variants corresponding to the GT of a specified sample, maintaining original allele IDs
   * The only allele trimming done is a single leading
   * padding base, and only if it is shared by all alleles.
   * Path finding will require full genotype to match.
   */
  class DefaultGtId implements VariantFactory {

    static final String NAME = "default-id";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public DefaultGtId(int sampleNo) {
      mSampleNo = sampleNo;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip both non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      assert gtArray.length == 1 || gtArray.length == 2;
      final Allele[] alleles = Allele.getTrimmedAlleles(rec, gtArray, false, true);
      final Range bounds = Allele.getAlleleBounds(alleles);
      final int alleleA = gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1];
      return new GtIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, alleleA, alleleB, VcfUtils.isPhasedGt(gt));
    }
  }

  /**
   * Construct Variants corresponding to the GT of a specified sample, maintaining original allele IDs
   * This version performs trimming of all common leading/trailing bases that match REF.
   * Path finding will require full genotype to match, but will be more permissive of reference overlaps.
   */
  class TrimmedGtId implements VariantFactory {

    static final String NAME = "default-trim-id";

    private final int mSampleNo;
    private final boolean mExplicitHalfCall;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public TrimmedGtId(int sampleNo) {
      mSampleNo = sampleNo;
      mExplicitHalfCall = GlobalFlags.isSet(GlobalFlags.VCFEVAL_EXPLICIT_HALF_CALL);
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip both non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      assert gtArray.length == 1 || gtArray.length == 2;
      final Allele[] alleles = Allele.getTrimmedAlleles(rec, gtArray, true, mExplicitHalfCall);
      final Range bounds = Allele.getAlleleBounds(alleles);
      final int alleleA = gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1];
      return new GtIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, alleleA, alleleB, VcfUtils.isPhasedGt(gt));
    }
  }

  /**
   * Creates a haploid oriented variant for each ALT allele referenced by the sample GT.
   * Path finding will match any variants where there are any non-ref allele matches.
   * Keep this for backward compatibility with previous versions of vcfeval.
   */
  class HaploidAltGt implements VariantFactory {

    static final String NAME = "squash";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    HaploidAltGt(int sampleNo) {
      mSampleNo = sampleNo;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }
      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final String gt = rec.getFormatAndSample().get(VcfUtils.FORMAT_GENOTYPE).get(mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      Arrays.sort(gtArray);
      int numAlts = 0;
      int prev = -1;
      for (final int gtId : gtArray) {
        if (gtId > 0 && gtId != prev) {
          numAlts++;
        }
        prev = gtId;
      }
      final Allele[] alleles = new Allele[numAlts];
      int j = 0;
      prev = -1;
      for (final int gtId : gtArray) {
        if (gtId > 0 && gtId != prev) {
          alleles[j++] = Allele.getAllele(rec, gtId, false, hasPreviousNt, true);
        }
        prev = gtId;
      }

      return new CompactVariant(id, rec.getSequenceName(), rec.getStart() + (hasPreviousNt ? 1 : 0), rec.getEnd(), alleles, false) {
        @Override
        public OrientedVariant[] orientations() {
          final OrientedVariant[] pos = new OrientedVariant[numAlleles()];
          for (int i = 0 ; i < numAlleles(); i++) {
            pos[i] = new OrientedVariant(this, i);
          }
          return pos;
        }
      };
    }
  }

  /**
   * Creates a haploid oriented variant for each ALT allele referenced by the sample GT.
   * Path finding will match any variants where there are any non-ref allele matches.
   */
  class HaploidAltTrimmedGtId implements VariantFactory {

    static final String NAME = "squash-trim-id";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    HaploidAltTrimmedGtId(int sampleNo) {
      mSampleNo = sampleNo;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      assert gtArray.length == 1 || gtArray.length == 2;
      final Allele[] alleles = Allele.getTrimmedAlleles(rec, gtArray, true, false);
      final Range bounds = Allele.getAlleleBounds(alleles);
      final int alleleA = gtArray[0] > 0 ? gtArray[0] : gtArray[1];
      final int alleleB = gtArray.length == 1 || gtArray[1] < 1 ? alleleA : gtArray[1];
      return new GtIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, alleleA, alleleB, false) {
        @Override
        public OrientedVariant[] orientations() {
          final OrientedVariant[] pos;
          if (alleleA == alleleB) {
            pos = new OrientedVariant[1];
            pos[0] = new OrientedVariant(this, alleleA);
          } else {
            pos = new OrientedVariant[2];
            pos[0] = new OrientedVariant(this, alleleA);
            pos[1] = new OrientedVariant(this, alleleB);
          }
          return pos;
        }
      };
    }
  }


  /**
   * Creates a haploid oriented variant for every ALT allele declared in the variant record.
   * Used to perform allele matching against the full set of declared alleles rather than those in a sample column.
   */
  class HaploidAlts implements VariantFactory {

    static final String NAME = "hap-alt";

    @Override
    public Variant variant(VcfRecord rec, int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // TODO should also ignore SV/symbolic alts, and entirely skip variants where there are no alts remaining.

      final Allele[] alleles = Allele.getTrimmedAlleles(rec, null, true, false);
      final Range bounds = Allele.getAlleleBounds(alleles);

      return new AlleleIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, false) {
        @Override
        public OrientedVariant[] orientations() {
          final OrientedVariant[] pos = new OrientedVariant[numAlleles() - 1];
          for (int i = 0 ; i < pos.length; i++) {
            pos[i] = new OrientedVariant(this, i + 1);
          }
          return pos;
        }
      };
    }
  }

  /**
   * Creates all possible non-ref diploid variants using any of the ALT alleles declared in the variant record.
   * Used to perform sample genotype matching against the possibilities defined by the full set of declared alleles.
   * Performs allele trimming (could make this optional?).
   * Depending on configuration, includes all combinations of REF and half calls.
   */
  class DiploidAlts implements VariantFactory {

    static final String NAME = "dip-alt";

    private final boolean mExplicitHalfCall;

    DiploidAlts() {
      mExplicitHalfCall = GlobalFlags.isSet(GlobalFlags.VCFEVAL_EXPLICIT_HALF_CALL);
    }

    @Override
    public Variant variant(final VcfRecord rec, final int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // TODO should also ignore SV/symbolic alts, and entirely skip variants where there are no alts remaining.

      final Allele[] alleles = Allele.getTrimmedAlleles(rec, null, true, mExplicitHalfCall);
      final Range bounds = Allele.getAlleleBounds(alleles);

      return new AlleleIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, false) {
        @Override
        public OrientedVariant[] orientations() {
          final ArrayList<OrientedVariant> pos = new ArrayList<>(numAlleles() * numAlleles() - 1);
          for (int i = 1 ; i < numAlleles(); i++) {
            for (int j = -1; j < i; j++) {
              pos.add(new OrientedVariant(this, true, i, j));
              pos.add(new OrientedVariant(this, false, j, i));
              if (j == -1 && !mExplicitHalfCall) {
                j++; // Jump from . to first allele, so skips will match as missing
              }
            }
            pos.add(new OrientedVariant(this, true, i, i));
          }
          return pos.toArray(new OrientedVariant[pos.size()]);
        }
      };
    }
  }
}
