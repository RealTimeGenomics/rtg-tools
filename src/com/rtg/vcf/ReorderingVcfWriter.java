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
import java.io.Serializable;
import java.util.Comparator;

import com.rtg.util.CompareHelper;
import com.rtg.util.ReorderingQueue;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.header.VcfHeader;


/**
 * Class to reorder VCF records during output.
 */
public class ReorderingVcfWriter extends ReorderingQueue<VcfRecord> implements VcfWriter {

  private static final int BUFFER_SIZE = 10000; // Assume records will be out of order by at most this amount.

  /**
   * Position based comparator for VCF record.
   */
  public static final class VcfPositionalComparator implements Comparator<VcfRecord>, Serializable {
    @Override
    public int compare(VcfRecord o1, VcfRecord o2) {
      return new CompareHelper()
          .compare(o1.getSequenceName(), o2.getSequenceName())
          .compare(o1.getStart(), o2.getStart())
          .compareList(o1.getAltCalls(), o2.getAltCalls())
          .compare(o1.getRefCall(), o2.getRefCall())
          .compareToString(o1, o2) // Expensive, but only if all earlier comparisons have failed
          .compare(System.identityHashCode(o1), System.identityHashCode(o2)) // Ensure none are dropped as duplicates
          .result();
    }
  }

  final VcfWriter mOut;

  /**
   * Constructor
   * @param out ultimate destination
   */
  public ReorderingVcfWriter(VcfWriter out) {
    super(BUFFER_SIZE, new VcfPositionalComparator());
    mOut = out;
  }

  @Override
  public VcfHeader getHeader() {
    return mOut.getHeader();
  }

  @Override
  public void write(VcfRecord record) throws IOException {
    addRecord(record);
  }

  @Override
  protected String getReferenceName(VcfRecord record) {
    return record.getSequenceName();
  }

  @Override
  protected int getPosition(VcfRecord record) {
    return record.getStart();
  }

  @Override
  protected void flushRecord(VcfRecord rec) throws IOException {
    mOut.write(rec);
  }

  @Override
  protected void reportReorderingFailure(VcfRecord rec) {
    Diagnostic.warning("VcfRecord dropped due to excessive out-of-order processing.\n" + rec);
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mOut) { // Use try with resources on existing writer for nice closing
      super.close();
    }
  }
}
