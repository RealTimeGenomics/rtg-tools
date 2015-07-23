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

import java.util.Arrays;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import junit.framework.TestCase;

/**
 */
public class SamCompareUtilsTest extends TestCase {

  public void test() {
    final SAMFileHeader header = new SAMFileHeader();
    header.setSequenceDictionary(new SAMSequenceDictionary(Arrays.asList(new SAMSequenceRecord("raga", 100), new SAMSequenceRecord("yaga", 100), new SAMSequenceRecord("zaga", 100))));
    final SAMRecord rec1 = new SAMRecord(header);
    rec1.setReferenceIndex(1);
    final SAMRecord rec2 = new SAMRecord(header);
    rec2.setReferenceIndex(2);
    assertEquals(-1, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(1, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec1.setReferenceIndex(2);
    rec1.setAlignmentStart(50);
    rec2.setAlignmentStart(25);
    assertEquals(1, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(-1, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec1.setReadPairedFlag(true);
    rec2.setReadPairedFlag(true);
    rec1.setProperPairFlag(true);
    rec2.setProperPairFlag(false);
    rec1.setAlignmentStart(25);
    assertEquals(-1, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(1, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec2.setProperPairFlag(true);
    rec1.setReadUnmappedFlag(true);
    assertEquals(1, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(-1, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec2.setReadUnmappedFlag(true);
    assertEquals(0, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(0, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec1.setReferenceIndex(-1);
    assertEquals(1, SamCompareUtils.compareSamRecords(rec1, rec2));
    assertEquals(-1, SamCompareUtils.compareSamRecords(rec2, rec1));
    rec2.setReferenceIndex(-1);
    assertEquals(0, SamCompareUtils.compareSamRecords(rec2, rec1));
  }
}
