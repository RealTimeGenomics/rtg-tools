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
package com.rtg.util.format;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Some simple utilities for formatting fixed width integer numbers
 * This is serious about it being fixed width - pads with spaces at
 * start to achieve this Puts "#" around an output that is too large.
 * Usage: <code>FormatInteger format = FormatInteger(3); ....
 * System.out.println(format.format(-1); //outputs " -1" ....
 * StringBuilder sb = ... format.format(sb,-23); //puts "-23" in string
 * buffer.</code>
 *
 */
public class FormatInteger {

  /**
   * Convert a number into a bit string with separators between each group of 8.
   *
   * @param x number to be displayed
   * @return string giving bit decomposition of x.
   */
  public static String toBits(final long x) {
    final StringBuilder sb = new StringBuilder();
    long t = x;
    for (int i = 0; i < 64; i++) {
      if ((i & 7) == 0 && i > 0) {
        sb.append(":");
      }
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  //static final FieldPosition POS = new FieldPosition(DecimalFormat.INTEGER_FIELD);

  protected final DecimalFormat mLocalFormat = (DecimalFormat) NumberFormat.getInstance();

  /** total length expected for resulting string */
  protected final int mLength;

  /** Enough spaces for any padding needed */
  protected final char[] mPadding;

  private final String mBlanks;

  /**
   * Construct a format for an integer.
   *
   * @param in number of positions (including - sign).
   */
  public FormatInteger(final int in) {
    this(in, false);
  }


  /**
   * Construct a format for a real.
   *
   * @param in number of positions before decimal point (including
   * - sign).
   * @param group true iff grouping to be used (as in 1,234 vs 1234)
   */
  public FormatInteger(final int in, final boolean group) {
    if (in < 0) {
      throw new IllegalArgumentException("leading digits is negative:" + in);
    }
    mLength = in;
    mPadding = new char[mLength];
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mLength; i++) {
      mPadding[i] = ' ';
      sb.append(" ");
    }
    mBlanks = sb.toString();
    //fill with spaces

    mLocalFormat.setGroupingUsed(group);
  }


  /**
   * Format long into String
   *
   * @param w long to be formatted
   * @return String containing formatted number
   */
  public String format(final long w) {
    final StringBuilder sb = new StringBuilder();
    format(sb, w);
    return sb.toString();
  }


  /**
   * Format long into StringBuilder
   *
   * @param sb StringBuilder to which formatted value is to be appended
   * @param w long to be formatted
   * @return StringBuilder that has been modified (standard convention)
   */
  public StringBuilder format(final StringBuilder sb, final long w) {
    final int initPosition = sb.length();
    sb.append(mLocalFormat.format(w));
    final int currLength = sb.length() - initPosition;
    //System.err.println(length+":"+w+":"+sb);
    if (currLength == mLength) {
      return sb;
    }
    if (currLength > mLength) {
      //too long - make sure visible to user
      sb.insert(initPosition, '#');
      sb.append('#');
    } else {
      //too short pad with leading spaces
      sb.insert(initPosition, mPadding, 0, mLength - currLength);
      assert sb.length() - initPosition == mLength;
    }
    return sb;
  }

  /**
   * Spaces equal to length that numbers will be formatted.
   *
   * @return String containing spaces.
   */
  public String blanks() {
    return mBlanks;
  }


  /**
   * Place blanks equal in length to a formatted integer into StringBuilder
   *
   * @param sb StringBuilder to which blanks to be appended
   * @return StringBuilder that has been modified (standard convention)
   */
  public StringBuilder blanks(final StringBuilder sb) {
    sb.append(mBlanks);
    return sb;
  }

}
