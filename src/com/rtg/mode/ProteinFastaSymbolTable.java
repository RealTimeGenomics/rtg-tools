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
 * Protein implementation for FASTA symbol table.
 */
public class ProteinFastaSymbolTable extends FastaSymbolTable {

  @Override
  public final byte[] getAsciiToOrdinalTable() {
    return getAsciiToOrdinalTable(this);
  }

  @Override
  public final byte[] getOrdinalToAsciiTable() {
    return getOrdinalToAsciiTable(Protein.values());
  }


  @Override
  public Residue scanResidue(final int code) {
    // This is done first, so that ~32 does not interfere!
    if (code == '*') {
      return Protein.STOP;
    }
    switch (code & ~32) {
    case 'A':
      return Protein.A;
    case 'C':
      return Protein.C;
    case 'D':
      return Protein.D;
    case 'E':
      return Protein.E;
    case 'F':
      return Protein.F;
    case 'G':
      return Protein.G;
    case 'H':
      return Protein.H;
    case 'I':
      return Protein.I;
    case 'K':
      return Protein.K;
    case 'L':
      return Protein.L;
    case 'M':
      return Protein.M;
    case 'N':
      return Protein.N;
    case 'P':
      return Protein.P;
    case 'Q':
      return Protein.Q;
    case 'R':
      return Protein.R;
    case 'S':
      return Protein.S;
    case 'T':
      return Protein.T;
    case 'V':
      return Protein.V;
    case 'W':
      return Protein.W;
    case 'Y':
      return Protein.Y;
    case 'X': //unknown residue
    case 'Z': //Z has 2 options
    case 'B': //B has 2 options
      return Protein.X;
    default:
      return null;
    }
  }
  @Override
  public Residue unknownResidue() {
    return Protein.X;
  }
  @Override
  public SequenceType getSequenceType() {
    return SequenceType.PROTEIN;
  }
}
