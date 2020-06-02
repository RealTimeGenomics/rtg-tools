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
package com.rtg.variant.cnv;

import java.util.Collection;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.VcfFilter;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Only retains valid SV DUP/DEL records on the chromosomes of interest
 */
public class CnvRecordFilter implements VcfFilter {

  private final Collection<String> mChrs;
  private final boolean mFilterOverlap;
  private String mLastSeq = null;
  private int mLastEnd = 0;

  /**
   * Constructor
   * @param chrs the set of chromosomes of interest
   * @param filterOverlap true if only the first of overlapping variants should be kept
   */
  public CnvRecordFilter(Collection<String> chrs, boolean filterOverlap) {
    mChrs = chrs;
    mFilterOverlap = filterOverlap;
  }

  @Override
  public void setHeader(VcfHeader header) { }

  @Override
  public boolean accept(VcfRecord rec) {
    final CnaType status = CnaType.valueOf(rec);
    if (status != CnaType.DEL && status != CnaType.DUP) { // Non SV or not a DEL/DUP
      return false;
    }
    final Integer end = VcfUtils.getIntegerInfoFieldFromRecord(rec, VcfUtils.INFO_END);
    if (end == null) {
      Diagnostic.warning("Skipping SV record without a defined END: " + rec);
      return false;
    }
    if (mFilterOverlap && rec.getSequenceName().equals(mLastSeq) && rec.getStart() + 1 < mLastEnd) { // +1 since spec says SV records include the base before the event
      Diagnostic.warning("Skipping SV record that overlaps a previous SV variant: " + rec);
      return false;
    }
    if (!mChrs.contains(rec.getSequenceName())) {
      return false;
    }
    mLastSeq = rec.getSequenceName();
    mLastEnd = end;
    return true;
  }

}
