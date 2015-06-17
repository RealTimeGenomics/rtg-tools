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

/**
 * Format an integer, left justifying and padding it out to the
 * specified length as necessary. If it is too long leave it as is.
 * Optionally insert ","s. Usage: <code>FormatInteger format =
 * FormatInteger(3); .... System.out.println(format.format(-1);
 * //outputs " -1" .... StringBuilder sb = ... format.format(sb,-23);
 * //puts "-23" in string buffer.</code>
 *
 */
public class FormatIntegerLeft extends FormatInteger {

  /**
   * Construct a format for an integer.
   *
   * @param in number of positions (including - sign).
   */
  public FormatIntegerLeft(final int in) {
    this(in, false);
  }


  /**
   * Construct a format for a real.
   *
   * @param in number of positions before decimal point (including
   * - sign).
   * @param group true iff grouping to be used (as in 1,234 vs 1234)
   */
  public FormatIntegerLeft(final int in, final boolean group) {
    super(in, group);
  }


  /**
   * Format long into StringBuilder
   *
   * @param sb StringBuilder to which formatted value is to be appended
   * @param w long to be formatted
   * @return StringBuilder that has been modified (standard convention)
   */
  @Override
  public StringBuilder format(final StringBuilder sb, final long w) {
    final int initPosition = sb.length();
    sb.append(mLocalFormat.format(w));
    final int currLength = sb.length() - initPosition;

    //pad at right as necessary.
    final int pad = mLength - currLength;
    if (pad > 0) {
      sb.append(mPadding, 0, pad);
    }
    return sb;
  }
}
