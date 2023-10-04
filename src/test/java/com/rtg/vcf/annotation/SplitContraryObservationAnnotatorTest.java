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

import java.io.IOException;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class SplitContraryObservationAnnotatorTest extends AbstractNanoTest {

  public void testSomaticCase() throws IOException {
    final SplitContraryObservationAnnotator an = new SplitContraryObservationAnnotator();

    final VcfHeader header = ContraryObservationCounterTest.makeHeaderSomatic();
    an.updateHeader(header);
    assertNotNull(header.getFormatField("OCOC"));
    assertNotNull(header.getFormatField("OCOF"));
    assertNotNull(header.getFormatField("DCOC"));
    assertNotNull(header.getFormatField("DCOF"));

    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.addAltCall("G");
    rec.setNumberOfSamples(2);
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/0");
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "0/1");
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, VcfUtils.MISSING_FIELD);
    rec.addFormatAndSample(VcfUtils.FORMAT_SOMATIC_STATUS, "2");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "9,1");
    rec.addFormatAndSample(VcfUtils.FORMAT_ALLELIC_DEPTH, "6,6");

    an.annotate(rec);
    mNano.check("scoc_cof_rec.vcf", rec.toString());
  }
}
