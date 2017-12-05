/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
import java.util.List;
import java.util.PriorityQueue;

import com.rtg.reader.SequencesReader;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Decomposes variants during reading.
 */
public class DecomposingVcfIterator extends Decomposer implements VcfIterator {

  private static final String ORP = "ORP";
  private static final String ORL = "ORL";

  private final VcfIterator mIn;
  // buffer to ensure output variants are still sorted
  private final PriorityQueue<VcfRecord> mQueue = new PriorityQueue<>(1, new ReorderingVcfWriter.VcfPositionalComparator());
  private VcfRecord mNext;

  /**
   * Wraps around an existing VCF iterator.
   *
   * @param in where to read from
   * @param template supplies reference bases not directly represented in the input VCF records
   * @throws IOException when IO or format errors occur.
   */
  public DecomposingVcfIterator(VcfIterator in, SequencesReader template) throws IOException {
    super(template);
    mIn = new VcfFilterIterator(in, new AssertVcfSorted());
    mIn.getHeader().ensureContains(new InfoField(ORP, MetaType.STRING, VcfNumber.ONE, "Original variant position"));
    mIn.getHeader().ensureContains(new InfoField(ORL, MetaType.STRING, VcfNumber.ONE, "Original reference length"));
    if (mIn.hasNext()) {
      mNext = mIn.next();
      populateQueue();
    }
  }

  @Override
  public VcfHeader getHeader() {
    return mIn.getHeader();
  }


  @Override
  public boolean hasNext() {
    return mQueue.size() != 0;
  }

  @Override
  public VcfRecord next() throws IOException {
    if (mQueue.size() == 0) {
      throw new IllegalStateException("No more records");
    }
    final VcfRecord rec = mQueue.poll();
    populateQueue();
    return rec;
  }

  private void populateQueue() throws IOException {
    if (mNext != null) {
      if (mQueue.isEmpty()) {
        decomposeNext();
      }
      assert !mQueue.isEmpty();

      // Read ahead until we are past the first variant or hit end of chromosome.
      // This assumes that decomposition can only shift variant start positions right, not left
      while (mNext != null && mNext.getSequenceName().equals(mQueue.peek().getSequenceName()) && mNext.getStart() <= mQueue.peek().getStart()) {
        decomposeNext();
      }
    }
  }

  private void decomposeNext() throws IOException {
    if (canDecompose(mNext)) {
      final List<VcfRecord> d = decompose(mNext);
      assert d.size() > 0 && d.get(0).getStart() >= mNext.getStart() : "Variants that shift left during decomposition are not supported";
      mQueue.addAll(d);
    } else {
      mQueue.add(mNext);
    }
    mNext = null;
    if (mIn.hasNext()) {
      mNext = mIn.next();
    }
  }

  @Override
  public void close() throws IOException {
    mIn.close();
  }

}
