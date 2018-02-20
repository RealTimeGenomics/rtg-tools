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

import static com.rtg.vcf.VcfUtils.FORMAT_GENOTYPE;

import java.util.Arrays;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.mode.IllegalBaseException;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.Range;
import com.rtg.vcf.VariantType;
import com.rtg.vcf.VcfFormatException;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Creates variants for evaluation from VCF records.
 */
public interface VariantFactory {

  /** The special sample name used to denote using ALTs rather than a particular sample */
  String ALT_SAMPLE = "ALT";

  /** Name used to select the sample variants factory */
  String SAMPLE_FACTORY = "sample";

  /** Name used to select the all-alts factory */
  String ALL_FACTORY = "all";

  /**
   * Resolve the name of the variant factory to use, incorporating flag override if present.
   * @param type which variant set this factory is for
   * @param sampleName the user specified sample name, which may be the special value "ALT" to select the all-alts factory.
   * @return the name of the variant factory to use, either <code>SAMPLE_FACTORY</code> or <code>ALL_FACTORY</code>
   */
  static String getFactoryName(VariantSetType type, String sampleName) {
    final String customFactory = GlobalFlags.getStringValue(ToolsGlobalFlags.VCFEVAL_VARIANT_FACTORY);
    if (customFactory.length() > 0) {
      final String[] f = StringUtils.split(customFactory, ',');
      if (type == VariantSetType.BASELINE) {
        return f[0];
      } else {
        return f.length == 1 ? f[0] : f[1];
      }
    } else {
      return ALT_SAMPLE.equals(sampleName) ? ALL_FACTORY : SAMPLE_FACTORY;
    }
  }

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
    private final boolean mExplicitUnknown;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     * @param trimming if true, trim all leading/trailing bases that match REF from alleles
     * @param explicitUnknown if true, treat half call allele as a separate token
     */
    public SampleVariants(int sampleNo, boolean trimming, boolean explicitUnknown) {
      mSampleNo = sampleNo;
      mTrim = trimming;
      mExplicitUnknown = explicitUnknown;
    }

    @Override
    public GtIdVariant variant(VcfRecord rec, int id) throws SkippedVariantException {
      // Currently we skip both non-variant and SV
      // Check we have a GT that refers to a non-SV variant in at least one allele
      if (!hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }

      final String gt = VcfUtils.getValidGtStr(rec, mSampleNo);
      final int[] gtArray = VcfUtils.splitGt(gt);
      if (gtArray.length == 0 || gtArray.length > 2) {
        throw new SkippedVariantException("GT value '" + gt + "' is not haploid or diploid.");
      }
      final Allele[] alleles;
      try {
        alleles = Allele.getTrimmedAlleles(rec, gtArray, mTrim, mExplicitUnknown);
      } catch (IllegalBaseException e) {
        throw new SkippedVariantException("Invalid VCF allele. " + e.getMessage());
      }
      final Range bounds = Allele.getAlleleBounds(alleles);

      final int alleleA = gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1];

      return new GtIdVariant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, alleleA, alleleB, VcfUtils.isPhasedGt(gt));
    }

    static boolean hasDefinedVariantGt(VcfRecord rec, int sampleId) {
      if (sampleId >= rec.getNumberOfSamples()) {
        throw new VcfFormatException("Record did not contain enough samples: " + rec.toString());
      }
      if (!rec.hasFormat(FORMAT_GENOTYPE)) {
        return false;
      }
      final int[] gtArr = VcfUtils.getValidGt(rec, sampleId);
      final String[] alleles = VcfUtils.getAlleleStrings(rec);
      for (final int a : gtArr) {
        if (a > 0) {
          final VariantType altType = VariantType.getType(alleles[0], alleles[a]);
          if (!(altType.isSvType() || altType.isNonVariant())) {
            return true;
          }
        }
      }
      return false;
    }
  }

  /**
   * Creates a variants with all ALT alleles declared in the variant record.
   * Used to perform sample genotype matching against the possibilities defined by the full set of declared alleles.
   * Performs allele trimming (could make this optional?).
   */
  class AllAlts implements VariantFactory {

    private final boolean mTrim;
    private final boolean mExplicitUnknown;

    AllAlts() {
      this(true, false);
    }

    /**
     * Constructor
     * @param trimming if true, trim all leading/trailing bases that match REF from alleles
     * @param explicitUnknown if true, treat half-call allele as a separate token
     */
    public AllAlts(boolean trimming, boolean explicitUnknown) {
      mTrim = trimming;
      mExplicitUnknown = explicitUnknown;
    }

    // Remove any of the ALT entries that have not been assigned an Allele
    static Allele[] pruneEmptyAlts(Allele[] alleles) {
      int b = 2;
      for (int a = 2; a < alleles.length; a++, b++) { // Ignore positions 0 (missing) and 1 (REF)
        if (alleles[a] == null || alleles[a].unknown()) {
          b--;
        } else if (a != b) {
          alleles[b] = alleles[a];
        }
      }
      return b == alleles.length ? alleles : Arrays.copyOf(alleles, b);
    }

    @Override
    public Variant variant(final VcfRecord rec, final int id) throws SkippedVariantException {
      if (rec.getAltCalls().isEmpty()) {
        return null;
      }

      final Allele[] alleles;
      try {
        alleles = pruneEmptyAlts(Allele.getTrimmedAlleles(rec, null, mTrim, mExplicitUnknown));
      } catch (IllegalBaseException e) {
        throw new SkippedVariantException("Invalid VCF allele. " + e.getMessage());
      }
      if (alleles.length < 3) { // No actual ALTs remaining
        return null;
      }
      final Range bounds = Allele.getAlleleBounds(alleles);

      return new Variant(id, rec.getSequenceName(), bounds.getStart(), bounds.getEnd(), alleles, false);
    }
  }
}
