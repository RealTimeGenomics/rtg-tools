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

/**
 * DNA implementation for FASTA symbol table.
 */
public class DNAFastaSymbolTable extends FastaSymbolTable {

  @Override
  public final byte[] getAsciiToOrdinalTable() {
    return getAsciiToOrdinalTable(this);
  }

  @Override
  public final byte[] getOrdinalToAsciiTable() {
    return getOrdinalToAsciiTable(DNA.values());
  }

  @Override
  public final Residue scanResidue(int code) {
    switch (code & ~32) {
      case 'A':
        return DNA.A;
      case 'C':
        return DNA.C;
      case 'G':
        return DNA.G;
      case 'T':
      case 'U':
        return DNA.T;

      // Explicitly map the IUPAC ambiguity codes to prevent "unrecognized symbol" warnings.
      // Currently map to N, giving same SDF as previous releases. In future it may be
      // worth either retaining enough bits to permit compatibile MD5 computation.
      case 'W':
      case 'M':
      case 'R':
      case 'D':
      case 'H':
      case 'V':
      case 'B':
      case 'S':
      case 'Y':
      case 'K':

      case 'N':
        return DNA.N;
      default:
    }
    return null;
  }

  @Override
  public final Residue unknownResidue() {
    return DNA.N;
  }

  @Override
  public final SequenceType getSequenceType() {
    return SequenceType.DNA;
  }
}

