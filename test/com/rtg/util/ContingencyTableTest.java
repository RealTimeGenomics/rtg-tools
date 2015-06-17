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
package com.rtg.util;

import junit.framework.TestCase;

/**
 */
public class ContingencyTableTest extends TestCase {

  public void test() {
    ContingencyTable eval = new ContingencyTable(100, 50, 25, 10);

    assertEquals(100.0, eval.truePositives());
    assertEquals(50.0, eval.falsePositives());
    assertEquals(25.0, eval.trueNegatives());
    assertEquals(10.0, eval.falseNegatives());
    assertEquals(125.0, eval.correct());
    assertEquals(60.0, eval.incorrect());

    assertEquals(185.0, eval.total());

    assertEquals(0.67, eval.accuracy(), 0.01);
    assertEquals(0.33, eval.errorRate(), 0.01);

    final double precision = ContingencyTable.precision(eval.truePositives(), eval.falsePositives());
    assertEquals(0.67, precision, 0.01);
    final double recall = ContingencyTable.recall(eval.truePositives(), eval.falseNegatives());
    assertEquals(0.91, recall, 0.01);
    assertEquals(0.77, ContingencyTable.fMeasure(precision, recall), 0.01);
  }
}
