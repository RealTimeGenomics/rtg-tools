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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.rtg.util.PseudoEnum;

/**
 * Distinguishes DNA/RNA sequences from Protein sequences.
 */
public abstract class SequenceType implements Serializable, PseudoEnum {

  /** DNA or RNA nucleotide sequences. */
  public static final SequenceType DNA = new SequenceType(0, "DNA") {
    @Override
    public int numberKnownCodes() {
      return com.rtg.mode.DNA.values().length - firstValid();
    }

    @Override
    public int numberCodes() {
      return com.rtg.mode.DNA.values().length;
    }

    @Override
    public int bits() {
      return 2;
    }

    @Override
    public int firstValid() {
      return 1;
    }
  };

  /** Polypeptide sequences. */
  public static final SequenceType PROTEIN = new SequenceType(1, "PROTEIN") {
    @Override
    public int numberKnownCodes() {
      return com.rtg.mode.Protein.values().length - firstValid();
    }

    @Override
    public int numberCodes() {
      return com.rtg.mode.Protein.values().length;
    }

    @Override
    public int bits() {
      return 5;
    }

    @Override
    public int firstValid() {
      return 2;
    }
  };

  private static final Map<String, SequenceType> VALUE_OF = new HashMap<>();

  private static final SequenceType[] VALUES = {DNA, PROTEIN};

  /**
   * Generate array of all the possible SequenceMode singletons.
   * These are in the same ordering as ordinal().
   * @return array of all the possible SequenceMode singletons.
   */
  public static SequenceType[] values() {
    return VALUES.clone();
  }

  static {
    for (final SequenceType sm : values()) {
      VALUE_OF.put(sm.toString(), sm);
    }
  }


  /**
   * Get the SequenceMode singleton with the specified value (aka name).
   * @param str the name of a SequenceMode singleton.
   * @return the singleton SequenceMode
   * @throws IllegalArgumentException if str is not a valid name.
   */
  public static SequenceType valueOf(final String str) {
    final SequenceType res = VALUE_OF.get(str);
    if (res == null) {
      throw new IllegalArgumentException(str);
    }
    return res;
  }


  private final int mOrdinal;

  private final String mToString;

  private SequenceType(final int ordinal, final String toString) {
    mOrdinal = ordinal;
    mToString = toString;
  }

  @Override
  public String name() {
    return toString();
  }

  /**
   * @return ordinal with same semantics as in enumerations.
   */
  @Override
  public int ordinal() {
    return mOrdinal;
  }

  /**
   * Get the number of codes for this type which are valid residues, that is,
   * they are not ignored (there will be 1 or more others which are
   * ignored during searching).
   * @return number of valid codes for this type.
   */
  public abstract int numberKnownCodes();

  /**
   * Get the total number of codes.
   * @return number of codes for this type.
   */
  public abstract int numberCodes();

  /**
   * The number of bits needed to encode the valid values.
   * That is
   * <pre>
   * 2^bits gt;= numberKnownCodes and 2^(bits -1) &lt; numberKnownCodes
   * </pre>
   * @return minimum number of bits needed to encode the valid values.
   */
  public abstract int bits();

  /**
   * Get the ordinal of the first valid (ignore() is false) residue of this type.
   * All residues prior to this should not be valid and all after it should be valid.
   * @return the ordinal of the first valid residue.
   */
  public abstract int firstValid();

  @Override
  public String toString() {
    return mToString;
  }

  /**
   * Special handling of Serialization to ensure we get singletons on deserialization.
   * @return a singleton.
   */
  Object readResolve() {
    return VALUES[ordinal()];
  }
}

