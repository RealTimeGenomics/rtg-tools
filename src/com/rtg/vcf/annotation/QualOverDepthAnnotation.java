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
 * Derived attribute giving QUAL divided by DP.
 */
public class QualOverDepthAnnotation extends AbstractDerivedAnnotation {

  /**
   * Constructor.
   */
  public QualOverDepthAnnotation() {
    super("QD", "QUAL / DP", AnnotationDataType.DOUBLE);
  }

  @Override
  public Double getValue(VcfRecord record, int sampleNumber) {
    final String squal = record.getQuality();
    if (!VcfRecord.MISSING.equals(squal)) {
      if (record.getInfo().containsKey(VcfUtils.INFO_COMBINED_DEPTH)) {
        // get dp from info DP field
        final String sdp = record.getInfo().get(VcfUtils.INFO_COMBINED_DEPTH).get(0);
        if (sdp != null && !VcfRecord.MISSING.equals(sdp)) {
          final int dp = Integer.parseInt(sdp);
          if (dp <= 0) {
            return Double.POSITIVE_INFINITY;
          }
          return Double.parseDouble(squal) / dp;
        }
      } else {
        final List<String> dps = record.getFormat(VcfUtils.FORMAT_SAMPLE_DEPTH);
        if (dps != null && dps.size() != 0) {
          // get dp from sum of DP in samples, as DP may not exist in INFO for single sample VCF
          int dp = 0;
          for (String sdp : dps) {
            if (!VcfRecord.MISSING.equals(sdp)) {
              dp += Integer.parseInt(sdp);
            }
          }
          if (dp <= 0) {
            return Double.POSITIVE_INFINITY;
          }
          return Double.parseDouble(squal) / dp;
        }
      }
    }
    return null;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    // QUAL column is not optional
    final String noCombinedDP = checkHeader(header, new String[]{VcfUtils.INFO_COMBINED_DEPTH}, null);
    final String noSampleDP = checkHeader(header, null, new String[]{VcfUtils.FORMAT_SAMPLE_DEPTH});
    // Only need one or the other, not both, but if neither is present, ask for combined depth column
    // as that is what this is technically supposed to run from
    return noCombinedDP != null && noSampleDP != null ? noCombinedDP : null;
  }
}
