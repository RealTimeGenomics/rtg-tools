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

package com.rtg.vcf;

import com.rtg.vcf.annotation.AbstractDerivedFormatAnnotation;
import com.rtg.vcf.annotation.AnnotationDataType;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 */
public class VcfFormatStringAnnotatorTest extends TestCase {

  private static final class DummyDerivedAnnotation extends AbstractDerivedFormatAnnotation {
    public DummyDerivedAnnotation() {
      super("DUMMY", "DUMMY-DESC", AnnotationDataType.STRING);
    }
    @Override
    public Object getValue(VcfRecord record, int sampleNumber) {
      return Integer.toString(sampleNumber);
    }
    @Override
    public String checkHeader(VcfHeader header) {
      return null;
    }
  }

  public void testDummyString() {
    final VcfFormatStringAnnotator ann = new VcfFormatStringAnnotator(new DummyDerivedAnnotation());
    final VcfHeader header = new VcfHeader();
    ann.updateHeader(header);
    ann.updateHeader(header); // Test that doing a second time doesn't break it / add extra
    assertEquals(1, header.getFormatLines().size());
    assertEquals("DUMMY", header.getFormatLines().get(0).getId());
    assertEquals("DUMMY-DESC", header.getFormatLines().get(0).getDescription());
    assertEquals(MetaType.STRING, header.getFormatLines().get(0).getType());
    assertEquals(1, header.getFormatLines().get(0).getNumber().getNumber());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    rec.setNumberOfSamples(2);
    ann.annotate(rec);
    ann.annotate(rec); // Test that doing a second time doesn't break it / add extra
    assertEquals(2, rec.getFormatAndSample().get("DUMMY").size());
    assertEquals("0", rec.getFormatAndSample().get("DUMMY").get(0));
    assertEquals("1", rec.getFormatAndSample().get("DUMMY").get(1));
    assertEquals("seq\t1\t.\tA\t.\t.\t.\t.\tDUMMY\t0\t1", rec.toString());
  }
}
