/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
 * Derived annotation categorizing the sample genotype as one of:
 * homozygous reference, homozygous alternate, heterozygous reference, heterozygous alternate.
 *
 * Haploid or half-calls are considered homozygous.
 */
public class RefAltAnnotation extends AbstractDerivedFormatAnnotation {

  /**
   * Constructor
   */
  public RefAltAnnotation() {
    super(new FormatField("RA", MetaType.STRING, VcfNumber.ONE, "Reference-alternate type of genotype. 'RR'=>hom ref, 'RA'=>het ref, 'AA'=>hom alt, 'AB'=>het alt"));
  }

  @Override
  public Object getValue(VcfRecord record, int sampleNumber) {
    final ArrayList<String> sampleValues = record.getFormat(VcfUtils.FORMAT_GENOTYPE);
    final Object value;
    if (sampleValues == null || sampleValues.size() < (sampleNumber + 1)) {
      value = null;
    } else {
      final String sValue = sampleValues.get(sampleNumber);
      if (VcfRecord.MISSING.equals(sValue)) {
        value = null;
      } else {
        final int[] gtArray = VcfUtils.getValidGt(record, sampleNumber);
        value = getCode(gtArray);
      }
    }
    return value;
  }

  protected static String getCode(int... gtArray) {
    int first = -1;
    int i = 0;
    for (; i < gtArray.length && first == -1; i++) {
      first = gtArray[i];
    }
    if (first == -1) {
      return null;
    }
    boolean hasRef = first == 0;
    boolean hom = true;
    boolean multi = false;
    int alt = -1;
    for (; i < gtArray.length; i++) {
      final int allele = gtArray[i];
      if (allele != -1) {
        if (allele == 0) {
          hasRef = true;
        } else if (alt == -1) {
          alt = allele;
        } else if (allele != alt) {
          multi = true;
        }
        if (allele != first) {
          hom = false;
        }
      }
    }
    return hom
      ? hasRef ? "RR" : "AA"
      : (hasRef && !multi) ? "RA" : "AB";
  }

  @Override
  public String checkHeader(VcfHeader header) {
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_GENOTYPE});
  }

}
