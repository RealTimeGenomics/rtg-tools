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


import junit.framework.TestCase;

/**
 *
 */
public class DNAFastaSymbolTableTest extends TestCase {

  /**
   */
  public DNAFastaSymbolTableTest(final String name) {
    super(name);
  }

  /**
   * Test method for {@link DNAFastaSymbolTable#scanResidue(int)}.
   */
  public final void testScanResidue() {
    DNAFastaSymbolTable t = new DNAFastaSymbolTable();
    assertEquals(DNA.A, t.scanResidue('A'));
    assertEquals(DNA.C, t.scanResidue('C'));
    assertEquals(DNA.G, t.scanResidue('G'));
    assertEquals(DNA.N, t.scanResidue('N'));
    assertEquals(DNA.T, t.scanResidue('T'));

    assertNull(t.scanResidue('J'));
    assertNull(t.scanResidue('O'));
  }

  /**
   * Test method for {@link DNAFastaSymbolTable#unknownResidue()}.
   */
  public final void testUnknownResidue() {
    DNAFastaSymbolTable table = new DNAFastaSymbolTable();
    assertEquals(DNA.N, table.unknownResidue());
  }

  /**
   * Test method for {@link DNAFastaSymbolTable#getSequenceType()}.
   */
  public final void testGetSequenceType() {
    DNAFastaSymbolTable t = new DNAFastaSymbolTable();
    assertEquals(SequenceType.DNA, t.getSequenceType());
  }

  /**
   * Test method for {@link DNAFastaSymbolTable#getAsciiToOrdinalTable()}
   */
  public final void testGetAsciiToOrdinalTable() {
    final DNAFastaSymbolTable t = new DNAFastaSymbolTable();
    final byte[] table = t.getAsciiToOrdinalTable();
    assertEquals(255, table.length);
    for (int i = 0; i < 255; i++) {
      final Residue r = t.scanResidue(i);
      if (r == null) {
        assertEquals((byte) 255, table[i]);
      } else {
        assertEquals((byte) r.ordinal(), table[i]);
      }
    }
  }

  /**
   * Test method for {@link DNAFastaSymbolTable#getOrdinalToAsciiTable()}
   */
  public final void testGetOrdinalToAsciiTable() {
    final DNAFastaSymbolTable t = new DNAFastaSymbolTable();
    final byte[] table = t.getOrdinalToAsciiTable();
    assertEquals(5, table.length);
    for (int k = 0; k < table.length; k++) {
      assertEquals((byte) DNA.values()[k].toString().charAt(0), table[k]);
    }
  }

}
