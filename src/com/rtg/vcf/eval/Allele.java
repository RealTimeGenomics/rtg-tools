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
import com.rtg.mode.DnaUtils;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;

/**
 * Hold a variant allele, one allele of a genotype.
 */
@TestClass("com.rtg.vcf.eval.VariantTest")
public class Allele extends SequenceNameLocusSimple {

  private final byte[] mNt;

  public Allele(String seq, int start, int end, byte[] nt) {
    super(seq, start, end);
    if (nt == null) {
      throw new NullPointerException();
    }
    mNt = nt;
  }

  public Allele(SequenceNameLocus locus, byte[] nt) {
    this(locus.getSequenceName(), locus.getStart(), locus.getEnd(), nt);
  }

  /**
   * @return the bases of the allele (may be zero length for a deletion)
   */
  byte[] nt() {
    return mNt;
  }

  public int compareTo(Allele that) {
    final int varPos = this.getStart() - that.getStart();
    if (varPos != 0) {
      return varPos;
    }

    final int length = this.mNt.length - that.mNt.length;
    if (length != 0) {
      return length;
    }
    for (int i = 0; i < length; i++) {
      final int diff = this.mNt[i] - that.mNt[i];
      if (diff != 0) {
        return diff;
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return String.valueOf(getStart() + 1) + ":" + (getEnd() + 1) + "(" + DnaUtils.bytesToSequenceIncCG(mNt) + ")";
  }
}
