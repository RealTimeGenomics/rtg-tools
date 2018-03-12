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

import htsjdk.samtools.SAMRecord;

import junit.framework.TestCase;

/**
 */
public class DuplicateSamFilterTest extends TestCase {

  public void testSingleEnd() {
    final DuplicateSamFilter dsf = new DuplicateSamFilter();

    assertFalse(dsf.acceptRecord(null));

    final SAMRecord rec1 = new SAMRecord(null);

    assertFalse(dsf.acceptRecord(rec1));

    rec1.setReadName("IronSheik");

    assertTrue(dsf.acceptRecord(rec1));

    rec1.setSecondaryAlignment(true);

    final SAMRecord rec2 = new SAMRecord(null);
    rec2.setReadName("Sabu");
    rec2.setSecondaryAlignment(true);

    final SAMRecord rec3 = new SAMRecord(null);
    rec3.setReadName("IronSheik");
    rec3.setSecondaryAlignment(true);

    assertTrue(dsf.acceptRecord(rec1));
    assertTrue(dsf.acceptRecord(rec2));
    assertFalse(dsf.acceptRecord(rec1));
    assertFalse(dsf.acceptRecord(rec3));
  }

  public void testPairedEnd() {
    final DuplicateSamFilter dsf = new DuplicateSamFilter();

    assertFalse(dsf.acceptRecord(null));

    final SAMRecord rec1f = new SAMRecord(null);

    assertFalse(dsf.acceptRecord(rec1f));

    final String firstReadName = "IronSheik";
    final String secondReadName = "Sabu";

    rec1f.setReadName(firstReadName);
    rec1f.setReadPairedFlag(true);
    rec1f.setFirstOfPairFlag(true);

    assertTrue(dsf.acceptRecord(rec1f));

    rec1f.setSecondaryAlignment(true);

    final SAMRecord rec2f = new SAMRecord(null);
    rec2f.setReadPairedFlag(true);
    rec2f.setFirstOfPairFlag(true);
    rec2f.setReadName(secondReadName);
    rec2f.setSecondaryAlignment(true);

    final SAMRecord rec3fdup = new SAMRecord(null);
    rec3fdup.setReadPairedFlag(true);
    rec3fdup.setFirstOfPairFlag(true);
    rec3fdup.setReadName(firstReadName);
    rec3fdup.setSecondaryAlignment(true);

    final SAMRecord rec1s = new SAMRecord(null);
    rec1s.setReadName(firstReadName);
    rec1s.setReadPairedFlag(true);
    rec1s.setFirstOfPairFlag(false);
    rec1s.setSecondaryAlignment(true);

    final SAMRecord rec2s = new SAMRecord(null);
    rec2s.setReadPairedFlag(true);
    rec2s.setFirstOfPairFlag(false);
    rec2s.setReadName(secondReadName);
    rec2s.setSecondaryAlignment(true);

    final SAMRecord rec3sdup = new SAMRecord(null);
    rec3sdup.setReadPairedFlag(true);
    rec3sdup.setFirstOfPairFlag(false);
    rec3sdup.setReadName(firstReadName);
    rec3sdup.setSecondaryAlignment(true);


    assertTrue(dsf.acceptRecord(rec1f));
    assertTrue(dsf.acceptRecord(rec1s));
    assertTrue(dsf.acceptRecord(rec2f));
    assertTrue(dsf.acceptRecord(rec2s));
    assertFalse(dsf.acceptRecord(rec1f));
    assertFalse(dsf.acceptRecord(rec1s));
    assertFalse(dsf.acceptRecord(rec3fdup));
    assertFalse(dsf.acceptRecord(rec3sdup));
  }
}
