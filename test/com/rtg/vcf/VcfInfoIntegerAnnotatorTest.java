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

import com.rtg.vcf.annotation.AbstractDerivedInfoAnnotation;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class VcfInfoIntegerAnnotatorTest extends TestCase {

  private static final class DummyDerivedAnnotation extends AbstractDerivedInfoAnnotation {

    DummyDerivedAnnotation() {
      super(new InfoField("DUMMY", MetaType.INTEGER, VcfNumber.ONE, "DUMMY-DESC"));
    }

    @Override
    public Object getValue(VcfRecord record, int sampleNumber) {
      assertEquals(-1, sampleNumber);
      return 123;
    }

    @Override
    public String checkHeader(VcfHeader header) {
      return null;
    }
  }

  public void test() {
    final VcfInfoIntegerAnnotator ann = new VcfInfoIntegerAnnotator(new DummyDerivedAnnotation());
    final VcfHeader header = new VcfHeader();
    ann.updateHeader(header);
    ann.updateHeader(header); // Test that doing a second time doesn't break it / add extra
    assertEquals(1, header.getInfoLines().size());
    assertEquals("DUMMY", header.getInfoLines().get(0).getId());
    assertEquals("DUMMY-DESC", header.getInfoLines().get(0).getDescription());
    assertEquals(MetaType.INTEGER, header.getInfoLines().get(0).getType());
    assertEquals(1, header.getInfoLines().get(0).getNumber().getNumber());
    final VcfRecord rec = new VcfRecord("seq", 0, "A");
    ann.annotate(rec);
    ann.annotate(rec); // Test that doing a second time doesn't break it / add extra
    assertEquals(1, rec.getInfo().get("DUMMY").size());
    assertEquals("123", rec.getInfo().get("DUMMY").get(0));
    assertEquals("seq\t1\t.\tA\t.\t.\t.\tDUMMY=123", rec.toString());
  }
}
