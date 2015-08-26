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

package com.rtg.vcf;

import java.io.Closeable;
import java.io.IOException;
import java.util.PriorityQueue;

import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.header.VcfHeader;

/**
 * There is no accepted sort order for variants at the same reference position. This class adjusts sort order in such cases
 * so that variants at the same reference position are locally sorted shortest first. This is primarily useful when performing
 * merge operations.
 */
public class VcfSortRefiner implements Closeable {

  private final VcfReader mIn;
  private final PriorityQueue<VcfRecord> mCurrent = new PriorityQueue<>(1, SequenceNameLocusComparator.SINGLETON);

  /**
   * Create a new VCF reader. In general you should use <code>VcfReader.openVcfReader</code> instead.
   *
   * @param in where to read from
   * @throws IOException when IO or format errors occur.
   */
  public VcfSortRefiner(VcfReader in) throws IOException {
    mIn = in;
    setNext();
  }

  /**
   * @return the header
   */
  public VcfHeader getHeader() {
    return mIn.getHeader();
  }


  /**
   * Check if there is another record to get.
   * @return boolean true if there is another record to get
   */
  public boolean hasNext() {
    return mCurrent.size() != 0;
  }

  /**
   * Get the current VCF record and advance the reader
   *
   * @return the current VCF record
   * @throws IOException when IO or format errors occur.
   */
  public VcfRecord next() throws IOException {
    if (mCurrent.size() == 0) {
      throw new IllegalStateException("No more records");
    }
    final VcfRecord rec = mCurrent.poll();
    setNext();
    return rec;
  }

  /**
   * Get the current VCF record without advancing the reader
   *
   * @return the current VCF record, or null if none.
   */
  public VcfRecord peek() {
    if (mCurrent.size() == 0) {
      throw new IllegalStateException("No more records");
    }
    return mCurrent.peek();
  }

  /**
   * Read the next record, if any.
   * @throws IOException when IO or format errors occur.
   */
  private void setNext() throws IOException {
    if (mCurrent.isEmpty() && mIn.hasNext()) {
      final VcfRecord rec = mIn.next();
      mCurrent.add(rec);
      while (mIn.hasNext()) { // Get additional records at same position
        final VcfRecord next = mIn.peek();
        if (next.getStart() != rec.getStart() || !next.getSequenceName().equals(rec.getSequenceName())) {
          break;
        }
        mCurrent.add(mIn.next());
      }
    }
  }

  @Override
  public void close() throws IOException {
    mIn.close();
  }

}
