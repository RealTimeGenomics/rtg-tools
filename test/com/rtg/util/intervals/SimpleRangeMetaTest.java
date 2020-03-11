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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class SimpleRangeMetaTest extends TestCase {

  public void testSingle() {
    SimpleRangeMeta<String> r = new SimpleRangeMeta<>(0, 1, "blah");
    assertEquals("blah", r.getMeta());
    assertTrue(r.contains(0));
    assertFalse(r.contains(1));
    assertFalse(r.contains(-1));
    assertEquals("1-1", r.toString());

    r = new SimpleRangeMeta<>(5, 20, "a");
    assertFalse(r.contains(0));
    assertTrue(r.contains(5));
    assertFalse(r.contains(20));

    r = new SimpleRangeMeta<>(1, 2, null);
    assertEquals("2-2", r.toString());
    assertEquals(1, r.getStart());
    assertEquals(2, r.getEnd());
    assertEquals(null, r.getMeta());
  }

  public void testList() {
    RangeMeta<List<String>> r2 = new SimpleRangeMeta<>(0, 1, Arrays.asList("blah", "boo"));
    assertEquals(2, r2.getMeta().size());
    assertEquals("blah", r2.getMeta().get(0));
    assertEquals("boo", r2.getMeta().get(1));
  }
}


