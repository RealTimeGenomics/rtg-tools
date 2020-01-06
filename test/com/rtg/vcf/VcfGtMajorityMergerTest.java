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

import com.rtg.AbstractTest;
import com.rtg.vcf.header.VcfHeader;

/**
 * Tests the corresponding class.
 */
public class VcfGtMajorityMergerTest extends AbstractTest {

  private VcfRecord getRecord(String gt) {
    return VcfReaderTest.vcfLineToRecord("chr1\t100\t.\tG\tA,C\t.\tPASS\t.\tGT\t" + gt);
  }

  public void checkExpected(String expGt, String...inGt) {
    final VcfHeader h1 = new VcfHeader().addSampleName("sample1");
    final VcfHeader[] inHead = new VcfHeader[inGt.length];
    final VcfRecord[] inRecs = new VcfRecord[inGt.length];
    int i = 0;
    for (String gt : inGt) {
      inHead[i] = h1;
      inRecs[i++] = getRecord(gt);
    }
    VcfRecord[] mergedArr = new VcfGtMajorityMerger().setHeader(h1).mergeRecords(inRecs, inHead);
    assertEquals(1, mergedArr.length);
    final String gtStr = mergedArr[0].getFormat(VcfUtils.FORMAT_GENOTYPE).get(0);
    assertEquals(expGt, gtStr);
  }

  public void testGtMajorityMerge() {
    checkExpected("0/1", "1/0");
    checkExpected("0/1", "0/1", "1/0");
    checkExpected("./.", "1/1", "0/1");
    checkExpected("./.", "1/1", "0/1", "1/0", "1/0");
    checkExpected("0/1", "0/1", "1/1", "0/1", "1/0", "1/0");
    checkExpected("./.", "0/1", "1/1", ".", "1/0", "1/0");
    checkExpected("1/1", "1/1", "1/1", "1/1", "1/1", "1/0");
    checkExpected("./.", "1/1", "1/1", "1/1", "2/1", "1/0");
  }

}
