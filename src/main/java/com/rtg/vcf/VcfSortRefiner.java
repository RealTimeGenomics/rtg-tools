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

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import com.rtg.util.intervals.IntervalComparator;
import com.rtg.util.io.IOIterator;
import com.rtg.vcf.header.VcfHeader;

/**
 * There is no accepted sort order for variants at the same reference position. This class adjusts sort order in such cases
 * so that variants at the same reference position are locally sorted shortest first. This is primarily useful when performing
 * merge operations. Assumes that input variants are otherwise sorted within chromosomes.
 */
public class VcfSortRefiner implements VcfIterator {

  private final VcfIterator mIn;
  private final PriorityQueue<VcfRecord> mCurrent = new PriorityQueue<>(1, IntervalComparator.SINGLETON);
  private VcfRecord mNext;

  /**
   * Wraps around an existing VCF reader.
   *
   * @param in where to read from
   * @throws IOException when IO or format errors occur.
   */
  public VcfSortRefiner(VcfIterator in) throws IOException {
    mIn = new VcfFilterIterator(in, new AssertVcfSorted());
    if (mIn.hasNext()) {
      mNext = mIn.next();
      setNext();
    }
  }

  @Override
  public VcfHeader getHeader() {
    return mIn.getHeader();
  }


  @Override
  public boolean hasNext() {
    return mCurrent.size() != 0;
  }

  @Override
  public VcfRecord next() throws IOException {
    if (mCurrent.size() == 0) {
      throw new IllegalStateException("No more records");
    }
    final VcfRecord rec = mCurrent.poll();
    setNext();
    return rec;
  }

  /**
   * Fill the queue for the current position if need be.
   * @throws IOException when IO or format errors occur.
   */
  private void setNext() throws IOException {
    if (mCurrent.isEmpty() && mNext != null) {
      final String chr = mNext.getSequenceName();
      final int pos = mNext.getStart();
      mCurrent.add(mNext);
      mNext = null;
      while (mIn.hasNext()) { // Get additional records at same position
        final VcfRecord rec = mIn.next();
        if (rec.getStart() != pos || !rec.getSequenceName().equals(chr)) {
          mNext = rec;
          break;
        }
        mCurrent.add(rec);
      }
    }
  }

  @Override
  public void forEach(Consumer<? super VcfRecord> action) throws IOException {
    IOIterator.forEach(this, action);
  }

  @Override
  public void close() throws IOException {
    mIn.close();
  }

}
