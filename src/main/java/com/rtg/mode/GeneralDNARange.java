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
 * DNA values. Uses an array to specify the single character values for each
 * value.
 *
 */
@TestClass(value = {"com.rtg.mode.DNARangeTest", "com.rtg.mode.GeneralDNARangeTest"})
public class GeneralDNARange extends IntegerRange {

  private final String mValues;

  /**
   * @param values string of the (single) character values.
   * @param invalid true iff an invalid value is allowed.
   */
  GeneralDNARange(final String values, final boolean invalid) {
    super(invalid, -1, 0, values.length() - 1);
    mValues = values;
  }

  @Override
  public String toString(int i) {
    return mValues.substring(i, i + 1);
  }

  /**
   * Get a single character representation of nucleotide.
   * @param i value to be converted.
   * @return the character corresponding to the valid value i.
   */
  public char toChar(int i) {
    return mValues.charAt(i);
  }

  @Override
  public int valueOf(String str) {
    final int res = mValues.indexOf(str);
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
  public int valueOf(char ch) {
    final int res = mValues.indexOf(ch);
    if (res < 0) {
      throw new RuntimeException("Invalid string:" + ch);
    }
    return res;
  }
}
