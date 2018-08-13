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

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import junit.framework.TestCase;

/**
 */
public class SamMultiRestrictingIteratorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Diagnostic.setLogStream();
  }

  public void testIterator() throws IOException, UnindexableDataException {
    // Test a situation where previously we had a bug that was skipping over some records
    // that should have been included in a subsequent region.
    final File testDir = FileUtils.createTempDir("multiregion", "test");
    try {
      final File samFile = new File(testDir, "test-multiregion.sam.gz");
      FileHelper.resourceToGzFile("com/rtg/sam/resources/test-multiregion.sam", samFile);
      new TabixIndexer(samFile).saveSamIndex();
      final SamRegionRestriction region1 = new SamRegionRestriction("17", 37866468, 37866469);
      final SamRegionRestriction region2 = new SamRegionRestriction("17", 37866577, 37866578);
      final SAMFileHeader header = SamUtils.getSingleHeader(samFile);
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
