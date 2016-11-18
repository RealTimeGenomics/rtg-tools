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

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * The fraction of evidence that is considered contrary to the call made for this sample.
 * For example, in a somatic call of 0/0 -&gt; 1/0, the <code>COF</code>
 * value is the fraction of the somatic (1) allele in the normal sample.
 * These attributes are also applicable to de novo calls, where the evidence
 * of the parents for the de novo allele is considered contrary.
 * Usually a high <code>COF</code> value indicates an unreliable call.
 */
public class ContraryObservationFractionAnnotation extends AbstractDerivedFormatAnnotation {

  static final FormatField COF_FIELD = new FormatField("COF", MetaType.FLOAT, VcfNumber.ONE, "Contrary observation fraction");

  protected final ContraryObservationCounter mCounter = new ContraryObservationCounter();

  protected ContraryObservationFractionAnnotation(FormatField f) {
    super(f);
  }

  /**
   * Construct a new contrary observation fraction format annotation.
   */
  public ContraryObservationFractionAnnotation() {
    super(COF_FIELD);
  }

  @Override
  public Object getValue(final VcfRecord record, final int sampleNumber) {
    final ContraryObservationCounter.Counts counts = mCounter.getCounts(record, sampleNumber);
    return (counts == null) ? null : counts.getContraryFraction();
  }

  @Override
  public String checkHeader(VcfHeader header) {
    mCounter.initSampleInfo(header);
    // Need either SS (for somatic caller) or DN (for family caller)
    final String somaticStatus = checkHeader(header, null, new String[]{VcfUtils.FORMAT_SOMATIC_STATUS});
    final String denovoStatus = checkHeader(header, null, new String[]{VcfUtils.FORMAT_DENOVO});
    if (somaticStatus != null && denovoStatus != null) {
      return "Derived annotation COC missing required fields in VCF header (FORMAT fields: SS or DN)";
    }
    return checkHeader(header, null, new String[]{VcfUtils.FORMAT_ALLELIC_DEPTH, VcfUtils.FORMAT_GENOTYPE});
  }

}
