/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.vcf.annotation;

import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class SimpleTandemRepeatAnnotatorTest extends TestCase {

  private int str(final String ref, final int pos) {
    return SimpleTandemRepeatAnnotator.strBidrectional(ref.getBytes(StandardCharsets.US_ASCII), pos, 0);
  }

  public void testStr() {
    assertEquals(0, str("A", 0));
    assertEquals(0, str("AC", 0));
    assertEquals(0, str("AC", 0));
    assertEquals(0, str("ACG", 0));
    assertEquals(0, str("ACGT", 0));
    assertEquals(0, str("ACGT", 1));
    assertEquals(3, str("AAAA", 0));
    assertEquals(3, str("AAAAT", 0));
    assertEquals(3, str("AAAAT", 2));
    assertEquals(2, str("CCAAT", 2));
    assertEquals(4, str("ACTACTACTACTACTACC", 0));
    assertEquals(4, str("ACTACTACTACTACTACG", 15));
    assertEquals(4, str("GACTACTACTACTACTACG", 16));
    assertEquals(10, str("CCTTTTTTGGGGGGCCC", 8));
    assertEquals(100, str("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 0));
  }
}
