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

package com.rtg.mode;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.integrity.IntegerRange;

/**
 * Class for dealing with integer subranges representing
 * DNA values (<code>NACGTD</code>).
 * Uses an array to specify the single character values for each value.
 *
 */
@TestClass(value = {"com.rtg.mode.DNARangeTest", "com.rtg.mode.GeneralDNARangeTest"})
public class DNARange extends IntegerRange {

  private static final String VALUES = "NACGT";

  /** N unknown nucleotide. */
  public static final byte N = 0;

  /** A nucleotide. */
  public static final byte A = 1;

  /** C nucleotide. */
  public static final byte C = 2;

  /** G nucleotide. */
  public static final byte G = 3;

  /** T nucleotide. */
  public static final byte T = 4;

  /** Range for standard DNA encoding. */
  public static final DNARange RANGE = new DNARange(N, T);

  /**
   * Check that a nucleotide is in the correct range.
   * @param nt nucleotide to be checked.
   * @param lo first allowable value (inclusive).
   * @param hi last allowable value (inclusive).
   * @return true iff nt is in the allowed range.
   */
  public static boolean valid(final int nt, final int lo, final int hi) {
    assert N <= lo && lo <= hi && hi <= T;
    return lo <= nt && nt <= hi;
  }

  /**
   * @param nt nucleotide.
   * @return the complement of nt.
   */
  public static byte complement(final byte nt) {
    if (nt == N) {
      return N;
    }
    return (byte) (5 - nt);
  }


  /**
   * @param start first valid value (inclusive).
   * @param end last valid value (inclusive).
   */
  public DNARange(final int start, final int end) {
    super(false, -1, start, end);
  }


  @Override
  public String toString(int i) {
    return VALUES.substring(i, i + 1);
  }

  /**
   * Get a single character representation of nucleotide.
   * @param i value to be converted.
   * @return the character corresponding to the valid value i.
   */
  public char toChar(int i) {
    return VALUES.charAt(i);
  }

  @Override
  public int valueOf(String str) {
    final int res = VALUES.indexOf(str);
    if (res < 0) {
      throw new RuntimeException("Invalid string:" + str);
    }
    return res;
  }

  /**
   * Convert value of ch as an integer when interpreted as a DNA character.
   * Should obey the equation <code>i = valueOf(toString(i).charAt(0))</code>.
   * @param ch to be converted.
   * @return value.
   */
  public byte valueOf(char ch) {
    assert VALUES.length() <= Byte.MAX_VALUE;
    final byte res = (byte) VALUES.indexOf(ch);
    if (res < 0) {
      throw new RuntimeException("Invalid string:" + ch);
    }
    return res;
  }
}
