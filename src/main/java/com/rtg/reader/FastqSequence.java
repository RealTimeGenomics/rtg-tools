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

package com.rtg.reader;

import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.DnaUtils;
import com.rtg.util.Utils;

/**
 */ // Bundles up a single sequence
class FastqSequence {
  private String mName;
  private byte[] mBases;
  private byte[] mQualities;

  FastqSequence(String name, byte[] read, byte[] qualities, int length) {
    mName = name;
    mBases = Arrays.copyOf(read, length);
    mQualities = Arrays.copyOf(qualities, length);
  }

  public void rc() {
    DNA.reverseComplementInPlace(mBases);
    Utils.reverseInPlace(mQualities);
  }

  public void trim(ReadTrimmer trimmer) {
    final int newLen = trimmer.trimRead(mBases, mQualities, mBases.length);
    mBases = Arrays.copyOf(mBases, newLen);
    mQualities = Arrays.copyOf(mQualities, newLen);
  }

  public String toFasta() {
    return ">" + mName + "\n"
      + DnaUtils.bytesToSequenceIncCG(mBases) + "\n";
  }

  public String toFastq() {
    return "@" + mName + "\n"
      + DnaUtils.bytesToSequenceIncCG(mBases) + "\n"
      + "+" + "\n"
      + FastaUtils.rawToAsciiString(mQualities) + "\n";
  }

  public int length() {
    return mBases.length;
  }

  public String toString() {
    return toFastq();
  }

  public String getName() {
    return mName;
  }

  public byte[] getBases() {
    return mBases;
  }

  public byte[] getQualities() {
    return mQualities;
  }

  public void append(FastqSequence other, String suffix) {
    if (other.length() > 0) {
      final int oldLen = length();
      final int newLen = oldLen + other.length();
      mBases = Arrays.copyOf(mBases, newLen);
      mQualities = Arrays.copyOf(mQualities, newLen);
      System.arraycopy(other.mBases, 0, mBases, oldLen, other.length());
      System.arraycopy(other.mQualities, 0, mQualities, oldLen, other.length());
      if (suffix != null) {
        mName += suffix;
      }
    }
  }
}
