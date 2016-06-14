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

package com.rtg.vcf.eval;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.vcf.VcfRecord;

/**
 * Class template for value extractor.
 */
@TestClass("com.rtg.vcf.eval.RocScoreFieldTest")
public abstract class RocSortValueExtractor {

  abstract boolean requiresSample();

  abstract double getSortValue(VcfRecord rec, int sampleNo);

  abstract RocSortOrder getSortOrder();

  /** Dummy null extractor for testing purposes */
  public static final RocSortValueExtractor NULL_EXTRACTOR = new RocSortValueExtractor() {

    @Override
    boolean requiresSample() {
      return false;
    }

    @Override
    double getSortValue(VcfRecord rec, int sampleNo) {
      return 0;
    }

    @Override
    RocSortOrder getSortOrder() {
      return RocSortOrder.ASCENDING;
    }

    @Override
    public String toString() {
      return "TEST";
    }
  };
}
