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

import com.rtg.mode.DnaUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * A Variant that offers only haploid alternatives.
 */
public final class SquashPloidyVariant extends Variant {

  // Creates a haploid variant for each alt allele referenced by the sample GT
  static class Factory implements VariantFactory {
    private final RocSortValueExtractor mExtractor;
    private final int mSampleNo;

    public Factory(int sampleNo, RocSortValueExtractor extractor) {
      mSampleNo = sampleNo;
      mExtractor = extractor;
    }

    @Override
    public Variant variant(VcfRecord rec) {
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
      final SquashPloidyVariant var = new SquashPloidyVariant(seqName, start, end, alleles, sortValue);
      for (final RocFilter filter : RocFilter.values()) {
        if (filter.accept(rec, mSampleNo)) {
          var.mFilters.add(filter);
        }
      }
      return var;
    }
  }


  // Creates a haploid variant for each alt allele declared in the variant record
  static class AnyAltFactory implements VariantFactory {

    private final RocSortValueExtractor mExtractor;

    public AnyAltFactory(RocSortValueExtractor extractor) {
      mExtractor = extractor;
    }

    @Override
    public Variant variant(VcfRecord rec) {
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
      final SquashPloidyVariant var = new SquashPloidyVariant(seqName, start, end, alleles, sortValue);
      var.mFilters.add(RocFilter.ALL);
      return var;
    }
  }



  private SquashPloidyVariant(String seq, int start, int end, byte[][] alleles, double sortValue) {
    super(seq, start, end, alleles, false, sortValue);
  }

  @Override
  public boolean isPhased() {
    return false;
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
