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

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Ploidy derived attribute
 */
public class PloidyAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * Constructor
   */
  public PloidyAnnotation() {
    super(new FormatField("PD", MetaType.STRING /* MetaType.CHARACTER */, VcfNumber.ONE, "Ploidy of sample. 'h'=>haploid, 'd'=>diploid"));
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final ArrayList<String> sampleValues = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    final String value;
    if (sampleValues == null || sampleValues.size() < (sampleNumber + 1)) {
      value = null;
    } else {
      final String sValue = sampleValues.get(sampleNumber);
      if (sValue.contains(VcfRecord.MISSING)) {
        value = null;
      } else {
        switch (VcfUtils.getValidGt(record, sampleNumber).length) {
          case 1:
            value = "h";
            break;
          case 2:
            value = "d";
            break;
          default:
            value = "p";
            break;
        }
      }
    }
    return value;
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_GENOTYPE});
  }

}
