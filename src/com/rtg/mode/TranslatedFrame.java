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

import static com.rtg.mode.DNA.COMPLEMENT;

import java.util.Arrays;

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * The possible ways a nucleotide sequence can be translated to a protein.
 * (Forward and reverse and 3 different phases).
 *
 */
public abstract class TranslatedFrame implements Frame, PseudoEnum {
//public abstract class TranslatedFrame implements Frame<Residue>, PseudoEnum {
  /**
   * Normal forward direction - phase 1.
   */
  public static final TranslatedFrame FORWARD1 = new TranslatedFrame(0, "FORWARD1", "+1", 0) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      final int x = firstValid + index + 2;
      if (x >= length) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      return codonToAmino(codes[firstValid + index], codes[firstValid + index + 1], codes[x]);
    }

    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidForward(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidForward(offset, length);
    }

    @Override
    public boolean isForward() {
      return true;
    }

    @Override
    public Frame getReverse() {
      return REVERSE1;
    }


  };

  /**
   * Normal forward direction - phase 2.
   */
  public static final TranslatedFrame FORWARD2 = new TranslatedFrame(1, "FORWARD2", "+2", 1) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      final int x = firstValid + index + 3;
      if (x >= length) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      return codonToAmino(codes[firstValid + index + 1], codes[firstValid + index + 2], codes[x]);
    }
    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidForward(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidForward(offset, length);
    }
    @Override
    public boolean isForward() {
      return true;
    }


    @Override
    public Frame getReverse() {
      return REVERSE2;
    }
  };

  /**
   * Normal forward direction - phase 3.
   */
  public static final TranslatedFrame FORWARD3 = new TranslatedFrame(2, "FORWARD3", "+3", 2) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      final int x = firstValid + index + 4;
      if (x >= length) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
      return codonToAmino(codes[index + 2], codes[index + 3], codes[x]);

    }
    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidForward(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidForward(offset, length);
    }

    @Override
    public boolean isForward() {
      return true;
    }
    @Override
    public Frame getReverse() {
      return REVERSE3;
    }
  };

  /**
   * Reverse - phase 1.
   */
  public static final TranslatedFrame REVERSE1 = new TranslatedFrame(3, "REVERSE1", "-1", 0) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
//      final int i = length - index - 1;
      final int i = lastValid - index - 1;
      return codonToAmino(COMPLEMENT[codes[i]], COMPLEMENT[codes[i - 1]], COMPLEMENT[codes[i - 2]]);
    }

    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidReverse(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidReverse(offset, length, fullLength);
    }

    @Override
    public boolean isForward() {
      return false;
    }

    @Override
    public Frame getReverse() {
      return FORWARD1;
    }
  };

  /**
   * Reverse - phase 2.
   */
  public static final TranslatedFrame REVERSE2 = new TranslatedFrame(4, "REVERSE2", "-2", 1) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
