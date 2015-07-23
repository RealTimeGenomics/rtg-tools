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

import static com.rtg.util.StringUtils.TAB;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import htsjdk.samtools.SAMFileHeader;
import junit.framework.TestCase;

/**
 * Tests for SamRangeUtils
 */
public class SamRangeUtilsTest extends TestCase {

  private static final String NL = "\n";  // SAM files always use \n.

  public SamRangeUtilsTest(final String name) {
    super(name);
  }

  private static final String SAM_HEADER = ""
    + "@HD" + TAB + "VN:1.0" + TAB + "SO:coordinate" + NL
    + "@SQ" + TAB + "SN:g0" + TAB + "LN:200" + NL
    + "@SQ" + TAB + "SN:g1" + TAB + "LN:400" + NL
    + "@SQ" + TAB + "SN:g2" + TAB + "LN:600" + NL
    ;

  private static final String BED_REGIONS = ""
    + "g1\t0\t50\n"
    + "g1\t100\t500\n"
    + "g2\t20\t500\n"
    ;

  public void testCreateSingleRangeList() throws IOException {
    final ByteArrayInputStream bis = new ByteArrayInputStream(SAM_HEADER.getBytes());
    final SAMFileHeader header = SamUtils.getSingleHeader(bis);

    final ReferenceRanges<String> refRanges = SamRangeUtils.createExplicitReferenceRange(header, new SamRegionRestriction("g1"));

    assertNull(refRanges.get("g0"));
    assertNull(refRanges.get("g2"));
    assertNotNull(refRanges.get("g1"));

    final RangeList<String> seqRanges = refRanges.get("g1");
    assertNotNull(seqRanges.find(100));
    assertNull(seqRanges.find(1000));

    try {
      SamRangeUtils.createExplicitReferenceRange(header, new SamRegionRestriction("g3"));
    } catch (NoTalkbackSlimException e) {
      //
    }
  }

  public void testCreateBedRangeList() throws IOException {
    try (final TestDirectory dir = new TestDirectory("samrangeutils")) {
      final ByteArrayInputStream bis = new ByteArrayInputStream(SAM_HEADER.getBytes());
      final SAMFileHeader header = SamUtils.getSingleHeader(bis);
      final File bedfile = new File(dir, "regions.bed");
      FileUtils.stringToFile(BED_REGIONS, bedfile);
      ReferenceRanges<String> refRanges = SamRangeUtils.createBedReferenceRanges(bedfile);
      assertNull(refRanges.get("g0"));
      assertNotNull(refRanges.get("g1"));
      assertNotNull(refRanges.get("g2"));

      RangeList<String> seqRanges = refRanges.get("g1");
      assertNotNull(seqRanges.find(100));
      assertNull(seqRanges.find(1000));
      assertNotNull(seqRanges.find(450));

      refRanges = SamRangeUtils.createBedReferenceRanges(header, bedfile);
      seqRanges = refRanges.get("g1");
      assertNull(seqRanges.find(450)); // Validation against header will have clipped to length of g1
    }
  }

  public void testCreateFullRangeList() throws IOException {
    final ByteArrayInputStream bis = new ByteArrayInputStream(SAM_HEADER.getBytes());
    final SAMFileHeader header = SamUtils.getSingleHeader(bis);

    final ReferenceRanges<String> refRanges = SamRangeUtils.createFullReferenceRanges(header);

    assertNotNull(refRanges.get("g0"));
    assertNotNull(refRanges.get("g1"));
    assertNotNull(refRanges.get("g2"));
    assertNull(refRanges.get("g3"));

    final RangeList<String> seqRanges = refRanges.get("g1");
    assertNotNull(seqRanges.find(100));
    assertNull(seqRanges.find(1000));
  }

  public void testConvertNameToIdKeys() throws IOException {
    final ByteArrayInputStream bis = new ByteArrayInputStream(SAM_HEADER.getBytes());
    final SAMFileHeader header = SamUtils.getSingleHeader(bis);

    final ReferenceRanges<String> refRanges = SamRangeUtils.createFullReferenceRanges(header);
    refRanges.setIdMap(SamUtils.getSequenceIdLookup(header.getSequenceDictionary()));

    assertNotNull(refRanges.get(0));
    assertNotNull(refRanges.get(1));
    assertNotNull(refRanges.get(2));
    assertNull(refRanges.get(3));

    final RangeList<String> seqRanges = refRanges.get(1);
    assertNotNull(seqRanges.find(100));
    assertNull(seqRanges.find(1000));
  }

  public void testGetReferenceRanges() throws IOException {
    final ByteArrayInputStream bis = new ByteArrayInputStream(SAM_HEADER.getBytes());
    final SAMFileHeader header = SamUtils.getSingleHeader(bis);

    final SamFilterParams params = new SamFilterParams.SamFilterParamsBuilder().create();
    final ReferenceRanges<String> refRanges = SamRangeUtils.createReferenceRanges(header, params);
    assertEquals(3, refRanges.sequenceNames().size());

  }

}
