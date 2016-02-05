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
   * @return the Variant or null if the record was non-variant
   * @throws SkippedVariantException if the variant contained data that could not be converted according to the factory.
   */
  Variant variant(VcfRecord rec, int id) throws SkippedVariantException;

  /**
   * Construct Variants corresponding to the GT of a specified sample, only where the sample
   * is variant and maintaining original allele IDs.
   * Trimming is configurable, by default the only allele trimming done is a single leading
   * padding base, and only if it is shared by all alleles. If trimming is enabled, all common
   * leading/trailing bases that match REF will be removed, causing path finding to be more
   * permissive of reference overlaps.
   */
  class SampleVariants implements VariantFactory {

    private final int mSampleNo;
    private final boolean mTrim;
    private final boolean mExplicitHalfCall;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     * @param trimming if true, trim all leading/trailing bases that match REF from alleles
     * @param explicitHalfCall if true, treat half call allele as a separate token
     */
    public SampleVariants(int sampleNo, boolean trimming, boolean explicitHalfCall) {
      mSampleNo = sampleNo;
      mTrim = trimming;
      mExplicitHalfCall = explicitHalfCall;
    }

    @Override
    public GtIdVariant variant(VcfRecord rec, int id) throws SkippedVariantException {
      // Currently we skip both non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      if (gtArray.length == 0 || gtArray.length > 2) {
        throw new SkippedVariantException("GT value '" + gt + "' is not haploid or diploid.");
      }
      final Allele[] alleles = Allele.getTrimmedAlleles(rec, gtArray, mTrim, mExplicitHalfCall);
      final Range bounds = Allele.getAlleleBounds(alleles);

      final int alleleA = gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1];

      return new GtIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, alleleA, alleleB, VcfUtils.isPhasedGt(gt));
    }
  }

  /**
   * Creates a variants with all ALT alleles declared in the variant record.
   * Used to perform sample genotype matching against the possibilities defined by the full set of declared alleles.
   * Performs allele trimming (could make this optional?).
   */
  class AllAlts implements VariantFactory {

    private final boolean mTrim;
    private final boolean mExplicitHalfCall;

    AllAlts() {
      this(true, false);
    }

    /**
     * Constructor
     * @param trimming if true, trim all leading/trailing bases that match REF from alleles
     * @param explicitHalfCall if true, treat half call allele as a separate token
     */
    public AllAlts(boolean trimming, boolean explicitHalfCall) {
      mTrim = trimming;
      mExplicitHalfCall = explicitHalfCall;
    }

    @Override
    public Variant variant(final VcfRecord rec, final int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // TODO should also ignore SV/symbolic alts, and entirely skip variants where there are no alts remaining.

      final Allele[] alleles = Allele.getTrimmedAlleles(rec, null, mTrim, mExplicitHalfCall);
      final Range bounds = Allele.getAlleleBounds(alleles);

      return new Variant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, false);
    }
  }
}
