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
package com.rtg.jmx;

import java.io.IOException;
import java.text.NumberFormat;

/**
 * Utility methods for formatting data from management beans.
 */
public final class MonUtils {

  private MonUtils() { }

  // Formatter with 0 fraction digits
  static final NumberFormat NF0 = NumberFormat.getInstance();
  // Formatter with 1 fraction digits
  static final NumberFormat NF1 = NumberFormat.getInstance();
  // Formatter with 2 fraction digits
  static final NumberFormat NF2 = NumberFormat.getInstance();
  static {
    NF0.setGroupingUsed(false);
    NF0.setMinimumIntegerDigits(1);
    NF0.setMaximumFractionDigits(0);
    NF0.setMinimumFractionDigits(0);
    NF1.setGroupingUsed(false);
    NF1.setMinimumIntegerDigits(1);
    NF1.setMaximumFractionDigits(1);
    NF1.setMinimumFractionDigits(1);
    NF2.setGroupingUsed(false);
    NF2.setMinimumIntegerDigits(1);
    NF2.setMaximumFractionDigits(2);
    NF2.setMinimumFractionDigits(2);
  }

  static void padRight(Appendable out, String str, int width) throws IOException {
    padRight(out, str, width, ' ');
  }
  static void padRight(Appendable out, String str, int width, char pchar) throws IOException {
    out.append(str);
    for (int i = str.length(); i < width; ++i) {
      out.append(pchar);
    }
  }

  static void pad(Appendable out, String str, int width) throws IOException {
    pad(out, str, width, ' ');
  }
  static void pad(Appendable out, String str, int width, char pchar) throws IOException {
    for (int i = str.length(); i < width; ++i) {
        out.append(pchar);
    }
    out.append(str);
  }
}
