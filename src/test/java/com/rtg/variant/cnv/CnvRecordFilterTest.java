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
package com.rtg.variant.cnv;

import java.util.Collections;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class CnvRecordFilterTest extends TestCase {

  public void test() {
    Diagnostic.setLogStream();
    final CnvRecordFilter f = new CnvRecordFilter(Collections.singletonList("pretend"), true);
    f.setHeader(null);
    final VcfRecord record = new VcfRecord("pretend", 42, "A");
    assertFalse(f.accept(record)); // Not SV
    record.setInfo(VcfUtils.INFO_END, "42");
    assertFalse(f.accept(record)); // Not SV
    record.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertTrue(f.accept(record)); // SV with END

    final VcfRecord record2 = new VcfRecord("pretend2", 42, "A");
    record2.setInfo(VcfUtils.INFO_END, "42");
    record2.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertFalse(f.accept(record2)); // Overlap
  }

  public void testNoEnd() {
    Diagnostic.setLogStream();
    final CnvRecordFilter f = new CnvRecordFilter(Collections.singletonList("pretend"), true);
    final VcfRecord record = new VcfRecord("pretend", 42, "A");
    record.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertFalse(f.accept(record));
  }

  public void testOverlap() {
    Diagnostic.setLogStream();
    final CnvRecordFilter f = new CnvRecordFilter(Collections.singletonList("pretend"), true);
    final VcfRecord record = new VcfRecord("pretend", 42, "A");
    record.setInfo(VcfUtils.INFO_END, "48");
    record.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertTrue(f.accept(record));

    final VcfRecord record2 = new VcfRecord("pretend", 46, "A");
    record2.setInfo(VcfUtils.INFO_END, "49");
    record2.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertFalse(f.accept(record2));

    final VcfRecord record3 = new VcfRecord("pretend", 47, "A"); // Since according to VCF spec, SV records include the base BEFORE the event
    record3.setInfo(VcfUtils.INFO_END, "49");
    record3.setInfo(VcfUtils.INFO_SVTYPE, CnaType.DEL.toString());
    assertTrue(f.accept(record3));
  }

}
