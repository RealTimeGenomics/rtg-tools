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

import com.rtg.util.TestUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 */
public class SamFileAndRecordTest extends SkipInvalidRecordsIteratorTest {

  SamFileAndRecord getSamFileAndRecord(File f, int id) throws IOException {
    return new SamFileAndRecord(f, id);
  }

  /**
   * Test method for {@link com.rtg.sam.SamFileAndRecord#compareTo(com.rtg.sam.SamFileAndRecord)}.
   */
  public final void testCompareTo() throws IOException {
    try (final TestDirectory dir1 = new TestDirectory("testSAMFile1");
         final TestDirectory dir2 = new TestDirectory("testSAMFile2");
         final TestDirectory dir3 = new TestDirectory("testSAMFile3")) {
      final File sam1 = new File(dir1, SharedSamConstants.OUT_SAM);
      FileUtils.stringToFile(SkipInvalidRecordsIteratorTest.SAM_HEAD1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK3, sam1);
      try (final SamFileAndRecord sfr1 = getSamFileAndRecord(sam1, 1)) {

        final File sam2 = new File(dir2, SharedSamConstants.OUT_SAM);
        FileUtils.stringToFile(SkipInvalidRecordsIteratorTest.SAM_HEAD1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK2, sam2);
        try (final SamFileAndRecord sfr2 = getSamFileAndRecord(sam2, 2)) {


          final File sam3 = new File(dir3, SharedSamConstants.OUT_SAM);
          FileUtils.stringToFile(SkipInvalidRecordsIteratorTest.SAM_HEAD1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK1 + SkipInvalidRecordsIteratorTest.SAM_REC_OK2 + SkipInvalidRecordsIteratorTest.SAM_REC_OK3, sam3);
          try (final SamFileAndRecord sfr3 = getSamFileAndRecord(sam3, 3)) {

            TestUtils.testOrder(new SamFileAndRecord[]{sfr1, sfr2, sfr3}, false);

            sfr2.next();
            TestUtils.testOrder(new SamFileAndRecord[]{sfr1, sfr2, sfr3}, false);
            sfr2.next();
            //System.err.println("" + new SamFileAndRecord[] {sfr1, sfr3, sfr2});
            TestUtils.testOrder(new SamFileAndRecord[]{sfr1, sfr3, sfr2}, false);

            sfr3.next();
            TestUtils.testOrder(new SamFileAndRecord[]{sfr1, sfr2, sfr3}, false);

            sfr1.next();
            //System.err.println(sfr1.orderToString());
            //System.err.println(sfr3.orderToString());
            //System.err.println(sfr2.orderToString());
            TestUtils.testOrder(new SamFileAndRecord[]{sfr2, sfr3, sfr1}, false);

            sfr3.next();
            TestUtils.testOrder(new SamFileAndRecord[]{sfr2, sfr1, sfr3}, false);

          }
        }
      }
    }
  }



  private static final int[] EXPECTED_START_POSITIONS = {84, 109, 269, 310};

  public void testAllRecords() throws IOException {
    final File tempDir = FileUtils.createTempDir("testSAMFile", "sam");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/test2.sam.gz", new File(tempDir, "test2.sam.gz"));
      FileHelper.resourceToFile("com/rtg/sam/resources/test2.sam.gz.tbi", new File(tempDir, "test2.sam.gz.tbi"));
      final SAMFileHeader header = SamUtils.getSingleHeader(sam);
      final ReferenceRanges<String> r = SamRangeUtils.createExplicitReferenceRange(header, new SamRegionRestriction("simulatedSequence1"));
      try (RecordIterator<SAMRecord> it = new SkipInvalidRecordsIterator(sam.getPath(), new SamClosedFileReader(sam, r, header))) {
        int c = 0;
        while (it.hasNext()) {
          final SAMRecord rec = it.next();
          assertEquals(EXPECTED_START_POSITIONS[c++], rec.getAlignmentStart());
        }
        assertEquals(4, c);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }

  public void testAllRecordsBam() throws IOException {
    final File tempDir = FileUtils.createTempDir("testSAMFile", "sam");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/test2.bam", new File(tempDir, "test2.bam"));
      FileHelper.resourceToFile("com/rtg/sam/resources/test2.bam.bai", new File(tempDir, "test2.bam.bai"));
      final SAMFileHeader header = SamUtils.getSingleHeader(sam);
      final ReferenceRanges<String> r = SamRangeUtils.createExplicitReferenceRange(header, new SamRegionRestriction("simulatedSequence1"));
      try (RecordIterator<SAMRecord>  it = new SkipInvalidRecordsIterator(sam.getPath(), new SamClosedFileReader(sam, r, SamUtils.getSingleHeader(sam)))) {
        int c = 0;
        while (it.hasNext()) {
          it.next();
          c++;
        }
        assertEquals(4, c);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(tempDir));
    }
  }
}
