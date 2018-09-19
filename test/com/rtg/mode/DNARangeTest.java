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

import com.rtg.util.integrity.IntegerRange;
import com.rtg.util.integrity.IntegerRangeTest;

/**
 */
public class DNARangeTest extends IntegerRangeTest {

  @Override
  protected IntegerRange getRange() {
    return new DNARange(0, 4);
  }

  public void testGeneral() {
    final IntegerRange ir = getRange();
    if (ir.hasInvalid()) {
      assertEquals(-1, ir.invalid());
    }
    assertEquals(0, ir.low());
  }

  public void testToChar() {
    final DNARange ir = (DNARange) getRange();
    for (int i = ir.low(); i <= ir.high(); ++i) {
      assertEquals(ir.toString(i), "" + ir.toChar(i));
      assertEquals(i, ir.valueOf(ir.toChar(i)));
    }
  }

  public void testValidExplicit() {
    assertTrue(DNARange.valid(0, DNARange.N, DNARange.T));
    assertTrue(DNARange.valid(1, DNARange.N, DNARange.T));
    assertTrue(DNARange.valid(1, DNARange.N, DNARange.T));

    assertFalse(DNARange.valid(-1, DNARange.N, DNARange.T));
    assertFalse(DNARange.valid(6, DNARange.N, DNARange.T));
  }

  public void testComplement() {
    assertEquals(DNARange.N, DNARange.complement(DNARange.N));
    assertEquals(DNARange.T, DNARange.complement(DNARange.A));
    assertEquals(DNARange.G, DNARange.complement(DNARange.C));
    assertEquals(DNARange.C, DNARange.complement(DNARange.G));
    assertEquals(DNARange.A, DNARange.complement(DNARange.T));
  }
}
