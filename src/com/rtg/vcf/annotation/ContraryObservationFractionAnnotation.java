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

package com.rtg.vcf.annotation;

import java.util.ArrayList;
import java.util.List;

import com.rtg.util.StringUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.PedigreeField;
import com.rtg.vcf.header.VcfHeader;

/**
 * The fraction of evidence that is considered contrary to the call made for this sample.
 * For example, in a somatic call of 0/0 -&gt; 1/0, the <code>COF</code>
 * value is the fraction of the somatic (1) allele in the normal sample.
 * These attributes are also applicable to de novo calls, where the evidence
 * of the parents for the de novo allele is considered contrary.
 * Usually a high <code>COF</code> value indicates an unreliable call.
 */
public class ContraryObservationFractionAnnotation extends AbstractDerivedFormatAnnotation {

  private VcfHeader mHeader = null;
  private List<List<Integer>> mSampleToAntecedents = null;

  protected ContraryObservationFractionAnnotation(final String field, final String description, final AnnotationDataType type) {
    super(field, description, type);
  }

  /**
   * Construct a new contrary observation fraction format annotation.
   */
  public ContraryObservationFractionAnnotation() {
    this("COF", "Contrary observation fraction", AnnotationDataType.DOUBLE);
  }

  private void initSampleInfo(final VcfHeader header) {
    mHeader = header;
    mSampleToAntecedents = new ArrayList<>(header.getNumberOfSamples());
    for (int k = 0; k < header.getNumberOfSamples(); k++) {
      mSampleToAntecedents.add(new ArrayList<Integer>());
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

  protected Object getValue(final double contraryFraction, final int contraryCount) {
    return Double.isNaN(contraryFraction) ? null : contraryFraction;
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
    if (originalGt != null) {
      for (final int allele : originalGt) {
        if (allele >= 0) {
          res[allele] = true;
        }
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

  private int sum(final int... a) {
    int s = 0;
    for (final int v : a) {
      s += v;
    }
    return s;
  }

  @Override
  public Object getValue(final VcfRecord record, final int sampleNumber) {
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
    if (originalAd == null || derivedAd == null) {
      return null; // Safety, should not happen on well-formed data
    }
    final boolean[] originalAlleles = alleles(record, antecedents);
    final boolean[] derivedAlleles = alleles(record, sampleNumber);
    if (originalAlleles == null || derivedAlleles == null) {
      return null; // Safety, should not happen on well-formed data
    }
    assert originalAlleles.length == originalAd.length && originalAlleles.length == derivedAlleles.length && originalAlleles.length == derivedAd.length;
    final double invOriginalAdSum = 1.0 / sum(originalAd);
    final double invDerivedAdSum = 1.0 / sum(derivedAd);
    int contraryCount = 0;
    double contraryFraction = 0.0;
    for (int k = 0; k < originalAlleles.length; k++) {
      if (originalAlleles[k] ^ derivedAlleles[k]) {
        contraryCount += originalAlleles[k] ? derivedAd[k] : originalAd[k];
        contraryFraction += originalAlleles[k] ? derivedAd[k] * invDerivedAdSum : originalAd[k] * invOriginalAdSum;
      }
    }
    return getValue(contraryFraction, contraryCount);
  }

  @Override
  public String checkHeader(VcfHeader header) {
    initSampleInfo(header);
    // Need either SS (for somatic caller) or DN (for family caller)
    final String somaticStatus = checkHeader(header, null, new String[]{VcfUtils.FORMAT_SOMATIC_STATUS});
    final String denovoStatus = checkHeader(header, null, new String[]{VcfUtils.FORMAT_DENOVO});
    if (somaticStatus != null && denovoStatus != null) {
      return "Derived annotation COC missing required fields in VCF header (FORMAT fields: SS or DN)";
    }
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_ALLELIC_DEPTH, VcfUtils.FORMAT_GENOTYPE});
  }

}
