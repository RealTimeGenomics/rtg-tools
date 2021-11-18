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
package com.rtg.variant.cnv.cnveval;

import com.rtg.util.intervals.Interval;
import com.rtg.util.intervals.Range;
import com.rtg.variant.cnv.CnaType;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Corresponds to an intersection between a CNV VCF Record and one of the evaluation regions
 */
class CnaVariant extends Range {

  enum SpanType { PARTIAL, FULL}

  private final String mNames;
  private final VcfRecord mRec;
  private final CnaType mCnaType;
  private final SpanType mSpan;
  private boolean mCorrect;

  CnaVariant(Interval range, VcfRecord rec, String names) {
    this(range.getStart(), range.getEnd(), rec, names);
  }

  private CnaVariant(int start, int end, VcfRecord rec, String names) {
    super(start, end);
    mRec = rec;
    mCnaType = CnaType.valueOf(rec);
    mNames = names == null ? "" : names;
    assert mCnaType != null;
    assert Interval.overlaps(new Range(start, end), new Range(rec.getStart(), VcfUtils.getEnd(rec) - 1));
    mSpan = rec.getStart() > start || VcfUtils.getEnd(rec) < end ? SpanType.PARTIAL : SpanType.FULL;
  }

  CnaType cnaType() {
    return mCnaType;
  }

  SpanType spanType() {
    return mSpan;
  }

  VcfRecord record() {
    return mRec;
  }

  String names() {
    return mNames;
  }

  void setCorrect(boolean correct) {
    mCorrect = correct;
  }

  boolean isCorrect() {
    return mCorrect;
  }

  public String toString() {
    return super.toString() + " " + mCnaType + " " + mSpan;
  }
}
