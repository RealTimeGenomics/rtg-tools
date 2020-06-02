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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.TreeMap;

import com.rtg.launcher.Statistics;
import com.rtg.util.Counter;
import com.rtg.util.MultiSet;

/**
 * Encapsulate the counts of filtered VCF records
 */
class VcfFilterStatistics implements Statistics {

  enum Stat {
    SAME_AS_REF_FILTERED_COUNT,
    ALL_SAME_AS_REF_FILTERED_COUNT,
    ALLELE_COUNT,
    AMBIGOUS_FILTERED_COUNT,
    READ_DEPTH_FILTERED_COUNT,
    FAILED_KEEP_COUNT,
    NOT_SNP_COUNT,
    GENOTYPE_QUALITY_POSTERIOR_FILTERED_COUNT,
    QUALITY_FILTERED_COUNT,
    ALLELE_BALANCE_FILTERED_COUNT,
    DENSITY_WINDOW_COUNT,
    EXCLUDE_BED_COUNT,
    INCLUDE_BED_COUNT,
    WRITTEN_COUNT,
    TOTAL_COUNT,
    AVR_SCORE_FILTERED_COUNT,
    OVERLAP_COUNT,
    SNP_COUNT,
    DENOVO_SCORE,
    COMBINED_READ_DEPTH_FILTERED_COUNT,
    HOM_FILTERED_COUNT,
    USER_EXPRESSION_COUNT,
  }

  private final int[] mValues = new int[Stat.values().length];
  final MultiSet<String> mFilterTags = new MultiSet<>(new TreeMap<String, Counter>()); // valid filter tags - input file specific
  final MultiSet<String> mInfoTags = new MultiSet<>(new TreeMap<String, Counter>()); // valid info tags - input file specific
  private boolean mPosteriorFiltering;

  @Override
  public void printStatistics(final OutputStream stream) {
    if (stream != null) {
      final PrintStream output = new PrintStream(stream);
      output.println();
      output.println("Total records : " + get(Stat.TOTAL_COUNT));

      printCount(output, "allele count", get(Stat.ALLELE_COUNT));
      for (final String tag : mFilterTags.keySet()) {
        printCount(output, tag, mFilterTags.get(tag));
      }
      for (final String tag : mInfoTags.keySet()) {
        printCount(output, tag, mInfoTags.get(tag));
      }
      printCount(output, "quality", get(Stat.QUALITY_FILTERED_COUNT));
      if (isPosteriorFiltering()) {
        printCount(output, "posterior", get(Stat.GENOTYPE_QUALITY_POSTERIOR_FILTERED_COUNT));
      } else {
        printCount(output, "genotype quality", get(Stat.GENOTYPE_QUALITY_POSTERIOR_FILTERED_COUNT));
      }
      printCount(output, "AVR score", get(Stat.AVR_SCORE_FILTERED_COUNT));
      printCount(output, "sample read depth", get(Stat.READ_DEPTH_FILTERED_COUNT));
      printCount(output, "combined read depth", get(Stat.COMBINED_READ_DEPTH_FILTERED_COUNT));
      printCount(output, "ambiguity ratio", get(Stat.AMBIGOUS_FILTERED_COUNT));
      printCount(output, "allele balance", get(Stat.ALLELE_BALANCE_FILTERED_COUNT));
      printCount(output, "same as reference", get(Stat.SAME_AS_REF_FILTERED_COUNT));
      printCount(output, "all samples same as reference", get(Stat.ALL_SAME_AS_REF_FILTERED_COUNT));
      printCount(output, "homozygous", get(Stat.HOM_FILTERED_COUNT));
      printCount(output, "not a SNP", get(Stat.NOT_SNP_COUNT));
      printCount(output, "simple SNP", get(Stat.SNP_COUNT));
      printCount(output, "not in keep set", get(Stat.FAILED_KEEP_COUNT));
      printCount(output, "overlap with previous", get(Stat.OVERLAP_COUNT));
      printCount(output, "density window", get(Stat.DENSITY_WINDOW_COUNT));
      printCount(output, "include file", get(Stat.INCLUDE_BED_COUNT));
      printCount(output, "exclude file", get(Stat.EXCLUDE_BED_COUNT));
      printCount(output, "de novo score", get(Stat.DENOVO_SCORE));
      printCount(output, "user supplied expression", get(Stat.USER_EXPRESSION_COUNT));
      //output.println("_Filtered due to other : " + m_Other_Filtered_COUNT);
      output.println("Remaining records : " + get(Stat.WRITTEN_COUNT));

      //assert mTotalCount == (mComplexFilteredCount + mDensityFilteredCount + mGenotypeQualityPosteriorFilteredCount + mReadDepthFilteredCount + mWrittenCount + mAmbigousFilteredCount + mSameAsRefFilteredCount + mNotSnpCount + mAlleleBalanceFilteredCount + mQualityFilteredCount + mComplexRegionFilteredCount + mOtherFilteredCount);
    }
  }

  @Override
  public void generateReport() {
    // Unimplemented
  }


  private void printCount(PrintStream output, String tag, int count) {
    if (count != 0) {
      output.println("Filtered due to " + tag + " : " + count);
    }
  }
  int get(Stat s) {
    return mValues[s.ordinal()];
  }
  public void increment(Stat s) {
    mValues[s.ordinal()]++;
  }
  void incrementFilterTag(String tag) {
    mFilterTags.add(tag);
  }
  void incrementInfoTag(String tag) {
    mInfoTags.add(tag);
  }

  boolean isPosteriorFiltering() {
    return mPosteriorFiltering;
  }

  void setPosteriorFiltering(boolean posteriorFiltering) {
    mPosteriorFiltering = posteriorFiltering;
  }
}
