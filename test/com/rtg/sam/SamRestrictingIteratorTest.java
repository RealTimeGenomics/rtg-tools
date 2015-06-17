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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import junit.framework.TestCase;

/**
 * Test class
 */
public class SamRestrictingIteratorTest extends TestCase {

    private static final String SAM = "" + "@HD" + "\t" + "VN:1.0" + "\t" + "SO:coordinate" + StringUtils.LS
  + "@SQ" + "\t" + "SN:g1" + "\t" + "LN:40" + StringUtils.LS
  + "5" + "\t" + "0" + "\t" + "g1" + "\t" + "14" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGCTAGGTT" + "\t" + "&'(``````" + "\t" + "AS:i:0" + StringUtils.LS
  + "5" + "\t" + "0" + "\t" + "g1" + "\t" + "15" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "GCTAGGTTT" + "\t" + "&'(``````" + "\t" + "AS:i:0" + StringUtils.LS
  + "6" + "\t" + "0" + "\t" + "g1" + "\t" + "15" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "GCTAGGTTA" + "\t" + "`````````" + "\t" + "AS:i:0" + StringUtils.LS
  + "7" + "\t" + "0" + "\t" + "g1" + "\t" + "17" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TAGGTTATC" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
  + "8" + "\t" + "0" + "\t" + "g1" + "\t" + "18" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGGTTTTCG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
  + "9" + "\t" + "0" + "\t" + "g1" + "\t" + "18" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGGTTTTCG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
  + "10" + "\t" + "0" + "\t" + "g1" + "\t" + "23" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TTCGACTGG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
  + "11" + "\t" + "0" + "\t" + "g1" + "\t" + "24" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TCGACTGGT" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS;

  public void testIterator() throws IOException {
    ByteArrayInputStream baos = new ByteArrayInputStream(SAM.getBytes());
    final SamReader reader = SamUtils.makeSamReader(baos);
    ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(reader.getFileHeader(), new SamRegionRestriction("g1", 22, 23));
    SamRestrictingIterator it = new SamRestrictingIterator(reader.iterator(), ranges); //these positions are 0-based
    int[] expectedLocs = {15, 15, 17, 18, 18, 23};
    int i = 0;
    while (it.hasNext()) {
      final SAMRecord r = it.next();
      assertEquals(r.getSAMString().trim(), expectedLocs[i++], r.getAlignmentStart());
    }
    assertFalse(it.hasNext());
    assertEquals(6, i);
  }

  private static final String SAM2 = "" + "@HD" + "\t" + "VN:1.0" + "\t" + "SO:coordinate" + StringUtils.LS
    + "@SQ" + "\t" + "SN:g0" + "\t" + "LN:400" + StringUtils.LS
    + "@SQ" + "\t" + "SN:g1" + "\t" + "LN:400" + StringUtils.LS
    + "@SQ" + "\t" + "SN:g2" + "\t" + "LN:400" + StringUtils.LS
    + "5" + "\t" + "0" + "\t" + "g1" + "\t" + "140" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGCTAGGTT" + "\t" + "&'(``````" + "\t" + "AS:i:0" + StringUtils.LS
    + "5" + "\t" + "0" + "\t" + "g1" + "\t" + "150" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "GCTAGGTTT" + "\t" + "&'(``````" + "\t" + "AS:i:0" + StringUtils.LS
    + "6" + "\t" + "0" + "\t" + "g1" + "\t" + "150" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "GCTAGGTTA" + "\t" + "`````````" + "\t" + "AS:i:0" + StringUtils.LS
    + "7" + "\t" + "0" + "\t" + "g1" + "\t" + "170" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TAGGTTATC" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
    + "8" + "\t" + "0" + "\t" + "g1" + "\t" + "180" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGGTTTTCG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
    + "9" + "\t" + "0" + "\t" + "g1" + "\t" + "180" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "AGGTTTTCG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
    + "10" + "\t" + "0" + "\t" + "g1" + "\t" + "230" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TTCGACTGG" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS
    + "11" + "\t" + "0" + "\t" + "g1" + "\t" + "240" + "\t" + "255" + "\t" + "9M" + "\t" + "*" + "\t" + "0" + "\t" + "0" + "\t" + "TCGACTGGT" + "\t" + "`````````" + "\t" + "AS:i:1" + StringUtils.LS;

  public void testIterator2() throws IOException {
    ByteArrayInputStream baos = new ByteArrayInputStream(SAM2.getBytes());
    final SamReader reader = SamUtils.makeSamReader(baos);
    ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(reader.getFileHeader(),
      new SamRegionRestriction("g1", 149, 169),
      new SamRegionRestriction("g1", 180, 185),
      new SamRegionRestriction("g1", 186, 189),
      new SamRegionRestriction("g0", 100, 205),
      new SamRegionRestriction("g2", RegionRestriction.MISSING, RegionRestriction.MISSING)
      );
    //System.err.println(ranges);
    SamRestrictingIterator it = new SamRestrictingIterator(reader.iterator(), ranges); //these positions are 0-based
    int[] expectedLocs = {150, 150, 180, 180};
    int i = 0;
    while (it.hasNext()) {
      final SAMRecord r = it.next();
      assertEquals(r.getSAMString().trim(), expectedLocs[i++], r.getAlignmentStart());
    }
    assertFalse(it.hasNext());
    assertEquals(expectedLocs.length, i);
  }
}
