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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import junit.framework.TestCase;

/**
 */
public class SamMultiRestrictingIteratorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Diagnostic.setLogStream();
  }

  private SAMFileHeader getHeader() throws IOException {
    try (final InputStream is = getClass().getClassLoader().getResourceAsStream("com/rtg/sam/resources/test-multiregion.sam.gz");
         final SamReader reader = SamUtils.makeSamReader(is)) {
      return reader.getFileHeader();
    }
  }

  public void testIterator() throws IOException {
    // Test a situation where previously we had a bug that was skipping over some records
    // that should have been included in a subsequent region.  This does involve an
    // placed unmapped records, possibly some interaction with the tabix index.
    final File testDir = FileUtils.createTempDir("multiregion", "test");
    try {
      final File samFile = new File(testDir, "test-multiregion.sam.gz");
      final File samFileIndex = new File(testDir, "test-multiregion.sam.gz.tbi");
      FileUtils.copyResource("com/rtg/sam/resources/test-multiregion.sam.gz", samFile);
      FileUtils.copyResource("com/rtg/sam/resources/test-multiregion.sam.gz.tbi", samFileIndex);
      final SamRegionRestriction region1 = new SamRegionRestriction("17", 37866468, 37866469);
      final SamRegionRestriction region2 = new SamRegionRestriction("17", 37866577, 37866578);
      final SAMFileHeader header = getHeader();
      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(header, region1, region2);
      try (final SamClosedFileReader reader = new SamClosedFileReader(samFile, ranges, null, header)) {
        final int[] expectedLocs = {37866469, 37866470, 37866470};
        int recordCount = 0;
        while (reader.hasNext()) {
          final SAMRecord r = reader.next();
          assertEquals(r.getSAMString().trim(), expectedLocs[recordCount++], r.getAlignmentStart());
        }
        assertEquals(3, recordCount);
      }
    } finally {
      assertTrue(FileUtils.deleteFiles(testDir));
    }
  }
}
