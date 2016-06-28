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

import java.util.List;

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Derived attribute giving GQ divided by DP for a sample.
 */
public class GenotypeQualityOverDepthAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * Constructor.
   */
  public GenotypeQualityOverDepthAnnotation() {
    super("GQD", "GQ / DP for a single sample", AnnotationDataType.DOUBLE);
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final List<String> dps = record.getFormat(VcfUtils.FORMAT_SAMPLE_DEPTH);
    final List<String> gqs = record.getFormat(VcfUtils.FORMAT_GENOTYPE_QUALITY);
    if (dps != null && gqs != null && dps.size() > sampleNumber && dps.size() == gqs.size()) {
      final String dpVal = dps.get(sampleNumber);
      final String gqVal = gqs.get(sampleNumber);
      if (!VcfRecord.MISSING.equals(dpVal) && !VcfRecord.MISSING.equals(gqVal)) {
        final int dp = Integer.parseInt(dpVal);
        if (dp <= 0) {
          return Double.POSITIVE_INFINITY;
        }
        return Double.valueOf(gqVal) / dp;
      }
    }
    return null;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_SAMPLE_DEPTH, VcfUtils.FORMAT_GENOTYPE_QUALITY});
  }
}
