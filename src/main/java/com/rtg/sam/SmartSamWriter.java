/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import java.io.IOException;

import com.rtg.util.ReorderingQueue;
import com.rtg.util.diagnostic.Diagnostic;

import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordCoordinateComparator;


/**
 * Class to reorder SAM records during output. Note that only one record per position is supported.
 */
public class SmartSamWriter extends ReorderingQueue<SAMRecord> {

  private static final int BUFFER_SIZE = 10000; // Assume records will be out of order by at most this amount.

  final SAMFileWriter mOut;

  /**
   * Constructor
   * @param out where records should eventually be sent
   */
  public SmartSamWriter(SAMFileWriter out) {
    super(BUFFER_SIZE, new SAMRecordCoordinateComparator() {
      @Override
      public int compare(final SAMRecord samRecord1, final SAMRecord samRecord2) {
        final int cmp = super.compare(samRecord1, samRecord2);
        if (cmp != 0) {
          return cmp;
        }
        final SAMReadGroupRecord rg1 = samRecord1.getReadGroup();
        final SAMReadGroupRecord rg2 = samRecord2.getReadGroup();
        if (rg1 == rg2) {
          return 0;
        }
        return rg1 == null ? -1 : rg2 == null ? 1 : rg1.getReadGroupId().compareTo(rg2.getReadGroupId());
      }
    });
    mOut = out;
  }

  @Override
  public boolean addRecord(SAMRecord r) throws IOException {
    // Don't attempt to reorder all the fully-unmapped (i.e. no reference sequence) records
    if (r.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX) {
      if (!mRecordSet.isEmpty()) {
        flush();
      }
      flushRecord(r);
      return true;
    } else {
      return super.addRecord(r);
    }
  }

  @Override
  protected String getReferenceName(SAMRecord record) {
    return record.getReferenceName();
  }

  @Override
  protected int getPosition(SAMRecord record) {
    return record.getAlignmentStart();
  }

  @Override
  protected void flushRecord(SAMRecord rec) {
    mOut.addAlignment(rec);
  }

  @Override
  protected void reportReorderingFailure(SAMRecord rec) {
    Diagnostic.warning("SAMRecord dropped due to excessive out-of-order processing.\n" + rec);
  }

  @Override
  public void close() throws IOException {
    super.close();
    mOut.close();
  }
}