//      final int i = length - index  - 2;
      final int i = lastValid - index - 2;
      return codonToAmino(COMPLEMENT[codes[i]], COMPLEMENT[codes[i - 1]], COMPLEMENT[codes[i - 2]]);
    }
    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidReverse(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidReverse(offset, length, fullLength);
    }
    @Override
    public boolean isForward() {
      return false;
    }
    @Override
    public Frame getReverse() {
      return FORWARD2;
    }
  };

  /**
   * Reverse - phase 3.
   */
  public static final TranslatedFrame REVERSE3 = new TranslatedFrame(5, "REVERSE3", "-3", 2) {
    @Override
    public byte code(final byte[] codes, final int length, final int index) {
      return code(codes, length, index, 0, length);
    }

    @Override
    public byte code(byte[] codes, int length, int index, int firstValid, int lastValid) {
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException(index);
      }
//      final int i = length - index  - 3;
      final int i = lastValid - index - 3;
      return codonToAmino(COMPLEMENT[codes[i]], COMPLEMENT[codes[i - 1]], COMPLEMENT[codes[i - 2]]);
    }
    @Override
    public int calculateFirstValid(int offset, int length, int fullLength) {
      return calculateFirstValidReverse(offset, length, fullLength);
    }

    @Override
    public int calculateLastValid(int offset, int length, int fullLength) {
      return calculateLastValidReverse(offset, length, fullLength);
    }
    @Override
    public boolean isForward() {
      return false;
    }
    @Override
    public Frame getReverse() {
      return FORWARD3;
    }
  };

  private final String mDisplay;

  private final int mPhase;

  private final int mOrdinal;

  private final String mName;

  private TranslatedFrame(final int ordinal, final String name, final String display, final int phase) {
    mDisplay = display;
    mPhase = phase;
    mOrdinal = ordinal;
    mName = name;
  }

  @Override
  public String toString() {
    return mName;
  }

  @Override
  public String name() {
    return mName;
  }

  @Override
  public int ordinal() {
    return mOrdinal;
  }

  private static final EnumHelper<TranslatedFrame> HELPER = new EnumHelper<>(TranslatedFrame.class, new TranslatedFrame[] {FORWARD1, FORWARD2, FORWARD3, REVERSE1, REVERSE2, REVERSE3});

  /**
   * See {@link java.lang.Enum#valueOf(Class, String)}.
   * @param str name of the enum value
   * @return the enum value
   */
  public static TranslatedFrame valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  /**
   * @return list of the enum values
   */
  public static TranslatedFrame[] values() {
    return HELPER.values();
  }

  @Override
  public String display() {
    return mDisplay;
  }

  @Override
  public int phase() {
    return mPhase;
  }

  static int calculateFirstValidForward(int offset, int length, int fullLength) {
    return f(offset);
  }

  static int calculateFirstValidReverse(int offset, int length, int fullLength) {
    return 0;
    //return (fullLength - offset) % 3;
  }

  // series is 0, 2, 1, 0, 2, 1
  // this is because at offset 1 our new array position is 2 to get the first nucleotide
  // in frame FORWARD1, similarly for offset 2 the array position is 1 and so on
  private static int f(int x) {
    return (2 * x + 3) % 3;
  }

  static int calculateLastValidForward(int offset, int length) {
    return length;
    //return length - ((offset + length) % 3);
  }

  static int calculateLastValidReverse(int offset, int length, int fullLength) {
    return length - f(fullLength - (offset + length));
  }


  private static final TranslatedFrame[] INTERNAL_VALUES = HELPER.values();

  /**
   * Get the frame corresponding to the integer value.
   * @param value int value
   * @return the frame
   */
  static TranslatedFrame frameFromCode(final int value) {
    return INTERNAL_VALUES[value];
  }

  /**
   * Number of bits needed to represent a DNA enumeration including the
   * unknown value.
   */
  static final int DNA_UNKNOWN_BITS = 3;

  static final int DNA_UNKNOWN_BITS2 = 2 * DNA_UNKNOWN_BITS;

  static final byte FILL_BYTE = -1;

  static final byte[] CODON_TO_AMINO;
  static {
    CODON_TO_AMINO = populateCodonAminoArray(DNA.values());
  }

  static byte[] populateCodonAminoArray(DNA[] values) {
    final byte[] ret = new byte[1 << (DNA_UNKNOWN_BITS * 3)]; //default byte of 0 is also the default protein code for unknown
    Arrays.fill(ret, FILL_BYTE); //put in an invalid value helps with mutation testing
    for (final DNA c1 : values) {
      final int x1 = c1.ordinal() << DNA_UNKNOWN_BITS2;
      for (final DNA c2 : values) {
        final int x2 = c2.ordinal() << DNA_UNKNOWN_BITS;
        for (final DNA c3 : values) {
          final int x3 = c3.ordinal();
          final int x = x1 | x2 | x3;
          ret[x] = (byte) codonToAmino(c1, c2, c3).ordinal();
        }
      }
    }
    return ret;
  }

  /**
   * Converts three characters of a codon into the corresponding amino
   * acid.
   *
   * @param c1 the <code>char</code> symbol for the first base in the
   * codon.
   * @param c2 the <code>char</code> symbol for the second base in the
   * codon.
   * @param c3 the <code>char</code> symbol for the third base in the
   * codon.
   * @return the <code>char</code> symbol for the corresponding amino
   * acid.
   */
  public static byte codonToAmino(final byte c1, final byte c2, final byte c3) {
    //System.err.println("codonToAmino c1=" + c1 + ":" + DNA.values()[c1] + " c2=" + c2+ ":" + DNA.values()[c2] + " c3=" + c3+ ":" + DNA.values()[c3]);
    final int x1 = c1 << DNA_UNKNOWN_BITS2;
    final int x2 = c2 << DNA_UNKNOWN_BITS;
    final int x = x1 | x2 | c3;
    //System.err.println("codonToAmino x=" + x + " res=" + res+ ":" + Protein.values()[res]);
    return CODON_TO_AMINO[x];
  }

  /**
   * Converts three characters of a codon into the corresponding amino
   * acid.
   * This version uses the enumerations to allow more careful checking.
   * @param c1a the first base in the codon.
   * @param c2a the second base in the codon.
   * @param c3a the third base in the codon.
   * @return the <code>char</code> symbol for the corresponding amino
   * acid.
   */
  static Protein codonToAmino(final DNA c1a, final DNA c2a, final DNA c3a) {
    final DNASimple c1 = c1a.simpleType();
    final DNASimple c2 = c2a.simpleType();
    final DNASimple c3 = c3a.simpleType();

    switch (c1) {
    case A:
      switch (c2) {
      case A:
        switch (c3) {
        case A: case G: return Protein.K;
        case C: case T: return Protein.N;
        default: return Protein.X;
        }
      case C:
        return Protein.T;
      case G:
        switch (c3) {
        case A: case G: return Protein.R;
        case C: case T: return Protein.S;
        default: return Protein.X;
        }
      case T:
        switch (c3) {
        case A: case C: case T: return Protein.I;
        case G: return Protein.M;
        default: return Protein.X;
        }
      default: return Protein.X;
      }
    case C:
      switch (c2) {
      case A:
        switch (c3) {
        case A: case G: return Protein.Q;
        case C: case T: return Protein.H;
        default: return Protein.X;
        }
      case C:
        return Protein.P;
      case G:
        return Protein.R;
      case T:
        return Protein.L;
      default: return Protein.X;
      }
    case G:
      switch (c2) {
      case A:
        switch (c3) {
        case A: case G: return Protein.E;
        case C: case T: return Protein.D;
        default: return Protein.X;
        }
      case C:
        return Protein.A;
      case G:
        return Protein.G;
      case T:
        return Protein.V;
      default: return Protein.X;
      }
    case T:
      switch (c2) {
      case A:
        switch (c3) {
        case A: case G: return Protein.STOP;
        case C: case T: return Protein.Y;
        default: return Protein.X;
        }
      case C:
        return Protein.S;
      case G:
        switch (c3) {
        case A: return Protein.STOP;
        case G: return Protein.W;
        case C: case T: return Protein.C;
        default: return Protein.X;
        }
      case T:
        switch (c3) {
        case A: case G: return Protein.L;
        case C: case T: return Protein.F;
        default: return Protein.X;
        }
      default: return Protein.X;
      }
    default: return Protein.X;
    }
  }


}

