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

package com.rtg.vcf;

import java.util.ArrayList;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Histogram;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;

/**
 *
 */
@TestClass("com.rtg.vcf.VariantStatisticsTest")
public class PerSampleVariantStatistics {

  private static final int EXP_STEP = 100;

  private static final String[] VARIANT_TYPE_NAMES = {
      "No-call", "Reference", "SNP", "MNP", "Delete", "Insert", "Indel", "Breakend", "Symbolic"
  };

  protected long mTotalUnchanged = 0;
  protected long mHeterozygous = 0;
  protected long mHomozygous = 0;
  protected long mHaploid = 0;
  protected long mDeNovo = 0;
  protected long mPhased = 0;

  protected long mTotalSnps = 0;
  protected long mTransitions = 0;
  protected long mTransversions = 0;
  protected long mHeterozygousSnps = 0;
  protected long mHomozygousSnps = 0;
  protected long mHaploidSnps = 0;

  protected long mTotalMnps = 0;
  protected long mHeterozygousMnps = 0;
  protected long mHomozygousMnps = 0;
  protected long mHaploidMnps = 0;

  protected long mTotalInsertions = 0;
  protected long mHeterozygousInsertions = 0;
  protected long mHomozygousInsertions = 0;
  protected long mHaploidInsertions = 0;
  protected long mTotalDeletions = 0;
  protected long mHeterozygousDeletions = 0;
  protected long mHomozygousDeletions = 0;
  protected long mHaploidDeletions = 0;

  protected long mTotalIndels = 0;
  protected long mHeterozygousIndels = 0;
  protected long mHomozygousIndels = 0;
  protected long mHaploidIndels = 0;

  protected long mTotalBreakends = 0;
  protected long mHeterozygousBreakends = 0;
  protected long mHomozygousBreakends = 0;
  protected long mHaploidBreakends = 0;

  protected long mTotalSymbolicSvs = 0;
  protected long mHeterozygousSymbolicSvs = 0;
  protected long mHomozygousSymbolicSvs = 0;
  protected long mHaploidSymbolicSvs = 0;

  protected long mMissingGenotype = 0;

  protected final Histogram[] mAlleleLengths;

