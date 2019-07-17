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

import com.rtg.util.TestUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class CnaTypeTest extends TestCase {

  public void test() {
    TestUtils.testEnum(CnaType.class, "[DEL, DUP, NONE]");
    assertEquals("SVTYPE", VcfUtils.INFO_SVTYPE);
    assertEquals("END", VcfUtils.INFO_END);
  }

  public void testFromVcf() {
    final VcfRecord rec = new VcfRecord("seq1", 42, "A");
    assertEquals(CnaType.NONE, CnaType.valueOf(rec));
    rec.setInfo("SVTYPE", "DUP");
    assertEquals(CnaType.DUP, CnaType.valueOf(rec));
    final VcfRecord rec2 = new VcfRecord("seq1", 42, "A");
    rec2.setInfo("SVTYPE", "DEL");
    assertEquals(CnaType.DEL, CnaType.valueOf(rec2));
    final VcfRecord rec3 = new VcfRecord("seq1", 42, "A");
    rec3.setInfo("SVTYPE", "FOO");
    assertEquals(CnaType.NONE, CnaType.valueOf(rec3));
  }
}
