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

package com.rtg.tabix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 * Test class
 */
public class BrLineReaderTest extends TestCase {

  private static final String EXPECTED = ""
      + "simulatedSequence19\t583\t.\tA\tT\t.\tPASS\t.\tGT:DP:RE:GQ:RS\t0/1:23:0.458:185.0:A,11,0.219,T,12,0.239" + StringUtils.LS
      + "simulatedSequence19\t637\t.\tG\tC\t.\tPASS\t.\tGT:DP:RE:GQ:RS\t1/0:27:0.537:53.0:C,7,0.139,G,20,0.398" + StringUtils.LS
      + "simulatedSequence19\t737\t.\tG\tC\t.\tPASS\t.\tGT:DP:RE:GQ:RS\t1/1:27:0.537:74.0:C,26,0.517,T,1,0.020" + StringUtils.LS;

  public void testSomeMethod() throws IOException {
    try (BrLineReader br = new BrLineReader(new BufferedReader(new StringReader(EXPECTED)))) {
      int count = 0;
      while (br.readLine() != null) {
        count++;
      }
      assertEquals(3, count);
    }
  }

}
