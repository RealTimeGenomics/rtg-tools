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

import com.rtg.util.intervals.SequenceNameLocus;

/** Wraps a variant that had to be skipped from path finding due to being within a region of very dense variation */
final class SkippedVariant implements VariantId {
  private final Variant mVariant;

  SkippedVariant(Variant v) {
    mVariant = v;
  }

  /** @return the underlying variant that was skipped */
  public Variant variant() {
    return mVariant;
  }

  @Override
  public int getId() {
    return mVariant.getId();
  }

  @Override
  public String getSequenceName() {
    return mVariant.getSequenceName();
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    return mVariant.overlaps(other);
  }

  @Override
  public boolean contains(String sequence, int pos) {
    return mVariant.contains(sequence, pos);
  }

  @Override
  public int getStart() {
    return mVariant.getStart();
  }

  @Override
  public int getEnd() {
    return mVariant.getEnd();
  }

  @Override
  public int getLength() {
    return mVariant.getLength();
  }
}
