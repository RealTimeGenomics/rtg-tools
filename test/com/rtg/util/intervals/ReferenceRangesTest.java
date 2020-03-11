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

package com.rtg.util.intervals;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 */
public class ReferenceRangesTest extends TestCase {

  public void testRanges() {
    final ReferenceRanges.Accumulator<String> acc = new ReferenceRanges.Accumulator<>();
    for (int i = 0; i < 1000; ++i) {
      acc.addRangeData("sequence1", new SimpleRangeMeta<>(i * 100, i * 100 + 50, "region" + i));
      acc.addRangeData("sequence3", new SimpleRangeMeta<>(i * 100, i * 100 + 50, "region" + i));
    }

    final ReferenceRanges<String> ranges = acc.getReferenceRanges();
    assertTrue(ranges.containsSequence("sequence1"));
    assertFalse(ranges.containsSequence("sequence2"));
    assertTrue(ranges.containsSequence("sequence3"));
    assertTrue(ranges.get("sequence1").getRangeList().size() == 1000);
    assertTrue(ranges.get("sequence1").getFullRangeList().size() == 2001);

    final Map<String, Integer> ids = new HashMap<>();
    ids.put("sequence1", 42);
    ids.put("sequence2", 43);
    ids.put("sequence3", 44);
    ranges.setIdMap(ids);
    
    assertTrue(ranges.containsSequence(42));
    assertFalse(ranges.containsSequence(12));
    assertTrue(ranges.get(42).getRangeList().size() == 1000);

    final ReferenceRanges<String> ranges2 = ranges.forSequence("sequence2");
    assertFalse(ranges2.containsSequence("sequence1"));
    assertFalse(ranges2.containsSequence("sequence2"));
    assertFalse(ranges2.containsSequence("sequence3"));

    final ReferenceRanges<String> ranges1 = ranges.forSequence("sequence1");
    assertTrue(ranges1.containsSequence("sequence1"));
    assertFalse(ranges1.containsSequence("sequence2"));
    assertFalse(ranges1.containsSequence("sequence3"));
    assertTrue(ranges1.get(42).getRangeList().size() == 1000);

    assertNotNull(ranges1.get("sequence1"));
    assertNull(ranges1.get("sequence2"));
    assertNull(ranges1.get("sequence3"));

  }
}
