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
public class ProteinFastaSymbolTableTest extends TestCase {

  /**
   */
  public ProteinFastaSymbolTableTest(final String name) {
    super(name);
  }

  /**
   * Test method for {@link ProteinFastaSymbolTable#scanResidue(int)}.
   */
  public final void testScanResidue() {
    ProteinFastaSymbolTable t = new ProteinFastaSymbolTable();
    assertEquals(Protein.A, t.scanResidue('A'));
    assertEquals(Protein.C, t.scanResidue('C'));
    assertEquals(Protein.D, t.scanResidue('D'));
    assertEquals(Protein.E, t.scanResidue('E'));
    assertEquals(Protein.F, t.scanResidue('F'));
    assertEquals(Protein.G, t.scanResidue('G'));
    assertEquals(Protein.H, t.scanResidue('H'));
    assertEquals(Protein.I, t.scanResidue('I'));
    assertEquals(Protein.K, t.scanResidue('K'));
    assertEquals(Protein.L, t.scanResidue('L'));
    assertEquals(Protein.M, t.scanResidue('M'));
    assertEquals(Protein.N, t.scanResidue('N'));
    assertEquals(Protein.P, t.scanResidue('P'));
    assertEquals(Protein.Q, t.scanResidue('Q'));
    assertEquals(Protein.R, t.scanResidue('R'));
    assertEquals(Protein.S, t.scanResidue('S'));
    assertEquals(Protein.T, t.scanResidue('T'));
    assertEquals(Protein.V, t.scanResidue('V'));
    assertEquals(Protein.W, t.scanResidue('W'));
    assertEquals(Protein.Y, t.scanResidue('Y'));
    assertEquals(Protein.X, t.scanResidue('X'));
    assertEquals(Protein.X, t.scanResidue('Z'));
    assertEquals(Protein.X, t.scanResidue('B'));

    assertNull(t.scanResidue('J'));
    assertNull(t.scanResidue('O'));
    assertNull(t.scanResidue('U'));

    assertEquals(Protein.STOP, t.scanResidue('*'));
  }

  /**
   * Test method for {@link ProteinFastaSymbolTable#unknownResidue()}.
   */
  public final void testUnknownResidue() {
    ProteinFastaSymbolTable table = new ProteinFastaSymbolTable();
    assertEquals(Protein.X, table.unknownResidue());
  }

  /**
   * Test method for {@link ProteinFastaSymbolTable#getSequenceType()}.
   */
  public final void testGetSequenceType() {
    ProteinFastaSymbolTable t = new ProteinFastaSymbolTable();
    assertEquals(SequenceType.PROTEIN, t.getSequenceType());
  }

  /**
   * Test method for {@link ProteinFastaSymbolTable#getAsciiToOrdinalTable()}
   */
  public final void testGetAsciiToOrdinalTable() {
    final ProteinFastaSymbolTable t = new ProteinFastaSymbolTable();
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
   * Test method for {@link ProteinFastaSymbolTable#getOrdinalToAsciiTable()}
   */
  public final void testGetOrdinalToAsciiTable() {
    final ProteinFastaSymbolTable t = new ProteinFastaSymbolTable();
    final byte[] table = t.getOrdinalToAsciiTable();
    assertEquals(22, table.length);
    for (int k = 0; k < table.length; k++) {
      assertEquals((byte) Protein.values()[k].toString().charAt(0), table[k]);
    }
  }

}
