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

import com.rtg.mode.DnaUtils;
import com.rtg.util.StringUtils;
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
   */
  class Default implements VariantFactory {

    static final String NAME = "default";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public Default(int sampleNo) {
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
      final String seqName = rec.getSequenceName();
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();
      final boolean phased = VcfUtils.isPhasedGt(gt);

      // Treats missing value as distinct value ("N")
      final Allele[] alleles = new Allele[VcfUtils.isHomozygousAlt(gtArray) ? 1 : 2];
      for (int i = 0; i < alleles.length; i++) {
        alleles[i] = new Allele(seqName, start, end, DnaUtils.encodeString(Variant.getAllele(rec, gtArray[i], hasPreviousNt)));
      }

      return new CompactVariant(id, seqName, start, end, alleles, phased);
    }
  }

  /**
   * Construct Variants corresponding to the GT of a specified sample, maintaining original allele IDs
   * The only allele trimming done is a single leading
   * padding base, and only if it is shared by all alleles.
   * Path finding will require full genotype to match.
   */
  class DefaultId implements VariantFactory {

    static final String NAME = "default-id";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public DefaultId(int sampleNo) {
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
      final String seqName = rec.getSequenceName();
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();
      final boolean phased = VcfUtils.isPhasedGt(gt);

      // Treats missing value as ref
      final Allele[] alleles = new Allele[rec.getAltCalls().size() + 1];
      for (int i = 0; i < alleles.length; i++) {
        alleles[i] = new Allele(seqName, start, end, DnaUtils.encodeString(Variant.getAllele(rec, i, hasPreviousNt)));
      }
      final int alleleA = gtArray[0] == -1 ? 0 : gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1] == -1 ? 0 : gtArray[1];  // Treats missing value as ref

      return new AlleleIdVariant(id, seqName, start, end, alleles, alleleA, alleleB, phased);
    }
  }

  /**
   * Construct Variants corresponding to the GT of a specified sample.
   * This version performs trimming of all common leading/trailing bases that match REF.
   * Path finding will require full genotype to match, but will be more permissive of padding bases.
   */
  class TrimmedGtFactory implements VariantFactory {

    static final String NAME = "default-trim";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public TrimmedGtFactory(int sampleNo) {
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

      final String[] allAlleles = VcfUtils.getAlleleStrings(rec, false);
      for (int i = 1; i < allAlleles.length; i++) {
        if (!(gtArray[0] == i || (gtArray.length == 2 && gtArray[1] == i))) {
          allAlleles[i] = null;
        }
      }
      final int stripLeading = StringUtils.longestPrefix(allAlleles);
      final int stripTrailing = StringUtils.longestSuffix(stripLeading, allAlleles);
      for (int i = 0; i < allAlleles.length; i++) {
        if (allAlleles[i] != null) {
          allAlleles[i] = StringUtils.clip(allAlleles[i], stripLeading, stripTrailing);
        }
      }
      final String seqName = rec.getSequenceName();
      final int start = rec.getStart() + stripLeading;
      final int end = rec.getEnd() - stripTrailing;
      final Allele[] alleles = new Allele[VcfUtils.isHomozygousAlt(gtArray) ? 1 : 2];
      for (int i = 0; i < alleles.length; i++) {
        final int allele = gtArray[i];
        alleles[i] = new Allele(seqName, start, end, DnaUtils.encodeString(allele == -1 ? "N" : allAlleles[allele]));
      }

      final boolean phased = VcfUtils.isPhasedGt(gt);

      return new CompactVariant(id, seqName, start, end, alleles, phased);
    }
  }

  /**
   * Construct Variants corresponding to the GT of a specified sample, maintaining original allele IDs
   * This version performs trimming of all common leading/trailing bases that match REF.
   * Path finding will require full genotype to match, but will be more permissive of reference overlaps.
   */
  class TrimmedGtIdFactory implements VariantFactory {

    static final String NAME = "default-trim-id";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    public TrimmedGtIdFactory(int sampleNo) {
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

      final Allele[] alleles = new Allele[rec.getAltCalls().size() + 1];
      final String seqName = rec.getSequenceName();
      int start = Integer.MAX_VALUE;
      int end = Integer.MIN_VALUE;
      for (int i = 0; i < alleles.length; i++) {
        if (gtArray[0] == i || (gtArray.length == 2 && gtArray[1] == i)) {
          final Allele allele = getAllele(rec, i, true);
          if (allele != null) {
            alleles[i] = allele;
            start = Math.min(start, alleles[i].getStart());
            end = Math.max(end, alleles[i].getEnd());
          }
        }
      }
      final int alleleA = gtArray[0];
      final int alleleB = gtArray.length == 1 ? alleleA : gtArray[1];
      final boolean phased = VcfUtils.isPhasedGt(gt);

      return new AlleleIdVariant(id, seqName, start, end, alleles, alleleA, alleleB, phased);
    }

    // Create an Allele from a VCF record.
    // If trim is enabled, remove all leading and trailing bases agreeing with ref, and adjust position accordingly.
    // If the resulting allele is a no-op, return null
    protected static Allele getAllele(VcfRecord rec, int allele, boolean trim) {
      if (allele == -1) {
        return null;
      }
      final String ref = rec.getRefCall();
      if (allele == 0) {
        return trim ? null : new Allele(rec, DnaUtils.encodeString(ref));
      }
      final String alt = rec.getAllele(allele);
      if (trim) {
        final int stripLeading = StringUtils.longestPrefix(ref, alt);
        final int stripTrailing = StringUtils.longestSuffix(stripLeading, ref, alt);
        return new Allele(rec.getSequenceName(), rec.getStart() + stripLeading, rec.getEnd() - stripTrailing,
          DnaUtils.encodeString(StringUtils.clip(alt, stripLeading, stripTrailing)));
      }
      return new Allele(rec, DnaUtils.encodeString(alt));
    }
  }

  /**
   * Creates a haploid oriented variant for each ALT allele referenced by the sample GT.
   * Path finding will match any variants where there are any non-ref allele matches.
   */
  class HaploidGtAltFactory implements VariantFactory {

    static final String NAME = "squash";

    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     */
    HaploidGtAltFactory(int sampleNo) {
      mSampleNo = sampleNo;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      // Currently we skip non-variant and SV
      if (!VcfUtils.hasDefinedVariantGt(rec, mSampleNo)) {
        return null;
      }
      final String seqName = rec.getSequenceName();
      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();

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
          alleles[j++] = new Allele(seqName, start, end, DnaUtils.encodeString(Variant.getAllele(rec, gtId, hasPreviousNt)));
        }
        prev = gtId;
      }

      return new SquashPloidyVariant(id, seqName, start, end, alleles);
    }
  }

  /**
   * Creates a haploid oriented variant for every ALT allele declared in the variant record.
   * Used to perform allele matching against the full set of declared alleles rather than those in a sample column.
   */
  class HaploidAltsFactory implements VariantFactory {

    static final String NAME = "hap-alt";

    @Override
    public Variant variant(VcfRecord rec, int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // XXXLen ignore SV/symbolic alts, skip variants where there are no alts remaining.
      final String seqName = rec.getSequenceName();
      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();

      final Allele[] alleles = new Allele[rec.getAltCalls().size()];
      for (int gtId = 0; gtId < alleles.length; gtId++) {
        alleles[gtId] = new Allele(seqName, start, end, DnaUtils.encodeString(Variant.getAllele(rec, gtId + 1, hasPreviousNt)));
      }

      return new SquashPloidyVariant(id, seqName, start, end, alleles);
    }
  }

  /**
   * Creates all possible non-ref diploid variants using any of the ALT alleles declared in the variant record.
   * Used to perform sample genotype matching against the possibilities defined by the full set of declared alleles.
   */
  class DiploidAltsFactory implements VariantFactory {

    static final String NAME = "dip-alt";

    @Override
    public Variant variant(final VcfRecord rec, final int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // XXXLen ignore SV/symbolic alts, skip variants where there are no alts remaining.
      final String seqName = rec.getSequenceName();
      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();
      final byte[][] alleles = new byte[1 + rec.getAltCalls().size()][];
      for (int gtId = 0; gtId < alleles.length; gtId++) {
        alleles[gtId] = DnaUtils.encodeString(Variant.getAllele(rec, gtId, hasPreviousNt));
      }

      return new Variant(id, seqName, start, end, alleles, false) {
        @Override
        public OrientedVariant[] orientations() {
          final OrientedVariant[] pos = new OrientedVariant[numAlleles() * numAlleles() - 1];
          int v = 0;
          for (int i = 1 ; i < numAlleles(); i++) {
            for (int j = 0; j < i; j++) {
              pos[v++] = new OrientedVariant(this, true, i, j);
              pos[v++] = new OrientedVariant(this, false, j, i);
            }
            pos[v++] = new OrientedVariant(this, true, i, i);
          }
          assert v == pos.length : rec.toString() + Arrays.toString(pos);
          return pos;
        }
      };
    }
  }
}
