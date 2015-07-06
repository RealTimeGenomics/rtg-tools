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

import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.Utils;
import com.rtg.util.integrity.IntegralAbstract;
import com.rtg.util.intervals.SequenceNameLocus;

/**
 */
public class MockVariant extends IntegralAbstract implements Variant {

  private final int mStart;
  private final int mEnd;
  private final byte[] mPlus;
  private final byte[] mMinus;
  private final boolean mPhased;

  /**
   * @param start one-based start position of mutation
   * @param end one-based end position of mutation
   * @param plus nucleotides on the plus strand
   * @param minus nucleotides on the minus strand
   * @param phased does this call have phasing information
   */
  public MockVariant(int start, int end, byte[] plus, byte[] minus, boolean phased) {
    super();
    mStart = start - 1;
    mEnd = end - 1;
    mPlus = Arrays.copyOf(plus, plus.length);
    if (minus != null) {
      mMinus = Arrays.copyOf(minus, minus.length);
    } else {
      mMinus = null;
    }
    mPhased = phased;
  }
  /**
   * Assumes not phased
   * @param start one-based start position of mutation
   * @param end one-based end position of mutation
   * @param plus nucleotides on the plus strand
   * @param minus nucleotides on the minus strand
   */
  public MockVariant(int start, int end, byte[] plus, byte[] minus) {
    this(start, end, plus, minus, false);
  }

  @Override
  public int getEnd() {
    return mEnd;
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(String sequence, int pos) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getLength() {
    return getEnd() - getStart();
  }

  @Override
  public byte[] nt(boolean strand) {
    return strand ? ntAlleleA() : ntAlleleB();
  }

  @Override
  public byte[] ntAlleleB() {
    if (mMinus != null) {
      return Arrays.copyOf(mMinus, mMinus.length);
    } else {
      return null;
    }
  }

  @Override
  public byte[] ntAlleleA() {
    return Arrays.copyOf(mPlus, mPlus.length);
  }

  @Override
  public int getStart() {
    return mStart;
  }

  @Override
  public boolean integrity() {
    return true;
  }

  @Override
  public void toString(StringBuilder sb) {
    sb.append(getStart() + 1).append(":").append(getEnd() + 1).append(" ");
    sb.append(DnaUtils.bytesToSequenceIncCG(mPlus));
    if (mMinus != null) {
      sb.append(":").append(DnaUtils.bytesToSequenceIncCG(mMinus));
    }
  }

  @Override
  public boolean isPhased() {
    return mPhased;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mStart, mEnd);
  }

  @Override
  public boolean equals(final Object o2) {
    if (this == o2) {
      return true;
    }
    if (o2 == null) {
      return false;
    }
    if (!(o2 instanceof MockVariant)) {
      return false;
    }
    final MockVariant other = (MockVariant) o2;
    return mStart == other.mStart && mEnd == other.mEnd;
  }

  @Override
  public String getSequenceName() {
    return "";
  }
}
