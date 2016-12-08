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

import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.Resources;

import htsjdk.samtools.util.BlockCompressedInputStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class SamPositionReaderTest extends TestCase {

  private static final String[] EXP_REF_NAME = {"gi|89161203|ref|NC_000022.9|NC_000022", "*"};
  private static final int[] ENTRIES = {15, 15};
  private static final int[][] START = {new int[] {14430018, 14430080, 14430108, 14430124, 14430131,
                                        14430133, 14430187, 14430210, 14430243, 14430246,
                                        14430251, 14430255, 14430302, 14430309, 14430328},
                                        new int[] {0, 0, 0, 0, 0,
                                         0, 0, 0, 0, 0,
                                         0, 0, 0, 0, 0}
  };

  private static final int[][] LENGTH = {new int[] {100, 100, 100, 100, 100,
                                                    100, 100, 100, 100, 100,
                                                    100, 100, 100, 100, 100},
                                                    new int[] {100, 100, 100, 100, 100,
                                                    100, 100, 100, 100, 100,
                                                    100, 100, 100, 100, 100}
  };

  private static final int[][] VIRTUAL_OFFSETS = {new int[] {312, 631, 951, 1271, 1591,
                                                              1911, 2232, 2551, 2873, 3193,
                                                              3515, 3835, 4157, 4477, 4798},
                                                              new int[] {5116, 5338, 5567, 5790, 6019,
                                                              6241, 6463, 6693, 6922, 7145,
                                                              7368, 7592, 7815, 8038, 8261}
  };

  private static final int[][] VIRTUAL_OFFSET_ENDS = {new int[] {631, 951, 1271, 1591, 1911,
                                                              2232, 2551, 2873, 3193, 3515,
                                                              3835, 4157, 4477, 4798, 5116},
                                                              new int[] {5338, 5567, 5790, 6019, 6241,
                                                              6463, 6693, 6922, 7145, 7368,
                                                              7592, 7815, 8038, 8261, 100139008}
  };

  private static final int[] BINS = {5561, 4681};

  public void testSomeMethod() throws IOException {
    try (InputStream is = Resources.getResourceAsStream("com/rtg/sam/resources/mixed.sam.gz")) {
      final SamPositionReader spr = new SamPositionReader(new BlockCompressedLineReader(new BlockCompressedInputStream(is)), 0);
      try {
        int ref = 0;
        int i = 0;
        while (spr.hasNext()) {
          spr.next();
          if (i >= ENTRIES[ref]) {
            i = 0;
            ++ref;
          }
          assertEquals(EXP_REF_NAME[ref], spr.getReferenceName());
          assertEquals(ref, spr.getReferenceId());
          assertEquals(START[ref][i], spr.getStartPosition());
          assertEquals(LENGTH[ref][i], spr.getLengthOnReference());
          assertEquals(BINS[ref], spr.getBinNum());
          assertEquals(VIRTUAL_OFFSETS[ref][i], spr.getVirtualOffset());
          assertEquals(VIRTUAL_OFFSET_ENDS[ref][i], spr.getNextVirtualOffset());
          assertTrue(spr.hasReference());
          assertTrue(spr.hasCoordinates());
          assertFalse(spr.isUnmapped());
          ++i;
        }
      } finally {
        spr.close();
      }
    }
  }
}
