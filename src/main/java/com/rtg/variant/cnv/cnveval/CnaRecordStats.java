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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.variant.cnv.cnveval.CnaVariant.RegionContext;
import com.rtg.vcf.VcfRecord;

import java.util.HashSet;
import java.util.Set;

/**
 * Store simple per VcfRecord evaluation region statistics
 */
@TestClass("com.rtg.variant.cnv.cnveval.CnvEvalCliTest")
class CnaRecordStats {

  private final VcfRecord mRecord;
  private Set<String> mNames = new HashSet<String>();
  private int mHit = 0;
  private int mMiss = 0;

  CnaRecordStats(VcfRecord rec) {
    mRecord = rec;
  }

  void increment(CnaVariant v) {
    if (v.context() == RegionContext.NORMAL) {
      increment(v.isCorrect());
    }
    if (v.names() != null) {
      mNames.add(v.names());
    }
  }

  void increment(boolean correct) {
    if (correct) {
      mHit++;
    } else {
      mMiss++;
    }
  }

  VcfRecord record() {
    return mRecord;
  }

  Set<String> names() {
    return mNames;
  }

  int hit() {
    return mHit;
  }

  int miss() {
    return mMiss;
  }

  double hitFraction() {
    return (double) mHit / (mHit + mMiss);
  }
}
