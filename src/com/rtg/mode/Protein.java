/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import java.util.HashMap;
import java.util.Map;

import com.rtg.util.PseudoEnum;

/**
 * Individual amino acids.
 * These conform to the standard unambiguous
 * <a href="http://www.dna.affrc.go.jp/misc/MPsrch/InfoIUPAC.html">single letter IUPAC codes</a>
 * As SLIM does not currently support ambiguous codes the two ambiguous codes
 * "B" and "Z" should be translated to "X".
 */
public class Protein implements Residue, Comparable<Protein>, PseudoEnum {

  /** Unknown */
  public static final Protein X = new XProtein(0, "X", "Xaa", "Any amino acid");


  private static class XProtein extends Protein {
    XProtein(final int ordinal, final String ename,  final String sname, final String name) {
      super(ordinal, ename, sname, name);
    }
    @Override
    public boolean ignore() {
      return true;
    }
  }

  /** Stop (from translated DNA) */
  public static final Protein STOP = new STOPProtein(1, "STOP", "***", "Translated stop codon");

  private static class STOPProtein extends Protein {
    STOPProtein(final int ordinal, final String ename, final String sname, final String name) {
      super(ordinal, ename, sname, name);
    }
    @Override
    public boolean ignore() {
      return true;
    }

    @Override
    public String toString() {
      return "*";
    }
  }

  /** Alanine */
  public static final Protein A = new Protein(2, "A", "Ala", "Alanine");

  /** Arginine */
  public static final Protein R = new Protein(3, "R", "Arg", "Arginine");

  /** Asparagine */
  public static final Protein N = new Protein(4, "N", "Asn", "Asparagine");

  /** Aspartic acid */
  public static final Protein D = new Protein(5, "D", "Asp", "Aspartic acid");

  /** Cysteine */
  public static final Protein C = new Protein(6, "C", "Cys", "Cysteine");

  /** Glutamine */
  public static final Protein Q = new Protein(7, "Q", "Gln", "Glutamine");

  /** Glutamic acid */
  public static final Protein E = new Protein(8, "E", "Glu", "Glutamic acid");

  /** Glycine */
  public static final Protein G = new Protein(9, "G", "Gly", "Glycine");

  /** Histidine */
  public static final Protein H = new Protein(10, "H", "His", "Histidine");

  /** Isoleucine */
  public static final Protein I = new Protein(11, "I", "Ile", "Isoleucine");

  /** Leucine */
  public static final Protein L = new Protein(12, "L", "Leu", "Leucine");

  /** Lysine */
  public static final Protein K = new Protein(13, "K", "Lys", "Lysine");

  /** Methionine */
  public static final Protein M = new Protein(14, "M", "Met", "Methionine");

  /** Phenylalanine */
  public static final Protein F = new Protein(15, "F", "Phe", "Phenylalanine");

  /** Proline */
  public static final Protein P = new Protein(16, "P", "Pro", "Proline");

  /** Serine */
  public static final Protein S = new Protein(17, "S", "Ser", "Serine");

  /** Threonine */
  public static final Protein T = new Protein(18, "T", "Thr", "Threonine");

  /** Tryptophan */
  public static final Protein W = new Protein(19, "W", "Trp", "Tryptophan");

  /** Tyrosine */
  public static final Protein Y = new Protein(20, "Y", "Tyr", "Tyrosine");

  /** Valine */
  public static final Protein V = new Protein(21, "V", "Val", "Valine");

  private final String mThreeLetter;

  private final String mFullName;

  private final int mOrdinal;

  private final String mEnumName;

  protected Protein(final int ordinal, final String ename, final String threeLetter, final String fullName) {
    mThreeLetter = threeLetter;
    mFullName = fullName;
    mOrdinal = ordinal;
    mEnumName = ename;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  @Override
  public boolean ignore() {
    return false;
  }

  @Override
  public SequenceType type() {
    return SequenceType.PROTEIN;
  }

  /**
   * @return the list of enum values
   */
  public static Protein[] values() {
    return new Protein[] {X, STOP, A, R, N, D, C, Q, E, G, H, I, L, K, M, F, P, S, T, W, Y, V};
  }

  private static final Map<String, Protein> VALUE_OF = new HashMap<>();

  static {
    for (final Protein p : values()) {
      VALUE_OF.put(p.toString(), p);
    }
  }

  /**
   * Get the Protein singleton with the specified value (aka name).
   * @param str the name of a Protein singleton.
   * @return the singleton Protein
   * @throws IllegalArgumentException if str is not a valid name.
   */
  public static Protein valueOf(final String str) {
    final Protein res = VALUE_OF.get(str);
    if (res == null) {
      throw new IllegalArgumentException(str);
    }
    return res;
  }

  @Override
  public String toString() {
    return mEnumName;
  }

  @Override
  public String name() {
    return toString();
  }

  /**
   * Get the IUPAC standard three letter code.
   * @return the IUPAC standard three letter code.
   */
  public String threeLetter() {
    return mThreeLetter;
  }

  /**
   * Get the IUPAC standard full name.
   * @return the IUPAC standard full name.
   */
  public String fullName() {
    return mFullName;
  }

  @Override
  public int compareTo(final Protein p) {
    return Integer.compare(ordinal(), p.ordinal());
  }

  @Override
  public boolean equals(final Object p) {
    if (!(p instanceof Protein)) {
      return false;
    }
    final Protein po = (Protein) p;
    return ordinal() == po.ordinal();
  }

  @Override
  public int hashCode() {
    return ordinal();
  }



  private static final byte UNKNOWN_RESIDUE = 0;
  private static final char[] PBASES = {
    Character.toLowerCase(Protein.values()[0].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[1].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[2].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[3].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[4].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[5].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[6].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[7].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[8].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[9].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[10].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[11].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[12].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[13].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[14].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[15].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[16].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[17].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[18].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[19].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[20].toString().charAt(0)),
    Character.toLowerCase(Protein.values()[21].toString().charAt(0)),
  };

  /**
   * Convert an internal amino acid code into an ascii character.
   * @param a array of amino acid internal codes.
   * @param p index into <code>a</code>
   * @return a lowercase char
   */
  public static int pbase(final byte[] a, final int p) {
    return p >= 0 && p < a.length ? PBASES[a[p]] : PBASES[UNKNOWN_RESIDUE];
  }

  /**
   * Convert an ascii amino acid character into internal encoding
   * @param c the amino acid character.
   * @return the encoded value
   */
  public static byte encodeProtein(final char c) {
    return (byte) valueOf(String.valueOf(c)).ordinal();
  }

  /**
   * Convert an ascii amino acid string into internal encoding
   * @param str the amino acid sequence.
   * @return the encoded value
   */
  public static byte[] encodeProteins(final String str) {
    final byte[] b = new byte[str.length()];
    for (int i = 0; i < str.length(); ++i) {
      b[i] = encodeProtein(str.charAt(i));
    }
    return b;
  }
}

