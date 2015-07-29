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
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * A Variant that offers only haploid alternatives.
 */
public final class SquashPloidyVariant extends Variant {

  // Creates a haploid variant for each alt allele referenced by the sample GT
  static class GtAltFactory implements VariantFactory {
    private final RocSortValueExtractor mExtractor;
    private final int mSampleNo;

    /**
     * Constructor
     * @param sampleNo the sample column number (starting from 0) for multiple sample variant calls
     * @param extractor ROC value extractor implementation to use
     */
    GtAltFactory(int sampleNo, RocSortValueExtractor extractor) {
      mSampleNo = sampleNo;
      mExtractor = extractor;
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
      int numAlts = 0;
      for (final int gtId : gtArray) {
        if (gtId > 0) {
          numAlts++;
        }
      }
      final byte[][] alleles = new byte[numAlts][];
      int j = 0;
      for (final int gtId : gtArray) {
        if (gtId > 0) {
          alleles[j++] = DnaUtils.encodeString(getAllele(rec, gtId, hasPreviousNt));
        }
      }
      final double sortValue = mExtractor.getSortValue(rec, mSampleNo);
      final Variant var = new SquashPloidyVariant(id, seqName, start, end, alleles, sortValue);
      for (final RocFilter filter : RocFilter.values()) {
        if (filter.accept(rec, mSampleNo)) {
          var.mFilters.add(filter);
        }
      }
      return var;
    }
  }


  // Creates a haploid variant for each alt allele declared in the variant record
  static class HaploidAltFactory implements VariantFactory {

    private final RocSortValueExtractor mExtractor;

    /**
     * Constructor
     * @param extractor ROC value extractor implementation to use
     */
    HaploidAltFactory(RocSortValueExtractor extractor) {
      mExtractor = extractor;
    }

    @Override
    public Variant variant(VcfRecord rec, int id) {
      if (rec.getAltCalls().size() == 0) {
        return null;
      } // XXXLen ignore SV/symbolic alts, skip variants where there are no alts remaining.
      final String seqName = rec.getSequenceName();
      final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(rec);
      final int start = rec.getStart() + (hasPreviousNt ? 1 : 0);
      final int end = rec.getEnd();

      final byte[][] alleles = new byte[rec.getAltCalls().size()][];
      for (int gtId = 0; gtId < alleles.length; gtId++) {
        alleles[gtId] = DnaUtils.encodeString(getAllele(rec, gtId + 1, hasPreviousNt));
      }
      double sortValue = Double.NaN;
      try {
        sortValue = mExtractor.getSortValue(rec, -1);
      } catch (IndexOutOfBoundsException ignored) {
      }
      final Variant var = new SquashPloidyVariant(id, seqName, start, end, alleles, sortValue);
      var.mFilters.add(RocFilter.ALL);
      return var;
    }
  }

  // Creates all possible diploid variants that include any of the alt alleles declared in the variant record
  static class DiploidAltFactory implements VariantFactory {

    private final RocSortValueExtractor mExtractor;

    /**
     * Constructor
     * @param extractor ROC value extractor implementation to use
     */
    DiploidAltFactory(RocSortValueExtractor extractor) {
      mExtractor = extractor;
    }

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
        alleles[gtId] = DnaUtils.encodeString(getAllele(rec, gtId, hasPreviousNt));
      }
      double sortValue = Double.NaN;
      try {
        sortValue = mExtractor.getSortValue(rec, -1);
      } catch (IndexOutOfBoundsException ignored) {
      }
      final Variant var = new Variant(id, seqName, start, end, alleles, false, sortValue) {
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
      var.mFilters.add(RocFilter.ALL);
      return var;
    }
  }


  private SquashPloidyVariant(int id, String seq, int start, int end, byte[][] alleles, double sortValue) {
    super(id, seq, start, end, alleles, false, sortValue);
  }

  @Override
  public OrientedVariant[] orientations() {
    final OrientedVariant[] pos = new OrientedVariant[numAlleles()];
    for (int i = 0 ; i < numAlleles(); i++) {
      pos[i] = new OrientedVariant(this, i);
    }
    return pos;
  }
}
