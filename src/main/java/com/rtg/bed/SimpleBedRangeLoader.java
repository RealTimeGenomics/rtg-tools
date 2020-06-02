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
package com.rtg.bed;

import com.rtg.util.intervals.SimpleRangeMeta;
import com.rtg.util.intervals.RangeMeta;

/**
 * Loads BED records into range data that has either name (if present) or
 * string representation of the region as meta data.
 */
public class SimpleBedRangeLoader extends BedRangeLoader<String> {

  /**
   * Constructor
   */
  public SimpleBedRangeLoader() {
    super(0);
  }

  @Override
  protected RangeMeta<String> getRangeData(BedRecord rec) {
    return new SimpleRangeMeta<>(rec.getStart(), rec.getEnd() == rec.getStart() ? rec.getEnd() + 1 : rec.getEnd(), getMeta(rec));
  }

  @Override
  protected String getMeta(final BedRecord rec) {
    if (rec.getAnnotations() != null && rec.getAnnotations().length > 0) {
      return rec.getAnnotations()[0];
    }
    return rec.getSequenceName() + ":" + (rec.getStart() + 1) + "-" + rec.getEnd();
  }
}
