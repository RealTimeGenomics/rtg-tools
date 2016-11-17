/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import com.rtg.util.Utils;
import com.rtg.vcf.VcfAnnotator;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;

/**
 * The counts (and fraction) of evidence that is considered contrary to the call made for this sample.
 * For example, in a somatic call of 0/0 -&gt; 1/0, the <code>COF</code>
 * value is the fraction of the somatic (1) allele in the normal sample.
 * These attributes are also applicable to de novo calls, where the evidence
 * of the parents for the de novo allele is considered contrary.
 * Usually a high <code>COF</code> value indicates an unreliable call.
 */
public class ContraryObservationAnnotator implements VcfAnnotator {

  private static final String FORMAT_COF = ContraryObservationFractionAnnotation.COF_FIELD.getId();
  private static final String FORMAT_COC = ContraryObservationCountAnnotation.COC_FIELD.getId();

  protected final ContraryObservationCounter mCounter = new ContraryObservationCounter();

  @Override
  public void updateHeader(VcfHeader header) {
    mCounter.initSampleInfo(header);
    header.ensureContains(ContraryObservationCountAnnotation.COC_FIELD);
    header.ensureContains(ContraryObservationFractionAnnotation.COF_FIELD);
  }

  @Override
  public void annotate(VcfRecord rec) {
    for (int i = 0; i < rec.getNumberOfSamples(); i++) {
      final ContraryObservationCounter.Counts counts = mCounter.getCounts(rec, i);
      if (counts == null) {
        continue;
      }
      rec.setFormatAndSample(FORMAT_COC, "" + counts.getContraryCount(), i);
      final Double cof = counts.getContraryFraction();
      if (cof != null) {
        rec.setFormatAndSample(FORMAT_COF, Utils.realFormat(cof, 3), i);
      }
    }
    rec.padFormatAndSample(FORMAT_COF);
    rec.padFormatAndSample(FORMAT_COC);
  }
}
