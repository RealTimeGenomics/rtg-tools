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

  static final class VariantTypeCounts {
    private long mTotal = 0;
    private final long[] mCounts = new long[VariantType.values().length];
    void incrementTotal() {
      mTotal++;
    }
    long total() {
      return mTotal;
    }
    void increment(VariantType type) {
      mCounts[type.ordinal()]++;
    }
    long count(VariantType type) {
      return mCounts[type.ordinal()];
    }
  }

  protected long mTotalUnchanged = 0;
  protected long mDeNovo = 0;
  protected long mPhased = 0;
  protected long mSomatic = 0;

  protected long mTransitions = 0;
  protected long mTransversions = 0;

  protected final VariantTypeCounts mAll = new VariantTypeCounts();
  protected final VariantTypeCounts mHeterozygous = new VariantTypeCounts();
  protected final VariantTypeCounts mHomozygous = new VariantTypeCounts();
  protected final VariantTypeCounts mHaploid = new VariantTypeCounts();

  protected long mMissingGenotype = 0;
  protected long mPartialCalls = 0;
  protected long mPolyploidCalls = 0;

  protected final Histogram[] mAlleleLengths;

  protected PerSampleVariantStatistics() {
    mAlleleLengths = new Histogram[VariantType.values().length];
    for (int i = 0; i < mAlleleLengths.length; ++i) {
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
  protected Pair<List<String>, List<String>> getStatistics() {
    final List<String> names = new ArrayList<>();
    final List<String> values = new ArrayList<>();

    addBreakdown(names, values, mAll, null);
    names.add("Same as reference");
    values.add(Long.toString(mTotalUnchanged));
    names.add("Missing Genotype");
    values.add(mMissingGenotype > 0 ? Long.toString(mMissingGenotype) : null);
    names.add("Partial Genotype");
    values.add(mPartialCalls > 0 ? Long.toString(mPartialCalls) : null);
    names.add("Polyploid Genotypes");
    values.add(mPolyploidCalls > 0 ? Long.toString(mPolyploidCalls) : null);
    names.add("Somatic Genotypes");
    values.add(mSomatic > 0 ? Long.toString(mSomatic) : null);
    names.add("De Novo Genotypes");
    values.add(mDeNovo > 0 ? Long.toString(mDeNovo) : null);
    names.add("Phased Genotypes");
    final long totalNonMissingGenotypes = mAll.count(VariantType.SNP) + mAll.count(VariantType.MNP) + mAll.count(VariantType.INSERTION) + mAll.count(VariantType.DELETION) + mAll.count(VariantType.INDEL) + mTotalUnchanged + mPartialCalls;
    values.add(mPhased > 0 ? VariantStatistics.percent(mPhased, totalNonMissingGenotypes) : null);
    names.add("SNP Transitions/Transversions");
    values.add(VariantStatistics.divide(mTransitions, mTransversions));

    //haploid stats
    addBreakdown(names, values, mHaploid, "Haploid");

    //if diploid het/hom breakdowns are available
    final Maybe notHaploid = maybe(mHeterozygous.total() > 0 || mHomozygous.total() > 0);
    names.add("Total Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.total(), mHomozygous.total())));
    names.add("SNP Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.count(VariantType.SNP), mHomozygous.count(VariantType.SNP))));
    names.add("MNP Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.count(VariantType.MNP), mHomozygous.count(VariantType.MNP))));
    names.add("Insertion Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.count(VariantType.INSERTION), mHomozygous.count(VariantType.INSERTION))));
    names.add("Deletion Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.count(VariantType.DELETION), mHomozygous.count(VariantType.DELETION))));
    names.add("Indel Het/Hom ratio");
    values.add(notHaploid.val(VariantStatistics.divide(mHeterozygous.count(VariantType.INDEL), mHomozygous.count(VariantType.INDEL))));
    names.add("Breakend Het/Hom ratio");
    values.add(notHaploid.val(mAll.count(VariantType.SV_BREAKEND) - mHaploid.count(VariantType.SV_BREAKEND) > 0 ? VariantStatistics.divide(mHeterozygous.count(VariantType.SV_BREAKEND), mHomozygous.count(VariantType.SV_BREAKEND)) : null));
    names.add("Symbolic SV Het/Hom ratio");
    values.add(notHaploid.val(mAll.count(VariantType.SV_SYMBOLIC) - mHaploid.count(VariantType.SV_SYMBOLIC) > 0 ? VariantStatistics.divide(mHeterozygous.count(VariantType.SV_SYMBOLIC), mHomozygous.count(VariantType.SV_SYMBOLIC)) : null));

    names.add("Insertion/Deletion ratio");
    values.add(VariantStatistics.divide(mAll.count(VariantType.INSERTION), mAll.count(VariantType.DELETION)));
    names.add("Indel/SNP+MNP ratio");
    values.add(VariantStatistics.divide(mAll.count(VariantType.INDEL) + mAll.count(VariantType.INSERTION) + mAll.count(VariantType.DELETION), mAll.count(VariantType.SNP) + mAll.count(VariantType.MNP)));
    return Pair.create(names, values);

  }

  private void addBreakdown(List<String> names, List<String> values, VariantTypeCounts counts, String label) {
    final Maybe maybe = maybe(label == null || counts.total() > 0);
    final String prefix;
    if (label == null) {
      prefix = "";
    } else {
      names.add("Total " + label);
      prefix = label + " ";
      values.add(maybe.val(Long.toString(counts.total())));
    }
    names.add(prefix + "SNPs");
    values.add(maybe.val(Long.toString(counts.count(VariantType.SNP))));
    names.add(prefix + "MNPs");
    values.add(maybe.val(Long.toString(counts.count(VariantType.MNP))));
    names.add(prefix + "Insertions");
    values.add(maybe.val(Long.toString(counts.count(VariantType.INSERTION))));
    names.add(prefix + "Deletions");
    values.add(maybe.val(Long.toString(counts.count(VariantType.DELETION))));
    names.add(prefix + "Indels");
    values.add(maybe.val(Long.toString(counts.count(VariantType.INDEL))));
    names.add(prefix + "Breakends");
    values.add(maybe.val(counts.count(VariantType.SV_BREAKEND) > 0 ? Long.toString(counts.count(VariantType.SV_BREAKEND)) : null));
    names.add(prefix + "Symbolic SVs");
    values.add(maybe.val(counts.count(VariantType.SV_SYMBOLIC) > 0 ? Long.toString(counts.count(VariantType.SV_SYMBOLIC)) : null));
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
    for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; ++i) {
      if (i <= VariantType.INDEL.ordinal() || mAlleleLengths[i].getLength() != 0) {
        sb.append("\t").append(VARIANT_TYPE_NAMES[i]);
      }
    }
    sb.append(StringUtils.LS);

    int size = 0;
    final int[] lengths = new int[mAlleleLengths.length];
    for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; ++i) {
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
      for (int i = VariantType.SNP.ordinal(); i < mAlleleLengths.length; ++i) {
        if (i <= VariantType.INDEL.ordinal() || mAlleleLengths[i].getLength() != 0) {
          long sum = 0L;
          for (int j = bin; j < end; ++j) {
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
