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
 * DNA utility functions.
 *
 */
public final class DnaUtils {

  private static final char[] BASES_LOWER = {
    Character.toLowerCase(DNA.values()[0].toString().charAt(0)),
    Character.toLowerCase(DNA.values()[1].toString().charAt(0)),
    Character.toLowerCase(DNA.values()[2].toString().charAt(0)),
    Character.toLowerCase(DNA.values()[3].toString().charAt(0)),
    Character.toLowerCase(DNA.values()[4].toString().charAt(0)),
  };
  private static final char[] BASES = {
    Character.toUpperCase(DNA.values()[0].toString().charAt(0)),
    Character.toUpperCase(DNA.values()[1].toString().charAt(0)),
    Character.toUpperCase(DNA.values()[2].toString().charAt(0)),
    Character.toUpperCase(DNA.values()[3].toString().charAt(0)),
    Character.toUpperCase(DNA.values()[4].toString().charAt(0)),
  };

  /** Value for the unknown residue */
  public static final byte UNKNOWN_RESIDUE = 0;

  private DnaUtils() { }

  /**
   * Take reverse complement of a string.
   * @param seq to be reverse complemented.
   * @return the reversed sequence.
   */
  public static String reverseComplement(final String seq) {
    final StringBuilder sb = new StringBuilder();
    for (int i = seq.length() - 1; i >= 0; i--) {
      final char c = seq.charAt(i);
      switch (c) {
      case 'a':
        sb.append('t');
        break;
      case 'A':
        sb.append('T');
        break;
      case 'c':
        sb.append('g');
        break;
      case 'C':
        sb.append('G');
        break;
      case 'g':
        sb.append('c');
        break;
      case 'G':
        sb.append('C');
        break;
      case 't':
        sb.append('a');
        break;
      case 'T':
        sb.append('A');
        break;
      case '-':
        break;
      default: // n
        sb.append(c);
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Convert from a 0-4 based numerical base to an uppercase <code>NACGT</code> style character.
   * @param b byte representing the numerical value of the base
   * @return character representation of the byte (<code>NACGT</code>)
   */
  public static char getBase(final int b) {
    return BASES[b];
  }

  /**
   * Converts an encoded base into a uppercase <code>ACGTN</code> character.
   * Handles all the off-the-end cases by returning 'N'.
   *
   * @param a array of encoded residues.
   * @param p position of the desired base in <code>a</code>.
   * @return a character form of the base, or 'N'.
   */
  public static char base(final byte[] a, final int p) {
    return p >= 0 && p < a.length ? getBase(a[p]) : getBase(UNKNOWN_RESIDUE);
  }

  /**
   * Convert from a 0-4 based numerical base to a lowercase <code>nacgt</code> style character.
   * Note, we don't generally output lowercase bases, so if possible use <code>getBase</code> instead.
   * @param b byte representing the numerical value of the base
   * @return character representation of the byte (<code>nacgt</code>)
   */
  public static char getBaseLower(final int b) {
    return BASES_LOWER[b];
  }

  /**
   * Convert a binary DNA sequence to a human readable string using uppercase characters
   * @param seq DNA in internal 0..4 bytes
   * @param start offset into array
   * @param length length of DNA
   * @return readable string
   */
  public static String bytesToSequenceIncCG(final byte[] seq, final int start, final int length) {
    final StringBuilder sb = new StringBuilder();
    for (int i = start; i < start + length; i++) {
      if (i < 0 || i >= seq.length) {
        sb.append('N');
      } else if (seq[i] != 5) { //allow for CG spacer, but strip it
        sb.append(BASES[seq[i]]);
      }
    }
    return sb.toString();
  }

  /**
   * Convert a binary DNA sequence to a human readable string using uppercase characters
   * @param seq DNA in internal 0..4 bytes
   * @return readable string
   */
  public static String bytesToSequenceIncCG(final byte[] seq) {
    return bytesToSequenceIncCG(seq, 0, seq.length);
  }

  /**
   * Transform a human-readable DNA sequence into internal 0..4 bytes.
   * @param str Eg. <code>"ACGTN"</code> will become {1,2,3,4,0}.
   * @return the encoded array
   */
  public static byte[] encodeString(final String str) {
    return DnaUtils.encodeArray(str.getBytes());
  }

  /**
   * Transform a human-readable DNA sequence into internal 0..4 bytes.
   * Hyphens are ignored and can be used to space sequence data for readability in tests.
   * @param str Eg. <code>"ACGTN"</code> will become {1,2,3,4,0}.
   * @return the encoded array
   */
  public static byte[] encodeStringWithHyphen(final String str) {
    return DnaUtils.encodeString(str.replace("-", ""));
  }

  /**
   * Transform (in-situ) a human-readable DNA sequence into internal 0..4 bytes.
   * @param a Eg. {'a','c','g','t','n'} will become {1,2,3,4,0}.
   * @param length length to convert
   * @return the encoded array (which will be the same array as <code>a</code>)
   */
  public static byte[] encodeArray(final byte[] a, final int length) {
    return encodeArray(a, a, 0, length);
  }

  /**
   * Transform a human-readable DNA sequence into internal 0..4 bytes.
   * @param a Eg. {'a','c','g','t','n'} will become {1,2,3,4,0}.
   * @param dest array transformed sequence will be written into
   * @param start start position in source array to transform from
   * @param length length to convert
   * @return the encoded array (which will be the same array as <code>dest</code>)
   */
  public static byte[] encodeArray(final byte[] a, final byte[] dest, final int start, final int length) {
    for (int k = 0; k < length; k++) {
      switch (a[k + start]) {
        case (byte) 'a':
        case (byte) 'A':
          dest[k] = 1;
          break;
        case (byte) 'c':
        case (byte) 'C':
          dest[k] = 2;
          break;
        case (byte) 'g':
        case (byte) 'G':
          dest[k] = 3;
          break;
        case (byte) 't':
        case (byte) 'T':
          dest[k] = 4;
          break;
        case (byte) 'n':
        case (byte) 'N':
          dest[k] = 0;
          break;
        default:
          throw new IllegalArgumentException(Character.valueOf((char) a[k + start]).toString());
      }
    }
    return dest;
  }

  /**
   * Transform a human-readable DNA sequence into the reverse complemented version, uppercase.
   * @param src Eg. {'a','c','g','t','n'} will become {'N', 'A', 'C', 'G', 'T'}.
   * @param dest destination byte array
   * @param length length to convert
   */
  public static void reverseComplement(byte[] src, byte[] dest, int length) {
    for (int k = 0; k < length; k++) {
      switch (src[k]) {
      case (byte) 'a':
      case (byte) 'A':
        dest[length - 1 - k] = 'T';
        break;
      case (byte) 'c':
      case (byte) 'C':
        dest[length - 1 - k] = 'G';
        break;
      case (byte) 'g':
      case (byte) 'G':
        dest[length - 1 - k] = 'C';
        break;
      case (byte) 't':
      case (byte) 'T':
        dest[length - 1 - k] = 'A';
        break;
      default:
        dest[length - 1 - k] = 'N';
        break;
      }
    }
  }

  /**
   * Transform (in-situ) a human-readable DNA sequence into internal 0..4 bytes.
   * @param a Eg. {'a','c','g','t','n'} will become {1,2,3,4,0}.
   * @return the encoded array (which will be the same array as <code>a</code>)
   */
  public static byte[] encodeArray(final byte[] a) {
    return encodeArray(a, a.length);
  }

  /**
   * Transform a human-readable DNA sequence into internal 0..4 bytes without modifying original.
   * @param a Eg. {'a','c','g','t','n'} will become {1,2,3,4,0}.
   * @return the encoded array which will be a new array of length <code>a.length</code>
   */
  public static byte[] encodeArrayCopy(final byte[] a) {
    return encodeArray(a, new byte[a.length], 0, a.length);
  }

  /**
   * Transform a human-readable DNA sequence into internal 0..4 bytes without modifying original.
   * @param a Eg. {'a','c','g','t','n'} will become {1,2,3,4,0}.
   * @param start start position in <code>a</code> to transform from
   * @param length number of bases to transform
   * @return the encoded array which will be a new array of length <code>length</code>
   */
  public static byte[] encodeArrayCopy(final byte[] a, int start, int length) {
    return encodeArray(a, new byte[length], start, length);
  }

}
