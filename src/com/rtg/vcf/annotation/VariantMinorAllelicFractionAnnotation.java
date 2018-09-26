/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import static com.rtg.vcf.VcfUtils.FORMAT_ADE;
import static com.rtg.vcf.VcfUtils.FORMAT_ALLELIC_DEPTH;
import static com.rtg.vcf.VcfUtils.MISSING_FIELD;

import com.rtg.util.StringUtils;
import com.rtg.util.array.ArrayUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * The fraction of evidence that is supporting the most frequent non-reference allele.
 */
public class VariantMinorAllelicFractionAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * Constructor
   */
  public VariantMinorAllelicFractionAnnotation() {
    super(new FormatField("VAF1", MetaType.FLOAT, VcfNumber.ONE, "Allelic fraction of the most frequent ALT allele"), Formatter.DEFAULT_DOUBLE);
  }

  private double[] ad(final double[] res, final VcfRecord record, final int sample) {
    String ad = record.getSampleString(sample, FORMAT_ADE);
    if (ad == null || MISSING_FIELD.equals(ad)) {
      ad = record.getSampleString(sample, FORMAT_ALLELIC_DEPTH);
    }
    if (ad != null && !MISSING_FIELD.equals(ad)) {
      final String[] adSplit = StringUtils.split(ad, ',');
      for (int k = 0; k < res.length; ++k) {
        res[k] += Double.parseDouble(adSplit[k]);
      }
    }
    return res;
  }

  private double[] ad(final VcfRecord record, final int sample) {
    return ad(new double[record.getAltCalls().size() + 1], record, sample);
  }

  @Override
  public Object getValue(final VcfRecord record, final int sampleNumber) {
    final double[] ad = ad(record, sampleNumber);
    final double sum = ArrayUtils.sum(ad);
    if (ad.length < 2 || sum == 0) {
      return null;
    }
    final double[] vaf = new double[ad.length - 1];
    double max = 0;
    for (int i = 0; i < vaf.length; i++) {
      vaf[i] = ad[i + 1] / sum;
      max = Math.max(max, vaf[i]);
    }
    return max;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    if (header.getFormatField(FORMAT_ADE) == null && header.getFormatField(FORMAT_ALLELIC_DEPTH) == null) {
      return "Derived annotation " + getName() + " missing required fields in VCF header" + " (FORMAT fields: " + FORMAT_ALLELIC_DEPTH + " or " + FORMAT_ADE + ')';
    }
    return null;
  }
}
