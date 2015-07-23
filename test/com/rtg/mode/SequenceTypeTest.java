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
package com.rtg.mode;

import static com.rtg.mode.SequenceType.DNA;
import static com.rtg.mode.SequenceType.PROTEIN;

import java.io.ObjectStreamException;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class SequenceTypeTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.SequenceType}.
   * @throws ObjectStreamException
   */
  public final void test() throws ObjectStreamException {
    TestUtils.testPseudoEnum(SequenceType.class, "[DNA, PROTEIN]");
    assertEquals(DNA, DNA.readResolve());
    assertEquals(PROTEIN, PROTEIN.readResolve());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceType#numberKnownCodes()}.
   */
  public final void testNumberKnownCodes() {
    assertEquals(4, DNA.numberKnownCodes());
    assertEquals(5, DNA.numberCodes());
    assertEquals(com.rtg.mode.DNA.values().length, DNA.firstValid() + DNA.numberKnownCodes());
    assertEquals(20, PROTEIN.numberKnownCodes());
    assertEquals(22, PROTEIN.numberCodes());
    assertEquals(com.rtg.mode.Protein.values().length, PROTEIN.firstValid() + PROTEIN.numberKnownCodes());
    assertEquals("DNA", DNA.name());
  }

  /**
   * Test method for {@link com.rtg.mode.SequenceType#bits()}.
   */
  public final void testBits() {
    check(DNA);
    check(PROTEIN);
  }

  private void check(final SequenceType st) {
    final int b = st.bits();
    assertTrue(b < 64);
    final long cb = 1 << b;
    final int n = st.numberKnownCodes();
    assertTrue(cb >= n);
    final int bm = b - 1;
    final long cbm = 1 << bm;
    assertTrue(cbm < n);
  }
}

