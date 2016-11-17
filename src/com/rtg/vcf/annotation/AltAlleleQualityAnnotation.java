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
public class AltAlleleQualityAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * constructor
   */
  public AltAlleleQualityAnnotation() {
    super(new FormatField("QA", MetaType.FLOAT, VcfNumber.ONE, "Sum of quality of the alternate observations"));
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final String va = record.getFormatAndSample().get(VcfUtils.FORMAT_VARIANT_ALLELE).get(sampleNumber);
    if (va == null || va.equals(".")) {
      return null;
    }
    final int vaIndex = Integer.parseInt(va);
    final String aq = record.getFormat(VcfUtils.FORMAT_ALLELE_QUALITY).get(sampleNumber);
    if (aq == null) {
      return null;
    }
    final String[] aqs = StringUtils.split(aq, ',');
    return Double.parseDouble(aqs[vaIndex]);
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_VARIANT_ALLELE, VcfUtils.FORMAT_ALLELE_QUALITY});
  }
}
