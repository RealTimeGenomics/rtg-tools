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

import com.rtg.util.intervals.RegionRestriction;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import junit.framework.TestCase;

/**
 * Test class
 */
public class SamRegionRestrictionTest extends TestCase {

  public void testSomeMethod() {
    SamRegionRestriction srr = new SamRegionRestriction("fooo");
    final SAMSequenceDictionary sdd = new SAMSequenceDictionary();
    sdd.addSequence(new SAMSequenceRecord("fooo", 9876));
    assertEquals(0, srr.resolveStart());
    assertEquals(9876, srr.resolveEnd(sdd));
    assertEquals("fooo", srr.getSequenceName());
    assertEquals(RegionRestriction.MISSING, srr.getStart());
    assertEquals(RegionRestriction.MISSING, srr.getEnd());
    assertEquals("fooo", srr.toString());

    srr = new SamRegionRestriction("fooo", 75, 555);
    assertEquals(75, srr.resolveStart());
    assertEquals(555, srr.resolveEnd(sdd));
    assertEquals("fooo", srr.getSequenceName());
    assertEquals(75, srr.getStart());
    assertEquals(555, srr.getEnd());
    assertEquals("fooo:76-555", srr.toString());
    srr = new SamRegionRestriction("fooo", 75, RegionRestriction.MISSING);
    assertEquals("fooo:76", srr.toString());
  }

  public void testParsingConstructor() {
    try {
      new SamRegionRestriction("fooo:2-1");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("Malformed range in restriction: \"fooo:2-1\"", e.getMessage());
    }
    try {
      new SamRegionRestriction("fooo:-2-3");
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("Malformed range in restriction: \"fooo:-2-3\"", e.getMessage());
    }
    final SamRegionRestriction srr = new SamRegionRestriction("fooo:1-2");
    assertEquals("fooo", srr.getSequenceName());
    assertEquals(0, srr.getStart());
    assertEquals(2, srr.getEnd());
    assertEquals("fooo:1-2", srr.toString());
  }

}
