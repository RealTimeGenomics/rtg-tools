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

import com.reeltwo.jumble.annotations.TestClass;


/**
 * Interface for FASTA symbol table.
 *
 */
@TestClass("com.rtg.mode.DNAFastaSymbolTableTest")
public abstract class FastaSymbolTable {
  /**
   * Takes an <code>int</code> code and returns the associated residue
   * @param code byte read from FASTA file
   * @return residue representing code
   */
  public abstract Residue scanResidue(final int code);

  /**
   * Returns the residue to use for unknown values
   * @return the residue
   */
  public abstract Residue unknownResidue();

  /**
   * Returns the sequence type for current table
   * @return the sequence type
   */
  public abstract SequenceType getSequenceType();

  /**
   * Returns a table that can be used to map from the internal code
   * (ordinal value) to an ASCII character representation. The table
   * will have one entry per possible residue.
   * @return the mapping table.
   */
  public abstract byte[] getOrdinalToAsciiTable();

  /**
   * Returns a table that can be used to map from the character
   * representation to an internal code (the ordinal of the
   * residue). Any unrecognized characters will map in this table to
   * value 255.
   * @return the mapping table.
   */
  public abstract byte[] getAsciiToOrdinalTable();

  // Creates map from ascii representation to internal byte code
  static byte[] getAsciiToOrdinalTable(FastaSymbolTable table) {
    final byte[] mapping = new byte[255];
    Residue r;
    for (int i = 0; i < 255; i++) {
      r = table.scanResidue(i);
      if (r == null) {
        mapping[i] = (byte) 255;
      } else {
        mapping[i] = (byte) r.ordinal();
      }
    }
    return mapping;
  }

  // Creates map from internal byte code to ascii representation
  static byte[] getOrdinalToAsciiTable(Residue[] residues) {
    final byte[] mapping = new byte[residues.length];
    for (int k = 0; k < mapping.length; k++) {
      mapping[k] = (byte) residues[k].toString().charAt(0);
    }
    return mapping;
  }
}
