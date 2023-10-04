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

import static com.rtg.util.StringUtils.LS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.util.TestUtils;

/**
 */
public class RocSlopeTest extends AbstractNanoTest {

  private static final String ROC0 = (""
      + "10 1.0 1 seq1 1" + LS
      + "").replaceAll(" ", "\t");

  public void test0() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      RocSlope.writeSlope(new ByteArrayInputStream(ROC0.getBytes()), ps);
    }
    mNano.check("roc-slope-0.txt", TestUtils.sanitizeTsvHeader(baos.toString()));
  }

  private static final String ROC1 = ("#ignore me I'm a header" + LS
      + "10 1.0 0 seq1 1" + LS
      + "9 2.0 0 seq1 1" + LS
      + "9 3.0 0 seq1 1" + LS
      + "#ignore me too!" + LS
      + "9 3.0 1 seq1 1" + LS
      + "8 4.0 1 seq1 1" + LS
      + "7 5.0 1 seq1 1" + LS
      + "").replaceAll(" ", "\t");

  public void test1() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      RocSlope.writeSlope(new ByteArrayInputStream(ROC1.getBytes()), ps);
    }
    mNano.check("roc-slope-1.txt", TestUtils.sanitizeTsvHeader(baos.toString()));
  }

  private static final String ROC2 = ("#ignore me I'm a header" + LS
      + "10 1.0 0 seq1 1" + LS
      + "9 2.0 0 seq1 1" + LS
      + "9 3.0 0 seq1 1" + LS
      + "#ignore me too!" + LS
      + "9 3.0 1 seq1 1" + LS
      + "8 4.0 1 seq1 1" + LS
      + "7 5.0 1 seq1 1" + LS
      + "6 5.0 2 seq1 1" + LS
      + "5 8.0 2 seq1 1" + LS
      + "4 8.0 4 seq1 1" + LS
      + "3 10.0 4 seq1 1" + LS
      + "2 10.0 5 seq1 1" + LS
      + "").replaceAll(" ", "\t");

  public void test2() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      RocSlope.writeSlope(new ByteArrayInputStream(ROC2.getBytes()), ps);
    }
    mNano.check("roc-slope-2.txt", TestUtils.sanitizeTsvHeader(baos.toString()));
  }

  private static final String ROC3 = ("#ignore me I'm a header" + LS
      + "10 0.0 0 seq1 1" + LS
      + "9 0.0 1 seq1 1" + LS
      + "8 3.0 1 seq1 1" + LS
      + "7 3.0 2 seq1 1" + LS
      + "6 5.0 2 seq1 1" + LS
      + "5 5.0 3 seq1 1" + LS
      + "4 6.0 3 seq1 1" + LS
      + "").replaceAll(" ", "\t");

  public void test3() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      RocSlope.writeSlope(new ByteArrayInputStream(ROC3.getBytes()), ps);
    }
    mNano.check("roc-slope-3.txt", TestUtils.sanitizeTsvHeader(baos.toString()));
  }

  private static final String ROC3_A = ("#ignore me I'm a header" + LS
      + "10 0.0 0 seq1 1" + LS
      + "8 3.0 1 seq1 1" + LS
      + "6 5.0 2 seq1 1" + LS
      + "4 6.0 3 seq1 1" + LS
      + "").replaceAll(" ", "\t");
  public void test3a() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (PrintStream ps = new PrintStream(baos)) {
      RocSlope.writeSlope(new ByteArrayInputStream(ROC3_A.getBytes()), ps);
    }
    mNano.check("roc-slope-3a.txt", TestUtils.sanitizeTsvHeader(baos.toString()));
  }

}
