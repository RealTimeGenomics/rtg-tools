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

package com.rtg.vcf.eval;

import com.rtg.vcf.VcfReaderTest;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

import junit.framework.TestCase;

public class ExpressionRocFilterTest extends TestCase {

  private static final VcfRecord HOMOZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS . GT 1/1".replaceAll(" ", "\t"));
  private static final VcfRecord HETEROZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS . GT 0/1".replaceAll(" ", "\t"));
  private static final VcfRecord IDENTITY_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A . 20.0 PASS . GT 0/0".replaceAll(" ", "\t"));

  private static final VcfRecord COMPLEX_HOMOZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS XRX GT 1/1".replaceAll(" ", "\t"));
  private static final VcfRecord COMPLEX_HETEROZYGOUS_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A C 20.0 PASS XRX GT 0/1".replaceAll(" ", "\t"));
  private static final VcfRecord COMPLEX_IDENTITY_RECORD = VcfReaderTest.vcfLineToRecord("chr1 250 . A . 20.0 PASS XRX GT 0/0".replaceAll(" ", "\t"));

  private boolean accept(RocFilter ff, VcfRecord rec) {
    return ff.accept(rec, VcfUtils.getValidGt(rec, 0));
  }

  public void testExpression() {
    final RocFilter f = new ExpressionRocFilter("my_xrx", "has(INFO.XRX)");
    final VcfHeader header = new VcfHeader();
    header.addInfoField(new InfoField("XRX", MetaType.FLAG, VcfNumber.ONE, "Complex called"));
    f.setHeader(header);
    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_HOMOZYGOUS_RECORD));
    assertTrue(accept(f, COMPLEX_IDENTITY_RECORD));

    assertFalse(accept(f, HETEROZYGOUS_RECORD));
    assertFalse(accept(f, HOMOZYGOUS_RECORD));
    assertFalse(accept(f, IDENTITY_RECORD));

    f.setHeader(header);
    assertTrue(accept(f, COMPLEX_HETEROZYGOUS_RECORD));
  }
}
