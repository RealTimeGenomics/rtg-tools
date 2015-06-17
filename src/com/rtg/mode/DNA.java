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

import com.rtg.util.PseudoEnum;

/**
 * Individual nucleotides including an unknown value.
 */
public abstract class DNA implements Residue, PseudoEnum {
  //It is important that the ordinal of the unknown value be 0 (it is used in the hashing code).
  //Do not modify unless you have really thought about it
  /** Unknown. */
  public static final DNA N = new DNA(0, DNASimple.N) {
    @Override
    public boolean ignore() {
      return true;
    }
    @Override
    public DNA complement() {
      return N;
    }
  };

  /** adenine. */
  public static final DNA A = new DNA(1, DNASimple.A)  {
    @Override
    public DNA complement() {
      return T;
    }
  };

  /** cytosine */
  public static final DNA C  = new DNA(2, DNASimple.C) {
    @Override
    public DNA complement() {
      return G;
    }
  };

  /** guanine */
  public static final DNA G  = new DNA(3, DNASimple.G) {
    @Override
    public DNA complement() {
      return C;
    }
  };

  /** thymine or in RNA uracil (no distinction is drawn here). */
  public static final DNA T  = new DNA(4, DNASimple.T) {
    @Override
    public DNA complement() {
      return A;
    }
  };


  private static final DNA[] VALUES = {N, A, C, G, T};

  /**
   * Record fast way of getting complements directly from codes.
   */
  static final byte[] COMPLEMENT = new byte[DNA.values().length];
  static {
    populateComplementArray(COMPLEMENT, DNA.values());
  }

  static void populateComplementArray(byte[] comp, DNA[] orig) {
    for (final DNA dna : orig) {
      comp[dna.ordinal()] = (byte) dna.complement().ordinal();
    }
  }

  /**
   * Get all the values in this pseudo-enumeration.
   * @return the set of values in ordinal order.
   */
  public static DNA[] values() {
    return VALUES.clone();
  }

  /**
   * Create an array that enables internal codes to be converted to external
   * letters for the nucleotide.
   * @return the array of chars.
   */
  public static char[] valueChars() {
    final char[] chars = new char[VALUES.length];
    for (int i = 0; i < VALUES.length; i++) {
      chars[i] = VALUES[i].toString().charAt(0);
    }
    return chars;
  }

  /**
   * Return the complement of a byte coded residue.
   *
   * @param code code to get complement of
   * @return complement value
   */
  public static byte complement(final byte code) {
    return COMPLEMENT[code];
  }

  /**
   * In place complement (NOT REVERSE COMPLEMENT)
   * @param codes array to complement
   * @param offset position in array to start
   * @param length number of bytes to complement
   */
  public static void complementInPlace(final byte[] codes, final int offset, final int length) {
    for (int i = offset; i < offset + length; i++) {
      codes[i] = COMPLEMENT[codes[i]];
    }
  }

  /**
   * In place reverse complement
   * @param codes array to reverse complement
   * @param offset position in array to start
   * @param length number of bytes to complement
   */
  public static void reverseComplementInPlace(final byte[] codes, final int offset, final int length) {
    for (int i = offset, j = offset + length - 1; i <= j; i++, j--) {
      final byte tmp = codes[j];
      codes[j] = COMPLEMENT[codes[i]];
      codes[i] = COMPLEMENT[tmp];
    }
  }

  /**
   * In place reverse complement
   * @param codes array to reverse complement
   */
  public static void reverseComplementInPlace(final byte[] codes) {
    reverseComplementInPlace(codes, 0, codes.length);
  }

  /**
   * Mimics the <code>valueOf</code> in an enumeration.
   * @param str to be converted.
   * @return corresponding value.
   * @throws IllegalArgumentException if string doesn't correspond to a value.
   */
  public static DNA valueOf(final String str) {
    if (str.length() != 1) {
      throw new IllegalArgumentException();
    }
    return valueOf(str.charAt(0));
  }

  /**
   * Mimics the <code>valueOf</code> in an enumeration.
   * @param ch character to be converted.
   * @return corresponding value.
   * @throws IllegalArgumentException if string doesn't correspond to a value.
   */
  public static DNA valueOf(final char ch) {
    switch(ch) {
    case 'N':
    case 'n':
      return N;
    case 'A':
    case 'a':
      return A;
    case 'C':
    case 'c':
      return C;
    case 'G':
    case 'g':
      return G;
    case 'T':
    case 't':
      return T;
    default:
      throw new IllegalArgumentException(ch + "");
    }
  }

  private final int mOrdinal;

  private final DNASimple mSimple;

  DNA(final int ordinal, final DNASimple simple) {
    mOrdinal = ordinal;
    mSimple = simple;
  }

  @Override
  public boolean ignore() {
    return false;
  }

  @Override
  public SequenceType type() {
    return SequenceType.DNA;
  }

  /**
   * Get the underlying simple type.
   * @return the underlying simple type.
   */
  public DNASimple simpleType() {
    return mSimple;
  }

  @Override
  public String name() {
    return toString();
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  @Override
  public String toString() {
    return simpleType().toString();
  }

  /**
   * Get the complementary nucleotide.
   * @return the complementary nucleotide.
   */
  public abstract DNA complement();

  /**
   * Convert a string containing valid nucleotide characters into a  byte array
   * with values (0=N, ... 4=T).
   * @param dna the string to be converted.
   * @return the byte array.
   */
  public static byte[] stringDNAtoByte(final String dna) {
    final byte[] dnaBytes = new byte[dna.length()];
    for (int i = 0; i < dna.length(); i++) {
      dnaBytes[i] = (byte) getDNA(dna.charAt(i));
    }
    return dnaBytes;
  }

  /**
   * Convert a byte array containing valid nucleotide characters into a byte array
   * with values (0=N, ... 4=T).
   * @param dna the string to be converted.
   * @return the byte array.
   */
  public static byte[] byteDNAtoByte(final byte[] dna) {
    final byte[] dnaBytes = new byte[dna.length];
    for (int i = 0; i < dna.length; i++) {
      dnaBytes[i] = (byte) getDNA((char) dna[i]);
    }
    return dnaBytes;
  }

  /**
   * Get an integer in the range (0=N, ... 4=T)
   * @param charAt character to be converted.
   * @return integer representation of nucleotide.
   */
  public static int getDNA(final char charAt) {
    return valueOf(charAt).ordinal();
  }
}

