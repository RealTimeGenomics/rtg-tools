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

import com.rtg.util.StringUtils;
import com.rtg.util.array.ArrayUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * The fraction of evidence that is supporting the somatic variant allele for this sample.
 */
public class VariantAllelicFractionAnnotation extends AbstractDerivedFormatAnnotation {

  private static final String FORMAT_ADE = "ADE";
  private static final String FORMAT_VA = "VA";

  /**
   * Construct a new contrary observation fraction format annotation.
   */
  public VariantAllelicFractionAnnotation() {
    super(new FormatField("VAF", MetaType.FLOAT, VcfNumber.ONE, "Variant Allelic Fraction"));
  }

  private double[] ad(final double[] res, final VcfRecord record, final int sample) {
    final String ad = record.getSampleString(sample, FORMAT_ADE);
    if (ad != null && !VcfUtils.MISSING_FIELD.equals(ad)) {
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
    final Integer va = record.getSampleInteger(sampleNumber, FORMAT_VA);
    if (va == null) {
      return null;
    }
    final double[] ad = ad(record, sampleNumber);
    final double sum = ArrayUtils.sum(ad);
    if (sum == 0) {
      return null;
    }
    assert va <= ad.length && va > 0;
    final double vac = ad[va];
    assert sum > 0;
    assert vac > 0;
    final double vaf = vac / sum;
    return vaf;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    // Need both VA and ADE
    return checkHeader(header, null, new String[]{FORMAT_ADE, FORMAT_VA});
  }

}
