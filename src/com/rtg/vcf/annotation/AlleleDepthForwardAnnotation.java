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

import java.util.ArrayList;
import java.util.List;

import com.rtg.util.StringUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Derived annotation for forward allele depth.
 */
public class AlleleDepthForwardAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * Constructor.
   */
  public AlleleDepthForwardAnnotation() {
    super(new FormatField("ADF", MetaType.INTEGER, VcfNumber.REF_ALTS, "Total allelic depths on the forward strand"), Formatter.DEFAULT);
  }

  @Override
  public Object getValue(final VcfRecord record, final int sampleNumber) {
    final ArrayList<String> perSampleAdf1 = record.getFormat("ADF1");
    final ArrayList<String> perSampleAdf2 = record.getFormat("ADF2");
    return sum(sampleNumber, perSampleAdf1, perSampleAdf2);
  }

  static Object sum(final int sampleNumber, final List<String> perSampleAd1, final List<String> perSampleAd2) {
    if (perSampleAd1 == null || perSampleAd2 == null
      || perSampleAd1.size() < sampleNumber + 1 || perSampleAd2.size() < sampleNumber + 1) {
      return null;
    }
    final String[] ad1 = StringUtils.split(perSampleAd1.get(sampleNumber), ',');
    final String[] ad2 = StringUtils.split(perSampleAd2.get(sampleNumber), ',');
    if (ad1.length != ad2.length) {
      return null;
    }
    final StringBuilder sb = new StringBuilder();
    for (int k = 0; k < ad1.length; ++k) {
      if (k > 0) {
        sb.append(',');
      }
      if (VcfUtils.MISSING_FIELD.equals(ad1[k]) || VcfUtils.MISSING_FIELD.equals(ad2[k])) {
        sb.append(VcfUtils.MISSING_VALUE);
      } else {
        sb.append(Integer.parseInt(ad1[k]) + Integer.parseInt(ad2[k]));
      }
    }
    return sb;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[] {"ADF1", "ADF2"});
  }
}
