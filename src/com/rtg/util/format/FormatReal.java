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

/**
 * Some simple utilities for formatting fixed width real numbers This
 * is serious about it being fixed width - pads with spaces at start to
 * achieve this Puts "#" around an output that is too large. Usage:
 * <code>
 * FormatReal format = FormatReal(3,2); ....
 * System.out.println(format.format(-1.234); //outputs " -1.23" ....
 * StringBuilder sb = ... format.format(sb,-23.456); //puts "-23.45" in
 * string buffer.
 * </code>
 *
 */
public class FormatReal {

  private final DecimalFormat mLocalFormat; // = new DecimalFormat("#0.#####;-0.#####");

  /** total length expected for resulting string */
  private final int mLength;

  /** Enough spaces for any padding needed */
  private final char[] mPadding;

  /**
   * Construct a format for a real.
   *
   * @param in number of positions before decimal point (including
   * - sign).
   * @param iff number of positions after decimal point.
   */
  public FormatReal(final int in, final int iff) {
    if (in < 0) {
      throw new IllegalArgumentException("leading digits is negative:" + in);
    }
    if (iff < 0) {
      throw new IllegalArgumentException("trailing digits is negative:" + iff);
    }
    mLength = 1 + in + iff;
    mPadding = new char[mLength];
    for (int i = 0; i < mLength; i++) {
      mPadding[i] = ' ';
    }
    //fill with spaces

    final StringBuilder formatpos = new StringBuilder();
    final StringBuilder formatneg = new StringBuilder();
    formatpos.append("#");
    formatneg.append("-");
    for (int i = 2; i < in; i++) {
      formatpos.append("#");
      formatneg.append("#");
    }
    formatpos.append("0.");
    formatneg.append("0.");
    for (int i = 0; i < iff; i++) {
      formatpos.append("0");
      formatneg.append("0");
    }
    final String format = formatpos.toString() + ";" + formatneg.toString();
    //System.err.println(format);
    mLocalFormat = new DecimalFormat(format);
  }


  /**
   * Return String formatted output
   *
   * @param w number
   * @return formatted number
   */
  public String format(final double w) {
    final StringBuilder sb = new StringBuilder();
    format(sb, w);
    return sb.toString();
  }

  /**
   * Format double into StringBuilder
   *
   * @param sb StringBuilder to which formatted value is to be appended
   * @param w double to be formatted
   * @return StringBuilder that has been modified (standard convention)
   */
  public StringBuilder format(final StringBuilder sb, final double w) {
    final int initPosition = sb.length();
    if (Double.isNaN(w)) {
      sb.append("NaN");
    } else if (Double.isInfinite(w)) {
      sb.append(w < 0.0 ? "-Infinity" : "Infinity");
    } else {
      sb.append(mLocalFormat.format(w));
    }
    final int currLength = sb.length() - initPosition;
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
   * Format float into StringBuilder
   *
   * @param sb StringBuilder to which formatted value is to be appended
   * @param w float to be formatted
   * @return StringBuilder that has been modified (standard convention)
   */
  public StringBuilder format(final StringBuilder sb, final float w) {
    final int initPosition = sb.length();
    if (Float.isNaN(w)) {
      sb.append("NaN");
    } else if (Float.isInfinite(w)) {
      sb.append(w < 0.0 ? "-Infinity" : "Infinity");
    } else {
      sb.append(mLocalFormat.format(w));
    }
    final int currLength = sb.length() - initPosition;
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
}
