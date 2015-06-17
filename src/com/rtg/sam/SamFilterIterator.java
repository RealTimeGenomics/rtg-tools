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

import java.io.IOException;
import java.util.NoSuchElementException;

import com.reeltwo.jumble.annotations.TestClass;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

/**
 * Sam iterator that supports filtering
 */
@TestClass(value = {"com.rtg.reader.FormatCliTest"})
public class SamFilterIterator implements RecordIterator<SAMRecord> {

  private final RecordIterator<SAMRecord> mBaseIterator;

  private final SamFilter mSamFilter;

  private SAMRecord mNextRecord;

  private long mFilteredRecords;
  private long mValidCount = 0;

  private boolean mIsClosed;

  /**
   * Constructor
   * @param iterator the iterator to filter records from.
   * @param filter the sam record filter
   */
  public SamFilterIterator(RecordIterator<SAMRecord> iterator, SamFilter filter) {
    mBaseIterator = iterator;
    mSamFilter = filter;
    mFilteredRecords = 0;
  }

  @Override
  public void close() throws IOException {
    if (!mIsClosed) {
      mBaseIterator.close();
      mIsClosed = true;
    }
  }

  @Override
  public long getTotalNucleotides() {
    return mBaseIterator.getTotalNucleotides();
  }

  @Override
  public long getInvalidRecordsCount() {
    return mBaseIterator.getInvalidRecordsCount();
  }

  @Override
  public long getDuplicateRecordsCount() {
    return mBaseIterator.getDuplicateRecordsCount();
  }

  @Override
  public long getFilteredRecordsCount() {
    return mFilteredRecords + mBaseIterator.getFilteredRecordsCount();
  }

  @Override
  public long getOutputRecordsCount() {
    return mValidCount;
  }

  @Override
  public long getTotalRecordsCount() {
    return mBaseIterator.getTotalRecordsCount();
  }

  private void pullNextRecord() {
    if (!mBaseIterator.hasNext()) {
      mNextRecord = null;
      return;
    }
    mNextRecord = mBaseIterator.next();
  }

  @Override
  public boolean hasNext() {
    if (mNextRecord == null) {
      while (true) {
        pullNextRecord();
        if (mNextRecord != null) {
          if (!mSamFilter.acceptRecord(mNextRecord)) {
            mFilteredRecords++;
            continue;
          }
          break;
        } else {
          break;
        }
      }
    }
    return mNextRecord != null;
  }

  @Override
  public SAMRecord next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final SAMRecord ret = mNextRecord;
    mNextRecord = null;
    mValidCount++;
    return ret;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public SAMFileHeader header() {
    return mBaseIterator.header();
  }

}
