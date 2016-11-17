/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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
package com.rtg.vcf.annotation;

import java.util.ArrayList;
import java.util.List;

import com.rtg.util.StringUtils;
import com.rtg.util.array.ArrayUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.PedigreeField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Records counts of observations that are contrary to called genotypes
 */
class ContraryObservationCounter {

  static class Counts {
    private final int mOriginalContraryCount;
    private final int mDerivedContraryCount;
    private final double mOriginalContraryFraction;
    private final double mDerivedContraryFraction;

    public Counts(int originalContraryCount, int derivedContraryCount, double originalContraryFraction, double derivedContraryFraction) {
      mOriginalContraryCount = originalContraryCount;
      mDerivedContraryCount = derivedContraryCount;
      mOriginalContraryFraction = originalContraryFraction;
      mDerivedContraryFraction = derivedContraryFraction;
    }

    protected int getOriginalContraryCount() {
      return mOriginalContraryCount;
    }

    protected int getDerivedContraryCount() {
      return mDerivedContraryCount;
    }

    protected double getOriginalContraryFraction() {
      return mOriginalContraryFraction;
    }

    protected double getDerivedContraryFraction() {
      return mDerivedContraryFraction;
    }
    protected Double getContraryFraction() {
      final double contraryFraction = getOriginalContraryFraction() + getDerivedContraryFraction();
      return (Double.isNaN(contraryFraction)) ? null : contraryFraction;
    }
    protected int getContraryCount() {
      return getOriginalContraryCount() + getDerivedContraryCount();
    }

  }

  private VcfHeader mHeader = null;
  private List<List<Integer>> mSampleToAntecedents = null;

  void initSampleInfo(final VcfHeader header) {
    mHeader = header;
    mSampleToAntecedents = new ArrayList<>(header.getNumberOfSamples());
    for (int k = 0; k < header.getNumberOfSamples(); k++) {
      mSampleToAntecedents.add(new ArrayList<>());
    }
    final List<PedigreeField> pedigreeLines = mHeader.getPedigreeLines();
    for (final PedigreeField pedLine : pedigreeLines) {
      final String orig = pedLine.getOriginal();
      final String derived = pedLine.getDerived();
      if (orig != null && derived != null) {
        mSampleToAntecedents.get(header.getSampleIndex(derived)).add(header.getSampleIndex(orig));
      }
      final String child = pedLine.getChild();
      if (child != null) {
        final Integer childIndex = header.getSampleIndex(child);
        if (childIndex != null) {
          final String father = pedLine.getFather();
          if (father != null) {
            final Integer fatherIndex = header.getSampleIndex(father);
            if (fatherIndex != null) {
              mSampleToAntecedents.get(childIndex).add(fatherIndex);
            }
          }
          final String mother = pedLine.getMother();
          if (mother != null) {
            final Integer motherIndex = header.getSampleIndex(mother);
            if (motherIndex != null) {
              mSampleToAntecedents.get(childIndex).add(motherIndex);
            }
          }
        }
      }
    }
  }

  private int[] ad(final int[] res, final VcfRecord record, final int sample) {
    final String ad = record.getSampleString(sample, VcfUtils.FORMAT_ALLELIC_DEPTH);
    if (ad != null && !VcfUtils.MISSING_FIELD.equals(ad)) {
      final String[] adSplit = StringUtils.split(ad, ',');
      for (int k = 0; k < res.length; k++) {
        res[k] += Integer.parseInt(adSplit[k]);
      }
    }
    return res;
  }

  private int[] ad(final VcfRecord record, final int sample) {
    return ad(new int[record.getAltCalls().size() + 1], record, sample);
  }

  private int[] ad(final VcfRecord record, final List<Integer> samples) {
    final int[] res = new int[record.getAltCalls().size() + 1];
    for (final int s : samples) {
      ad(res, record, s);
    }
    return res;
  }

  private boolean[] alleles(final boolean[] res, final VcfRecord record, final int sampleNumber) {
    final int[] originalGt = VcfUtils.getValidGt(record, sampleNumber);
    for (final int allele : originalGt) {
      if (allele >= 0) {
        res[allele] = true;
      }
    }
    return res;
  }

  private boolean[] alleles(final VcfRecord record, final int sampleNumber) {
    return alleles(new boolean[record.getAltCalls().size() + 1], record, sampleNumber);
  }

  private boolean[] alleles(final VcfRecord record, final List<Integer> samples) {
    final boolean[] res = new boolean[record.getAltCalls().size() + 1];
    for (final int s : samples) {
      alleles(res, record, s);
    }
    return res;
  }

  // Return a counts object, or null if no counts are available
  Counts getCounts(VcfRecord record, int sampleNumber) {
    assert mHeader != null; // i.e. checkHeader method must be called before this
    if (sampleNumber >= mSampleToAntecedents.size()) {
      return null; // No such sample
    }
    final List<Integer> antecedents = mSampleToAntecedents.get(sampleNumber);
    if (antecedents.isEmpty()) {
      return null; // Not a derived or child sample
    }
    final Integer ss = record.getSampleInteger(sampleNumber, VcfUtils.FORMAT_SOMATIC_STATUS);
    if (ss != null) {
      if (ss != 2) {
        return null; // Not a somatic call
      }
    } else {
      // Might be a family de novo type call
      final String deNovoStatus = record.getSampleString(sampleNumber, VcfUtils.FORMAT_DENOVO);
      if (!"Y".equals(deNovoStatus)) {
        return null; // Not a de novo
      }
    }
    final int[] originalAd = ad(record, antecedents);
    final int[] derivedAd = ad(record, sampleNumber);
    final long oSum = ArrayUtils.sum(originalAd);
    final long dSum = ArrayUtils.sum(derivedAd);
    if (oSum == 0 || dSum == 0) {
      return null;
    }
    final boolean[] originalAlleles = alleles(record, antecedents);
    final boolean[] derivedAlleles = alleles(record, sampleNumber);
    assert originalAlleles.length == originalAd.length && originalAlleles.length == derivedAlleles.length && originalAlleles.length == derivedAd.length;
    final double invOriginalAdSum = 1.0 / oSum;
    final double invDerivedAdSum = 1.0 / dSum;
    int derivedContraryCount = 0;
    double derivedContraryFraction = 0.0;
    int origContraryCount = 0;
    double origContraryFraction = 0.0;
    for (int k = 0; k < originalAlleles.length; k++) {
      if (originalAlleles[k] && !derivedAlleles[k]) {
        derivedContraryCount += derivedAd[k];
        derivedContraryFraction += derivedAd[k] * invDerivedAdSum;
      } else if (!originalAlleles[k] && derivedAlleles[k]) {
        origContraryCount += originalAd[k];
        origContraryFraction += originalAd[k] * invOriginalAdSum;
      }
    }
    return new Counts(origContraryCount, derivedContraryCount, origContraryFraction, derivedContraryFraction);
  }
}
