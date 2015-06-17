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
package com.rtg.sam;

import com.rtg.util.IntegerOrPercentage;

import junit.framework.TestCase;

/**
 * Test class.
 */
public class SamFilterParamsTest extends TestCase {

  public void testDefaults() {
    // Default behaviour should be to not filter any records.
    final SamFilterParams.SamFilterParamsBuilder builder = SamFilterParams.builder();
    assertNotNull(builder);
    final SamFilterParams params = builder.create();
    assertNotNull(params);
    assertEquals(-1, params.maxAlignmentCount());
    assertEquals(null, params.maxMatedAlignmentScore());
    assertEquals(null, params.maxUnmatedAlignmentScore());
    assertEquals(-1, params.minMapQ());
    assertFalse(params.findAndRemoveDuplicates());
    assertEquals(0, params.requireSetFlags());
    assertEquals(0, params.requireUnsetFlags());
    assertFalse(params.excludeUnmated());
  }

  public void testActual() {
    final SamFilterParams p = SamFilterParams.builder()
      .minMapQ(10)
      .maxAlignmentCount(42)
      .maxMatedAlignmentScore(new IntegerOrPercentage(43))
      .maxUnmatedAlignmentScore(new IntegerOrPercentage(44))
      .excludeMated(true)
      .excludeUnmated(true)
      .excludeUnmapped(false)
      .excludeDuplicates(true)
      .create();
    assertEquals("SamFilterParams minMapQ=10 maxAlignmentCount=42 maxMatedAlignmentScore=43 maxUnmatedAlignmentScore=44 excludeUnmated=" + true + " excludeUnplaced=" + false + " requireSetFlags=0 requireUnsetFlags=1026 regionTemplate=" + null, p.toString());
    assertEquals(42, p.maxAlignmentCount());
    assertEquals(new IntegerOrPercentage(43), p.maxMatedAlignmentScore());
    assertEquals(new IntegerOrPercentage(44), p.maxUnmatedAlignmentScore());
    assertEquals(1026, p.requireUnsetFlags());
    assertEquals(0, p.requireSetFlags());
    assertTrue(p.excludeUnmated());
  }

  public void testActual2() {
    final SamFilterParams p = SamFilterParams.builder()
      .maxUnmatedAlignmentScore(new IntegerOrPercentage(44))
      .excludeUnmated(true)
      .excludeUnmapped(true)
      .excludeUnplaced(true)
      .requireSetFlags(256)
      .create();
    assertEquals("SamFilterParams minMapQ=-1 maxAlignmentCount=-1 maxMatedAlignmentScore=null maxUnmatedAlignmentScore=44 excludeUnmated=" + true + " excludeUnplaced=" + true + " requireSetFlags=256 requireUnsetFlags=4 regionTemplate=" + null, p.toString());
    assertEquals(-1, p.maxAlignmentCount());
    assertEquals(null, p.maxMatedAlignmentScore());
    assertEquals(new IntegerOrPercentage(44), p.maxUnmatedAlignmentScore());
    assertEquals(256, p.requireSetFlags());
    assertTrue(p.excludeUnmated());
  }

}
