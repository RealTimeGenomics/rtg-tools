/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import com.rtg.mode.DNA;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Stores the specifics of a generated diploid mutation
 */
public class MutatorResult extends IntegralAbstract {
  private static final char[] DNA_CHARS = DNA.valueChars();
  private final byte[] mFirstHaplotype;
  private final byte[] mSecondHaplotype;
  private final int mConsumed;

  /**
   * @param firstHaplotype the sequence generated for the first haplotype
   * @param secondHaplotype the sequence generated for the second haplotype
   * @param consumed how many bases on the template were consumed
   */
  MutatorResult(byte[] firstHaplotype, byte[] secondHaplotype, int consumed) {
    super();
    mFirstHaplotype = firstHaplotype.clone();
    mSecondHaplotype = secondHaplotype.clone();
    mConsumed = consumed;
    assert integrity();
  }

  /**
   * @return Returns the first haplotype.
   */
  public byte[] getFirstHaplotype() {
    return mFirstHaplotype.clone();
  }

  /**
   * @return Returns the second haplotype.
   */
  public byte[] getSecondHaplotype() {
    return mSecondHaplotype.clone();
  }
  /**
   * Get consumed.
   * @return Returns the consumed.
   */
  public int getConsumed() {
    return mConsumed;
  }

  @Override
  public final boolean integrity() {
    Exam.assertTrue(mConsumed >= 0);
    Exam.assertNotNull(mFirstHaplotype);
    Exam.assertNotNull(mSecondHaplotype);
    checkHaplotype(mFirstHaplotype);
    checkHaplotype(mSecondHaplotype);
    return true;
  }

  static void checkHaplotype(byte[] nt) {
    for (final byte b : nt) {
      Exam.assertTrue(0 <= b && b <= 4);
    }
  }

  static void haplotypeToString(final StringBuilder sb, final byte[] haplotypes) {
    for (byte haplotype : haplotypes) {
      sb.append(DNA_CHARS[haplotype]);
    }
  }

  @Override
  public void toString(StringBuilder sb) {
    sb.append(mConsumed).append(":");
    haplotypeToString(sb, mFirstHaplotype);
    sb.append(":");
    haplotypeToString(sb, mSecondHaplotype);
  }

}
