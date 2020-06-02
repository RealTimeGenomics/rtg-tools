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

import static com.rtg.util.StringUtils.TAB;

import junit.framework.TestCase;

/**
 */
public class VcfSubsetParserTest extends TestCase {

  static final String[] GOOD_NO_SAMPLES = {
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7",
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34",
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34   0|0:34",
  };
  static final String[] BAD_NO_SAMPLES = {
    "chr1   123    foo  G    A    29    PASS",
  };

  public void testNoSamples() {
    for (String recTxt : GOOD_NO_SAMPLES) {
      final String rec = recTxt.replaceAll(" +", TAB);
      String[] fields = VcfSubsetParser.split(rec, new int[0]);
      assertEquals(8, fields.length);
      assertEquals("NS=3;DP=7", fields[7]);
    }
    for (String recTxt : BAD_NO_SAMPLES) {
      final String rec = recTxt.replaceAll(" +", TAB);
      try {
        VcfSubsetParser.split(rec, new int[0]);
        fail("Expected failure on record: " + rec);
      } catch (VcfFormatException e) {
        //System.err.println(e.getMessage());
      }
    }
  }

  static final String[] GOOD_SAMPLES = {
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34",
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34   0|0:35",
    "chr1   123    foo  G    A    29    PASS    NS=3;DP=7    GT:GQ   0|0:34   0|0:35   0|0:36",
  };

  public void testOneSampleRecords() {
    for (String recTxt : GOOD_SAMPLES) {
      final String rec = recTxt.replaceAll(" +", TAB);
      checkSelection(rec, new int[] {0}, new String[] {"0|0:34"});
    }
  }

  public void testMultiSampleRecords() {
    String recTxt = GOOD_SAMPLES[2];
    final String rec = recTxt.replaceAll(" +", TAB);
    checkSelection(rec, new int[] {1}, new String[] {"0|0:35"});
    checkSelection(rec, new int[] {2}, new String[] {"0|0:36"});
    checkSelection(rec, new int[] {0, 1}, new String[] {"0|0:34", "0|0:35"});
    checkSelection(rec, new int[] {0, 2}, new String[] {"0|0:34", "0|0:36"});
    checkSelection(rec, new int[] {1, 2}, new String[] {"0|0:35", "0|0:36"});
    checkSelection(rec, new int[] {0, 1, 2}, new String[] {"0|0:34", "0|0:35", "0|0:36"});
  }

  private void checkSelection(String rec, int[] samplesToKeep, String[] expected) {
    String[] fields = VcfSubsetParser.split(rec, samplesToKeep);
    assertEquals(9 + samplesToKeep.length, fields.length);
    //System.err.println(Arrays.toString(fields));
    int f = 9;
    for (String expect : expected) {
      assertEquals(expect, fields[f]);
      f++;
    }
  }
}
