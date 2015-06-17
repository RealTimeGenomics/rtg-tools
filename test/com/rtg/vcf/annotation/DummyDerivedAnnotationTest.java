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
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

/**
 */
public class DummyDerivedAnnotationTest extends TestCase {

  private static final class DummyDerivedAnnotation extends AbstractDerivedAnnotation {

    public DummyDerivedAnnotation() {
      super("DUMMY", "DUMMY-DESC", AnnotationDataType.DOUBLE);
    }

    @Override
    public Object getValue(VcfRecord record, int sampleNumber) {
      return 0.0;
    }

    @Override
    public String checkHeader(VcfHeader header) {
      return null;
    }
  }

  public void testDerivedAnnotation() {
    final AbstractDerivedAnnotation ann = new DummyDerivedAnnotation();
    assertEquals("DUMMY", ann.getName());
    assertEquals("DUMMY-DESC", ann.getDescription());
    assertEquals(AnnotationDataType.DOUBLE, ann.getType());
  }

  public void testCheckHeader() {
    final AbstractDerivedAnnotation ann = new DummyDerivedAnnotation();
    assertEquals("Derived annotation DUMMY missing required fields in VCF header (INFO fields: II) (FORMAT fields: FF)", ann.checkHeader(null, new String[]{"II"}, new String[] {"FF"}));
    final VcfHeader header = new VcfHeader();
    header.addInfoField("II", MetaType.INTEGER, new VcfNumber("1"), "Info Field");
    header.addFormatField("FF", MetaType.INTEGER, new VcfNumber("1"), "Format Field");
    final String res = ann.checkHeader(header, new String[]{"II"}, new String[] {"FF"});
    assertNull(res, res);
  }
}
