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
import java.util.Iterator;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import junit.framework.TestCase;

/**
 * Test class
 */
public class SamClosedFileReaderTest extends TestCase {

  @Override
  protected void setUp() {
    Diagnostic.setLogStream();
  }

  public void testSam() throws IOException {
    final File dir = FileUtils.createTempDir("closedfilereader", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/test1_normal.sam", new File(dir, "sam.sam"));
      check(sam);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private void check(final File bam) throws IOException {
    final SamReader normalReader = SamUtils.makeSamReader(bam);
    final Iterator<SAMRecord> norm = normalReader.iterator();
    try {
      try (final RecordIterator<SAMRecord> closed = new SamClosedFileReader(bam, null, null, normalReader.getFileHeader())) {
        while (norm.hasNext() && closed.hasNext()) {
          final SAMRecord a = norm.next();
          final SAMRecord b = closed.next();
          assertEquals(a.getSAMString().trim(), b.getSAMString().trim());
        }
        assertEquals(norm.hasNext(), closed.hasNext());
      }
    } finally {
      normalReader.close();
    }
  }

  public void testBam() throws IOException {
    final File dir = FileUtils.createTempDir("closedfilereader", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/test1_normal.sam", new File(dir, "sam.sam"));
      final File bam = new File(dir, "bam.bam");
      SamUtilsTest.convertSamToBam(bam, BamIndexer.indexFileName(bam), sam);
      check(bam);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private void check2(final File sam, File exp) throws IOException {
    try (SamReader normalReader = SamUtils.makeSamReader(exp)) {
      final SamRegionRestriction rr = new SamRegionRestriction("simulatedSequence2", 10000, 20000);
      final Iterator<SAMRecord> norm = normalReader.query(rr.getSequenceName(), rr.getStart() + 1, rr.getEnd(), false);
      final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(normalReader.getFileHeader(), rr);
      checkRecords(sam, normalReader, norm, ranges);
    }
  }

  public void testSamRestrict() throws IOException {
    final File dir = FileUtils.createTempDir("closedfilereader", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow2.sam.gz", new File(dir, "sam.sam.gz"));
      FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow2.sam.gz.tbi", new File(dir, "sam.sam.gz.tbi"));
      final File bam = new File(dir, "bam.bam");
      SamUtilsTest.convertSamToBam(bam, BamIndexer.indexFileName(bam), sam);
      check2(sam, bam);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testBamRestrict() throws IOException {
    final File dir = FileUtils.createTempDir("closedfilereader", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow2.sam.gz", new File(dir, "sam.sam.gz"));
      final File bam = new File(dir, "bam.bam");
      SamUtilsTest.convertSamToBam(bam, BamIndexer.indexFileName(bam), sam);
      check2(bam, bam);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testRegionsAtEndOfSequence() throws IOException {
    // This test simulates an error that was occuring in coverage/sammerge when you have two regions at the end of a
    // and the second to last region is overlapped by the final read in the sequence.

    final File dir = FileUtils.createTempDir("closedfilereader", "test");
    try {
      final File sam = FileHelper.resourceToFile("com/rtg/sam/resources/multiRegionSequenceEnd.sam.gz", new File(dir, "sam.sam.gz"));
      final File bam = new File(dir, "bam.bam");
      SamUtilsTest.convertSamToBam(bam, BamIndexer.indexFileName(bam), sam);
      try (SamReader normalReader = SamUtils.makeSamReader(bam)) {
        final SamRegionRestriction r1 = new SamRegionRestriction("chr7", 159334000, 159335000);
        final SamRegionRestriction r2 = new SamRegionRestriction("chr7", 159335000, 159335001);
        final Iterator<SAMRecord> norm = normalReader.query(r1.getSequenceName(), r1.getStart(), r2.getEnd(), false);
        final ReferenceRanges<String> ranges = SamRangeUtils.createExplicitReferenceRange(normalReader.getFileHeader(), r1, r2);
        checkRecords(bam, normalReader, norm, ranges);
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private void checkRecords(File bam, SamReader normalReader, Iterator<SAMRecord> norm, ReferenceRanges<String> ranges) throws IOException {
    try (final RecordIterator<SAMRecord> closed = new SamClosedFileReader(bam, ranges, null, normalReader.getFileHeader())) {
      while (norm.hasNext() && closed.hasNext()) {
        final SAMRecord a = norm.next();
        final SAMRecord b = closed.next();
        assertEquals(a.getSAMString(), b.getSAMString());
      }
      assertEquals(norm.hasNext(), closed.hasNext());
    }
  }
}