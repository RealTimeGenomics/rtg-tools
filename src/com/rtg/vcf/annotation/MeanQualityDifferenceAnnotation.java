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
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Calculates field for difference in mean quality for called alleles
 */
public class MeanQualityDifferenceAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * constructor
   */
  public MeanQualityDifferenceAnnotation() {
    super(new FormatField("MEANQAD", MetaType.FLOAT, VcfNumber.ONE, "Difference between the mean alt quality and mean reference quality"));
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final String gtString = record.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sampleNumber);
    final int[] gts = VcfUtils.splitGt(gtString);
    if (gts.length < 2 || gts[0] == gts[1] || gts[0] == -1 || gts[1] == -1) {
      return null;
    }
    final String[] dps = StringUtils.split(record.getFormat(VcfUtils.FORMAT_ALLELIC_DEPTH).get(sampleNumber), ',');
    final String[] aqs = StringUtils.split(record.getFormat(VcfUtils.FORMAT_ALLELE_QUALITY).get(sampleNumber), ',');
    final int allele1;
    final int allele2;
    if (gts[0] > gts[1]) {
      allele2 = gts[0];
      allele1 = gts[1];
    } else {
      allele1 = gts[0];
      allele2 = gts[1];
    }
    final int dp1 = Integer.parseInt(dps[allele1]);
    final int dp2 = Integer.parseInt(dps[allele2]);
    if (dp1 == 0 || dp2 == 0) {
      return null;
    }
    final double meanaq1 = Double.parseDouble(aqs[allele1]) / dp1;
    final double meanaq2 = Double.parseDouble(aqs[allele2]) / dp2;
    if (allele1 == 0) {
      return meanaq1 - meanaq2;
    } else {
      return Math.abs(meanaq1 - meanaq2);
    }
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_GENOTYPE, VcfUtils.FORMAT_ALLELIC_DEPTH, VcfUtils.FORMAT_ALLELE_QUALITY});
  }
}
