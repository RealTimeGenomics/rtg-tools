/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.cnv.cnveval;

import com.rtg.util.intervals.Range;
import com.rtg.variant.cnv.CnaType;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

public class CnaVariantTest extends TestCase {

  static VcfRecord makeRecord(String chr, int start, int end, String svtype) {
    final VcfRecord record = new VcfRecord(chr, start, "A");
    record.addAltCall("<" + svtype + ">");
    record.setNumberOfSamples(0);
    record.setInfo(VcfUtils.INFO_SVTYPE, svtype);
    record.setInfo(VcfUtils.INFO_END, String.valueOf(end + 1));
    return record;
  }

  public void test() {
    VcfRecord rec = makeRecord("foo", 123, 100123, "DEL");

    CnaVariant var = new CnaVariant(new Range(0, 124), rec);
    assertFalse(var.isCorrect());
    assertEquals(CnaType.DEL, var.cnaType());
    assertEquals(CnaVariant.SpanType.PARTIAL, var.spanType());

    var.setCorrect(true);
    assertTrue(var.isCorrect());

    var = new CnaVariant(new Range(0, 200124), rec);
    assertEquals(CnaVariant.SpanType.PARTIAL, var.spanType());

    var = new CnaVariant(new Range(50123, 100000), rec);
    assertEquals(CnaVariant.SpanType.FULL, var.spanType());

    var = new CnaVariant(new Range(100122, 200124), rec);
    assertEquals(CnaVariant.SpanType.PARTIAL, var.spanType());

    rec = makeRecord("foo", 123, 100123, "DUP");
    var = new CnaVariant(new Range(0, 124), rec);
    assertEquals(CnaType.DUP, var.cnaType());

    rec = makeRecord("foo", 123, 100123, "foo");
    var = new CnaVariant(new Range(0, 124), rec);
    assertEquals(CnaType.NONE, var.cnaType());
  }
}
