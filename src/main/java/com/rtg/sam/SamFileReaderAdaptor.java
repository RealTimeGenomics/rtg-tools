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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.intervals.ReferenceRanges;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;

/**
 * Wraps around a <code>SAMFileReader</code> iterator, supplying simple region filtering if needed.
 */
@TestClass("com.rtg.sam.SamFileAndRecordTest")
public class SamFileReaderAdaptor extends AbstractSamRecordIterator {
  private final SamReader mReader;
  private final CloseableIterator<SAMRecord> mIterator;
  private boolean mIsClosed;

  /**
   * Constructor that adapts a regular <code>SAMFileReader</code>, optionally filtering on regions
   * @param reader the reader
   * @param regions regions to filter from output, may be null
   */
  public SamFileReaderAdaptor(SamReader reader, final ReferenceRanges<String> regions) {
    super(reader.getFileHeader());
    mReader = reader;
    SamUtils.logRunId(mReader.getFileHeader());
    if (regions == null || regions.allAvailable()) {
      mIterator = mReader.iterator();
    } else {
      mIterator = new SamRestrictingIterator(mReader.iterator(), regions);
    }
  }

  @Override
  public void close() throws IOException {
    if (!mIsClosed) {
      mIsClosed = true;
      mReader.close();
    }
  }

  @Override
  public boolean hasNext() {
    return mIterator.hasNext();
  }

  @Override
  public SAMRecord next() {
    return mIterator.next();
  }
}
