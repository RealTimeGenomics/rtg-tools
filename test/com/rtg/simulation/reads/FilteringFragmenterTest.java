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

package com.rtg.simulation.reads;

import java.io.IOException;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.simulation.DistributionSampler;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRegions;

import junit.framework.TestCase;

/**
 */
public class FilteringFragmenterTest extends TestCase {
  private static class MockMachine extends DummyMachineTest.MockMachine {
    Integer mLastStart;
    @Override
    public void processFragment(String id, int fragmentStart, byte[] data, int length) {
      mLastStart = fragmentStart;
    }
  }
  public void test() throws IOException {
    final ReferenceRegions bed = new ReferenceRegions();
    bed.add("foo", 20, 29);
    final DistributionSampler[] dist = {new DistributionSampler(1.0)};
    final SequencesReader[] readers = {ReaderTestUtils.getReaderDnaMemory(">foo" + StringUtils.LS + "ACGTACCCACAGAGATAGACACACGTAGATGACACAGCCATGTCCCGCCATAT")};
    final MockMachine m = new MockMachine();
    final FilteringFragmenter fragmenter = new FilteringFragmenter(bed, 23, dist, readers);
    fragmenter.setLengthChooser(new MinMaxGaussianSampler(2, 2));
    fragmenter.setMachine(m);
    fragmenter.emitFragment(2, 0, 0, "foo", 18);
    assertNull(m.mLastStart);
    fragmenter.emitFragment(2, 0, 0, "foo", 19);
    assertEquals(Integer.valueOf(19), m.mLastStart);
    fragmenter.emitFragment(2, 0, 0, "foo", 29);
    assertEquals(Integer.valueOf(19), m.mLastStart);
    fragmenter.emitFragment(2, 0, 0, "foo", 28);
    assertEquals(Integer.valueOf(28), m.mLastStart);
  }
}
