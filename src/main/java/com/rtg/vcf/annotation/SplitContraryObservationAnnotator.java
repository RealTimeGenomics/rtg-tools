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
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * The counts (and fraction) of evidence that is considered contrary to the call made for this sample,
 * separated by whether the observations are in the original vs derived genomes.
 *
 * For example, in a somatic call of 0/0 -&gt; 1/0, the <code>OCOF</code>
 * value is the fraction of the somatic (1) allele in the normal sample.
 * These attributes are also applicable to de novo calls, where the evidence
 * of the parents for the de novo allele is considered contrary.
 * Usually a high <code>OCOF</code> value indicates an unreliable call.
 *
 * Similarly for pure somatic calling, or family calling, the <code>DCOC</code> is the
 * count of observations in the derived sample (tumor / child) which correspond to an
 * original allele (normal / parent) which is not supposed to be present.
 */
public class SplitContraryObservationAnnotator implements VcfAnnotator {

  private static final FormatField OCOC_FIELD = new FormatField("OCOC", MetaType.INTEGER, VcfNumber.ONE, "Contrary observations seen in original (as count)");
  private static final FormatField OCOF_FIELD = new FormatField("OCOF", MetaType.FLOAT, VcfNumber.ONE, "Contrary observations seen in original (as fraction of total)");
  private static final FormatField DCOC_FIELD = new FormatField("DCOC", MetaType.INTEGER, VcfNumber.ONE, "Contrary observations seen in derived (as count)");
  private static final FormatField DCOF_FIELD = new FormatField("DCOF", MetaType.FLOAT, VcfNumber.ONE, "Contrary observations seen in derived (as fraction of total)");

  private static final FormatField[] ALL =  {
    OCOC_FIELD, OCOF_FIELD,
    DCOC_FIELD, DCOF_FIELD,
  };

  protected final ContraryObservationCounter mCounter = new ContraryObservationCounter();

  @Override
  public void updateHeader(VcfHeader header) {
    mCounter.initSampleInfo(header);
    for (FormatField f : ALL) {
      header.ensureContains(f);
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    for (int i = 0; i < rec.getNumberOfSamples(); ++i) {
      final ContraryObservationCounter.Counts counts = mCounter.getCounts(rec, i);
      if (counts == null) {
        continue;
      }
      rec.setFormatAndSample(OCOC_FIELD.getId(), String.valueOf(counts.getOriginalContraryCount()), i);
      rec.setFormatAndSample(DCOC_FIELD.getId(), String.valueOf(counts.getDerivedContraryCount()), i);
      rec.setFormatAndSample(OCOF_FIELD.getId(), Utils.realFormat(counts.getOriginalContraryFraction(), 3), i);
      rec.setFormatAndSample(DCOF_FIELD.getId(), Utils.realFormat(counts.getDerivedContraryFraction(), 3), i);
    }
    for (FormatField f : ALL) {
      rec.padFormatAndSample(f.getId());
    }
  }
}