  PerSampleVariantStatistics() {
    mAlleleLengths = new Histogram[VariantType.values().length];
    for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; i++) {
      // i from SNP as we don't care about NO_CALL/UNCHANGED
      mAlleleLengths[i] = new Histogram();
    }
  }

  Maybe maybe(boolean b) {
    return b ? new Something() : new Nothing();
  }
  private interface Maybe {
    <T> T val(T v);
  }
  private static class Something implements Maybe {
    @Override
    public <T> T val(T v) {
      return v;
    }
  }
  private static class Nothing implements Maybe {
    @Override
    public <T> T val(T v) {
      return null;
    }
  }
  Pair<List<String>, List<String>> getStatistics() {
    final List<String> names = new ArrayList<>();
    final List<String> values = new ArrayList<>();
    names.add("SNPs");
    values.add(Long.toString(mTotalSnps));
    names.add("MNPs");
    values.add(Long.toString(mTotalMnps));
    names.add("Insertions");
    values.add(Long.toString(mTotalInsertions));
    names.add("Deletions");
    values.add(Long.toString(mTotalDeletions));
    names.add("Indels");
    values.add(Long.toString(mTotalIndels));
    names.add("Structural variant breakends");
    values.add(mTotalBreakends > 0 ? Long.toString(mTotalBreakends) : null);
    names.add("Symbolic structural variants");
    values.add(mTotalSymbolicSvs > 0 ? Long.toString(mTotalSymbolicSvs) : null);
    names.add("Same as reference");
    values.add(Long.toString(mTotalUnchanged));
    names.add("Missing Genotype");
    values.add(mMissingGenotype > 0 ? Long.toString(mMissingGenotype) : null);
    final long totalNonMissingGenotypes = mTotalSnps + mTotalMnps + mTotalInsertions + mTotalDeletions + mTotalIndels + mTotalUnchanged;
    names.add("De Novo Genotypes");
    values.add(mDeNovo > 0 ? Long.toString(mDeNovo) : null);
    names.add("Phased Genotypes");
    values.add(mPhased > 0 ? VariantStatistics.percent(mPhased, totalNonMissingGenotypes) : null);
    names.add("SNP Transitions/Transversions");
    values.add(VariantStatistics.divide(mTransitions, mTransversions));

    //haploid stats
    final Maybe haploid = maybe(mHaploid > 0);
    names.add("Total Haploid");
    values.add(haploid.val(Long.toString(mHaploid)));
    names.add("Haploid SNPs");
    values.add(haploid.val(Long.toString(mHaploidSnps)));
    names.add("Haploid MNPs");
    values.add(haploid.val(Long.toString(mHaploidMnps)));
    names.add("Haploid Insertions");
    values.add(haploid.val(Long.toString(mHaploidInsertions)));
    names.add("Haploid Deletions");
    values.add(haploid.val(Long.toString(mHaploidDeletions)));
    names.add("Haploid Indels");
    values.add(haploid.val(Long.toString(mHaploidIndels)));
    names.add("Haploid Breakends");
    values.add(haploid.val(mHaploidBreakends > 0 ? Long.toString(mHaploidBreakends) : null));
    names.add("Haploid Symbolic SVs");
    values.add(haploid.val(mHaploidSymbolicSvs > 0 ? Long.toString(mHaploidSymbolicSvs) : null));

    //not if haploid
    final Maybe notHaploid = maybe(mHeterozygous > 0 || mHomozygous > 0);
    names.add("Total Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous, mHomozygous)));
    names.add("SNP Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygousSnps, mHomozygousSnps)));
    names.add("MNP Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygousMnps, mHomozygousMnps)));
    names.add("Insertion Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygousInsertions, mHomozygousInsertions)));
    names.add("Deletion Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygousDeletions, mHomozygousDeletions)));
    names.add("Indel Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygousIndels, mHomozygousIndels)));
    names.add("Breakend Het/Hom ratio");
    values.add(notHaploid.val(mTotalBreakends - mHaploidBreakends > 0 ? VariantStatistics.divide(mHeterozygousBreakends, mHomozygousBreakends) : null));
    names.add("Symbolic SV Het/Hom ratio");
    values.add(notHaploid.val(mTotalSymbolicSvs - mHaploidSymbolicSvs > 0 ? VariantStatistics.divide(mHeterozygousSymbolicSvs, mHomozygousSymbolicSvs) : null));

    names.add("Insertion/Deletion ratio");
    values.add(VariantStatistics.divide(mTotalInsertions, mTotalDeletions));
    names.add("Indel/SNP+MNP ratio");
    values.add(VariantStatistics.divide(mTotalIndels + mTotalInsertions + mTotalDeletions, mTotalSnps + mTotalMnps));
    return Pair.create(names, values);

  }

  /**
   * Append per sample statistics to a buffer.
   * @param sb buffer to append to
   */
  public void appendStatistics(StringBuilder sb) {
    final Pair<List<String>, List<String>> statistics = getStatistics();
    VariantStatistics.printCounts(statistics.getA(), statistics.getB(), sb);
  }

  /**
   * Append per sample histograms to a buffer.
   * @param sb buffer to append to
   */
  public void appendHistograms(StringBuilder sb) {
    sb.append("Variant Allele Lengths :").append(StringUtils.LS);
    //sb.append("bin\tSNP\tMNP\tInsert\tDelete\tIndel").append(StringUtils.LS);
    sb.append("length");
    for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; i++) {
      if (i <= VariantType.INDEL.ordinal() || mAlleleLengths[i].getLength() != 0) {
        sb.append("\t").append(VARIANT_TYPE_NAMES[i]);
      }
    }
    sb.append(StringUtils.LS);

    int size = 0;
    final int[] lengths = new int[mAlleleLengths.length];
    for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; i++) {
      lengths[i] = mAlleleLengths[i].getLength();
      if (lengths[i] > size) {
        size = lengths[i];
      }
    }
    int bin = 1;
    int step = 1;
    while (bin < size) {
      sb.append(bin);
      final int end = bin + step;
      if (end - bin > 1) {
        sb.append("-").append(end - 1);
      }
      for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; i++) {
        if (i <= VariantType.INDEL.ordinal() || mAlleleLengths[i].getLength() != 0) {
          long sum = 0L;
          for (int j = bin; j < end; j++) {
            if (j < lengths[i]) {
              sum += mAlleleLengths[i].getValue(j);
            }
          }
          sb.append("\t").append(sum);
        }
      }
      sb.append(StringUtils.LS);

      bin += step;
      // increase step to give exp bin sizes
      if (bin % (EXP_STEP * step) == 0) {
        step *= EXP_STEP;
      }
    }
  }
}
