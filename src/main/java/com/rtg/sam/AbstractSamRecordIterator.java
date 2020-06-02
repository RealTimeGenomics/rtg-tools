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

import com.reeltwo.jumble.annotations.TestClass;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * Implements the minimal header tracking and record counting likely to be used by any lowest-level <code>SAMRecord</code> RecordIterator
 */
@TestClass("com.rtg.sam.SkipInvalidRecordsIteratorTest")
public abstract class AbstractSamRecordIterator extends SimpleRecordCounter implements RecordIterator<SAMRecord> {

  protected SAMFileHeader mHeader;
  protected long mTotalNucleotides = 0;

  /**
   * Constructor
   * @param header give it the damn header
   */
  public AbstractSamRecordIterator(SAMFileHeader header) {
    mHeader = header;
  }

  @Override
  public long getTotalNucleotides() {
    return mTotalNucleotides;
  }

  @Override
  public SAMFileHeader header() {
    return mHeader;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
