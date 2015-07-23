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
    case (int) 'A':
      return Protein.A;
    case (int) 'C':
      return Protein.C;
    case (int) 'D':
      return Protein.D;
    case (int) 'E':
      return Protein.E;
    case (int) 'F':
      return Protein.F;
    case (int) 'G':
      return Protein.G;
    case (int) 'H':
      return Protein.H;
    case (int) 'I':
      return Protein.I;
    case (int) 'K':
      return Protein.K;
    case (int) 'L':
      return Protein.L;
    case (int) 'M':
      return Protein.M;
    case (int) 'N':
      return Protein.N;
    case (int) 'P':
      return Protein.P;
    case (int) 'Q':
      return Protein.Q;
    case (int) 'R':
      return Protein.R;
    case (int) 'S':
      return Protein.S;
    case (int) 'T':
      return Protein.T;
    case (int) 'V':
      return Protein.V;
    case (int) 'W':
      return Protein.W;
    case (int) 'Y':
      return Protein.Y;
    case (int) 'X': //unknown residue
    case (int) 'Z': //Z has 2 options
    case (int) 'B': //B has 2 options
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
