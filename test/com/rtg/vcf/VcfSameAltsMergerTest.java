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

package com.rtg.vcf;

import java.util.Collections;

import com.rtg.AbstractTest;
import com.rtg.vcf.header.VcfHeader;

/**
 * Tests the corresponding class.
 */
public class VcfSameAltsMergerTest extends AbstractTest {

  private VcfRecord getRecord(String alts) {
    return VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\t" + alts + "\t.\tPASS\t.\tGT\t0/1");
  }

  public void checkExpected(int expLen, String... inAlts) {
    final VcfHeader h1 = new VcfHeader();
    final VcfHeader[] inHead = new VcfHeader[inAlts.length];
    final VcfRecord[] inRecs = new VcfRecord[inAlts.length];
    int i = 0;
    for (String alts : inAlts) {
      inHead[i] = new VcfHeader();
      inHead[i].addSampleName("sample" + i);
      h1.addSampleName("sample" + i);
      inRecs[i++] = getRecord(alts);
    }
    VcfRecord[] mergedArr = new VcfSameAltsMerger().mergeRecords(inRecs, inHead, h1, Collections.emptySet(), false);
    assertEquals(expLen, mergedArr.length);
  }

  public void testSameAltsMerge() {
    checkExpected(1, ".");
    checkExpected(1, "T", "T");
    checkExpected(2, "T", "C");
    checkExpected(2, "T", "T,C");
    checkExpected(2, "C,T", "T,C"); // Considered a different set of alts, since GT ids have changed
  }

}
