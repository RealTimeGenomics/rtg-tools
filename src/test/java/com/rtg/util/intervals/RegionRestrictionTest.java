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

import static com.rtg.util.intervals.RegionRestriction.MISSING;

import junit.framework.TestCase;

/**
 */
public class RegionRestrictionTest extends TestCase {

  public void test() {
    checkMalformed("");
    //checkMalformed("blah::");
    checkMalformed("blah:-600");
    checkMalformed("blah:600-400");
    checkMalformed("blah:600-+400");
    checkMalformed("blah:600+-400");
    checkMalformed("blah:600-599");
    checkMalformed("blah:600+0");
    checkMalformed("blah:600~0");
    checkMalformed("blah:0+10");
    checkMalformed("chr16:2194033+5000QRST");
    checkMalformed("chr16:2194033-2194060QRST");
    checkMalformed("chr16:2Q-3T");
    checkMalformed("chr16:2-T3");
    checkSuccess("blah:600-600", "blah", 599, 600, "blah:600-600");
    checkSuccess("blah:600+1", "blah", 599, 600, "blah:600-600");
    checkSuccess("blah:600~1", "blah", 598, 600, "blah:599-600");
    checkSuccess("blah:600+2", "blah", 599, 601, "blah:600-601");
    checkSuccess("blah:600~2", "blah", 597, 601, "blah:598-601");
    checkSuccess("blah:600-609", "blah", 599, 609, "blah:600-609");
    // Subsequence from a sequence named like a region
    checkSuccess("blah:500-509:600-609", "blah:500-509", 599, 609, "blah:500-509:600-609");
    // All of a sequence that is named like a region
    checkSuccess("blah:500-509:", "blah:500-509", MISSING, MISSING, "blah:500-509:");
    // All of a sequence, using colon
    checkSuccess("blah:", "blah", MISSING, MISSING, "blah");
    // All of a sequence containing a colon, using colon
    checkSuccess("blah::", "blah:", MISSING, MISSING, "blah::");
    checkSuccess("blah:600+10", "blah", 599, 609, "blah:600-609");
    checkSuccess("blah:600~10", "blah", 589, 609, "blah:590-609");
    checkSuccess("blah:50~100", "blah", 0, 149, "blah:1-149"); // Check truncation on left
    checkSuccess("blah", "blah", MISSING, MISSING, "blah");
    checkSuccess("blah:1-2", "blah", 0, 2, "blah:1-2");
    checkSuccess("blah:1+2", "blah", 0, 2, "blah:1-2");
    checkSuccess("blah:1+1", "blah", 0, 1, "blah:1-1");
    checkSuccess("blah:1", "blah", 0, -1, "blah:1");

    // These tests assume the default locale uses ',' for thousands and '.' for decimal, make better if need be
    checkMalformed("blah:1000.1-2000");
    checkMalformed("blah:1000-2000.8");
    checkSuccess("blah:1,000-2,000", "blah", 999, 2000, "blah:1000-2000");
  }

  public void testBoundedOverlaps() {
    checkBoundedOverlaps(new RegionRestriction("blah:10-100"));
  }

  // This method currently checks both RegionRestriction and SequenceNameLocusSimple implementations
  static void checkBoundedOverlaps(SequenceNameLocus r) {
    assertEquals(9, r.getStart());
    assertEquals(100, r.getEnd());

    assertFalse(r.overlaps(new SequenceNameLocusSimple("blah", 0, 9)));

    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 0, 10)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 0, 100)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 0, 1000)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 11, 20)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 9, 1000)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 99, 1000)));

    //assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 9, 9)));
    //assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 99, 99)));

    assertFalse(r.overlaps(new SequenceNameLocusSimple("blah1", 99, 1000)));
    assertFalse(r.overlaps(new SequenceNameLocusSimple("blah", 100, 1000)));
  }

  // Test overlapping of unbounded regions
  public void testUnboundedOverlaps() {
    RegionRestriction r = new RegionRestriction("blah:10");
    assertFalse(r.overlaps(new SequenceNameLocusSimple("blah", 0, 9)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 0, 10)));
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 99, 1000)));

    r = new RegionRestriction("blah");
    assertTrue(r.overlaps(new SequenceNameLocusSimple("blah", 0, 10)));
    assertFalse(r.overlaps(new SequenceNameLocusSimple("blah1", 0, 10)));
  }

  public void testValidateRegion() {
    assertTrue(RegionRestriction.validateRegion("blah"));
    assertFalse(RegionRestriction.validateRegion("blah:-1"));
    assertTrue(RegionRestriction.validateRegion("blah:-1:"));
  }

  private void checkSuccess(String region, String template, int start, int end, String formatted) {
    checkBasic(template, start, end, formatted);
    final RegionRestriction parsed = new RegionRestriction(region);
    assertEquals(template, parsed.getSequenceName());
    assertEquals(start, parsed.getStart());
    assertEquals(end, parsed.getEnd());
    assertEquals(formatted, parsed.toString());
  }

  private void checkBasic(String template, int start, int end, String formatted) {
    final RegionRestriction expected = new RegionRestriction(template, start, end);
    assertEquals(template, expected.getSequenceName());
    assertEquals(start, expected.getStart());
    assertEquals(end, expected.getEnd());
    assertEquals(formatted, expected.toString());
  }

  private void checkMalformed(String region) {
    try {
      new RegionRestriction(region);
      fail("Expected parsing to grumble about: " + region);
    } catch (IllegalArgumentException e) {
      assertEquals("Malformed range in restriction: \"" + region + "\"", e.getMessage());
    }
  }
}
