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
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Implementation of the VCF format specified AC field.
 * Allele count in genotypes, for each alternative allele, in the same order as listed.
 */
public class AlleleCountInGenotypesAnnotation extends AbstractDerivedInfoAnnotation {

  /**
   * Constructor
   */
  public AlleleCountInGenotypesAnnotation() {
    super(new InfoField("AC", MetaType.INTEGER, VcfNumber.ALTS, "Allele count in genotypes, for each alternative allele, in the same order as listed"), null);
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final List<String> gtList = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    if (gtList == null || record.getAltCalls().isEmpty()) {
      return null;
    }
    final int[] ret = new int[record.getAltCalls().size()];
    for (final String gtStr : gtList) {
      final int[] gts = VcfUtils.splitGt(gtStr);
      for (final int gt : gts) {
        if (gt > 0) {
          ret[gt - 1]++;
        }
      }
    }
    return ret;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_GENOTYPE});
  }

  @Override
  public void annotate(VcfRecord rec) {
    final int[] vals = (int[]) getValue(rec, -1);
    if (vals != null) {
      final String[] vcfVals = new String[vals.length];
      for (int i = 0 ; i < vals.length; ++i) {
        vcfVals[i] = Integer.toString(vals[i]);
      }
      rec.setInfo(getName(), vcfVals);
    }
  }

}
