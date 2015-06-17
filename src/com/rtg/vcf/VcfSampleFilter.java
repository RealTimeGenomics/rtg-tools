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

import static com.rtg.vcf.VcfFilterStatistics.Stat;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.MathUtils;
import com.rtg.util.PosteriorUtils;

/**
 */
@TestClass("com.rtg.vcf.VcfFilterCliTest")
public abstract class VcfSampleFilter extends AbstractVcfFilter {
  int[] mSamples = null;
  boolean[] mSampleFailed = null;
  VcfSampleFilter(VcfFilterStatistics stats, Stat stat) {
    super(stats, stat);
  }

  void setSamples(int[] samples, boolean[] failedSamples) {
    mSamples = samples;
    mSampleFailed = failedSamples;
  }

  @Override
  boolean acceptCondition(VcfRecord record) {
    boolean result = true;
    if (mSamples != null) {
      for (int i : mSamples) {
        if (!acceptSample(record, i)) {
          result = false;
          if (mSampleFailed != null) {
            mSampleFailed[i] = true;
          } else {
            break;
          }
        }
      }
    }
    return result;
  }

  abstract boolean acceptSample(VcfRecord record, int index);


  /**
   * Filter on the range of a double field
   */
  public static class MinMaxDoubleFilter extends VcfSampleFilter {
    final double mMin;
    final double mMax;
    final String mField;
    MinMaxDoubleFilter(VcfFilterStatistics stats, Stat stat, double min, double max, String field) {
      super(stats, stat);
      mField = field;
      mMin = min;
      mMax = max;
    }
    @Override
    boolean acceptSample(VcfRecord record, int sampleIndex) {
      // check ambiguity ratio
      final Double val = record.getSampleDouble(sampleIndex, mField);
      return val == null || !(val < mMin || val > mMax);
    }
  }

  /**
   * Filter on the range of an integer field
   */
  public static class MinMaxIntFilter extends VcfSampleFilter {
    final int mMin;
    final int mMax;
    final String mField;
    MinMaxIntFilter(VcfFilterStatistics stats, Stat stat, int min, int max, String field) {
      super(stats, stat);
      mField = field;
      mMin = min;
      mMax = max;
    }
    @Override
    boolean acceptSample(VcfRecord record, int sampleIndex) {
      // check ambiguity ratio
      final Integer val = record.getSampleInteger(sampleIndex, mField);
      return val == null || !(val < mMin || val > mMax);
    }
  }

  /**
   * Filter on Genotype Quality
   */
  public static class GqFilter extends VcfSampleFilter {
    final double mMinGq;
    final double mMaxGq;
    final boolean mPosteriorFiltering;
    GqFilter(VcfFilterStatistics stats, double minQuality, double maxQuality, boolean posteriorFiltering) {
      super(stats, Stat.GENOTYPE_QUALITY_POSTERIOR_FILTERED_COUNT);
      mMinGq = minQuality;
      mMaxGq = maxQuality;
      mPosteriorFiltering = posteriorFiltering;
    }
    @Override
    boolean acceptSample(VcfRecord record, int index) {
      // GQ filtering
      final Double gq = record.getSampleDouble(index, VcfUtils.FORMAT_GENOTYPE_QUALITY);
      if (gq != null) {
        if (mPosteriorFiltering) {
          // check posterior
          final double pgq = PosteriorUtils.unphredIfy(gq) / MathUtils.LOG_10;
          if (pgq < mMinGq || pgq > mMaxGq) {
            return false;
          }
        } else {
          // check genotype quality
          if (gq < mMinGq || gq > mMaxGq) {
            return false;
          }
        }
      }
      return true;
    }
  }

  /**
   * Only allow filtering on a single sample.
   * Records will be accepted if that sample has a de Novo call within the score range
   * and it is the only such sample
   */
  public static class DenovoFilter extends VcfSampleFilter {
    final double mMinDenovoScore;
    final double mMaxDenovoScore;
    DenovoFilter(VcfFilterStatistics stats, double minQuality, double maxQuality) {
      super(stats, Stat.DENOVO_SCORE);
      mMinDenovoScore = minQuality;
      mMaxDenovoScore = maxQuality;
    }

    @Override
    boolean acceptCondition(VcfRecord record) {
      assert mSamples.length == 1;
      for (int sampleIndex : mSamples) {
        if (!"Y".equals(record.getSampleString(sampleIndex, VcfUtils.FORMAT_DENOVO))) {
          if (mSampleFailed != null) {
            mSampleFailed[sampleIndex] = true;
          }
          return false;
        }
      }
      boolean result = false;
      for (int sampleIndex : mSamples) {
        final Double dnp = record.getSampleDouble(sampleIndex, VcfUtils.FORMAT_DENOVO_SCORE);
        if (dnp != null) {
          if (dnp >= mMinDenovoScore && dnp <= mMaxDenovoScore) {
            result = true;
            for (int i = 0; i < record.getNumberOfSamples(); i++) {
              final Double otherDnp = record.getSampleDouble(i, VcfUtils.FORMAT_DENOVO_SCORE);
              final String otherDn = record.getSampleString(i, VcfUtils.FORMAT_DENOVO);
              if (i != sampleIndex && "Y".equals(otherDn) && otherDnp != null && otherDnp >= mMinDenovoScore && otherDnp <= mMaxDenovoScore) {
                result = false;
              }
            }
            return result;
          } else {
            if (mSampleFailed != null) {
              mSampleFailed[sampleIndex] = true;
            }
            return false;
          }

        }
      }
      return result;
    }
    @Override
    boolean acceptSample(VcfRecord record, int index) {
      throw new IllegalArgumentException("De novo filter is a bit weird don't call this");
    }

  }
}
