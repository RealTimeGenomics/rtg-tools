/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import java.io.IOException;

import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class DecomposingVcfIteratorTest extends TestCase {

  public void test() throws IOException {
    final VcfHeader h = new VcfHeader();
    final VcfIterator r = new DecomposingVcfIterator(new VcfFilterIteratorTest.ArrayVcfIterator(h,
      new VcfRecord("chr1", 10, "A").addAltCall("<DEL>"),
      new VcfRecord("chr1", 20, "T").addAltCall("TT"),
      new VcfRecord("chr1", 30, "TTTTT").addAltCall("TTTTA"),
      new VcfRecord("chr1", 30, "TTGT").addAltCall("TGC")
    ), null);
    assertEquals(h, r.getHeader());
    final String[] out = {
      "chr1 11 . A <DEL> . . .",
      "chr1 21 . T TT . . .",
      "chr1 31 . TT T . . ORP=31;ORL=4",
      "chr1 34 . T C . . ORP=31;ORL=4",
      "chr1 35 . T A . . ORP=31;ORL=5",
    };
    for (String o : out) {
      assertTrue(r.hasNext());
      assertEquals(o, r.next().toString().replaceAll("\t", " "));
    }
    assertFalse(r.hasNext());
  }
}
