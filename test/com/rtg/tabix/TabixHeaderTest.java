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
import java.util.Arrays;

import com.rtg.util.Resources;

import htsjdk.samtools.util.BlockCompressedInputStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class TabixHeaderTest extends TestCase {

  public void test() throws IOException {
    final TabixHeader th1;
    try (BlockCompressedInputStream is = new BlockCompressedInputStream(Resources.getResourceAsStream("com/rtg/tabix/resources/tabixmerge1.sam.gz.tbi"))) {
      th1 = TabixHeader.readHeader(is);
      assertEquals(4, th1.getNumSequences());
      checkOptions(th1.getOptions());
      assertTrue(Arrays.equals(new String[]{"simulatedSequence1", "simulatedSequence2", "simulatedSequence3", "simulatedSequence4"}, th1.getSequenceNamesUnpacked()));
    }
    final TabixHeader th2;
    try (BlockCompressedInputStream is2 = new BlockCompressedInputStream(Resources.getResourceAsStream("com/rtg/tabix/resources/tabixmerge2.sam.gz.tbi"))) {
      th2 = TabixHeader.readHeader(is2);
      assertEquals(5, th2.getNumSequences());
      checkOptions(th2.getOptions());
      assertTrue(Arrays.equals(new String[]{"simulatedSequence4", "simulatedSequence5", "simulatedSequence6", "simulatedSequence7", "simulatedSequence8"}, th2.getSequenceNamesUnpacked()));
      final TabixHeader merged = TabixHeader.mergeHeaders(th1, th2);
      assertEquals(8, merged.getNumSequences());
      checkOptions(th2.getOptions());
      assertTrue(Arrays.equals(new String[]{"simulatedSequence1", "simulatedSequence2", "simulatedSequence3", "simulatedSequence4", "simulatedSequence5", "simulatedSequence6", "simulatedSequence7", "simulatedSequence8"}, merged.getSequenceNamesUnpacked()));
    }
  }

  private void checkOptions(TabixIndexer.TabixOptions options) {
    assertEquals(0, options.mSkip);
    assertEquals(false, options.mZeroBased);
    assertEquals('@', options.mMeta);
    assertEquals(1, options.mFormat);
    assertEquals(2, options.mSeqCol);
    assertEquals(3, options.mStartCol);
    assertEquals(-1, options.mEndCol);

  }
}
