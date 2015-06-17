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

import com.rtg.util.EnumHelper;
import com.rtg.util.PseudoEnum;

/**
 * Protein can only have a single untranslated frame (no translation possible
 * and it makes no sense to take a reverse complement).
 */
public final class ProteinFrame implements Frame, PseudoEnum {
  /** Protein. */
  public static final ProteinFrame PROTEIN = new ProteinFrame(0, "PROTEIN");

  private final int mOrdinal;
  private final String  mName;

  private ProteinFrame(final int ordinal, final String name) {
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

  private static final EnumHelper<ProteinFrame> HELPER = new EnumHelper<>(ProteinFrame.class, new ProteinFrame[] {PROTEIN});

  /**
   * @return the list of enum values
   */
  public static ProteinFrame[] values() {
    return HELPER.values();
  }

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str name of enum
   * @return the enum value
   */
  public static ProteinFrame valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  @Override
  public String display() {
    return "";
  }

  @Override
  public boolean isForward() {
    return true;
  }

  @Override
  public Frame getReverse() {
    throw new UnsupportedOperationException("Not supported");
  }

  /**
   * Get the frame corresponding to the integer value.
   * @param value int value
   * @return the frame
   */
  static ProteinFrame frameFromCode(final int value) {
    if (value != 0) {
      throw new IllegalArgumentException(String.valueOf(value));
    }
    return PROTEIN;
  }

  private static final byte UNKNOWN_RESIDUE = (byte) Protein.X.ordinal();

  @Override
  public byte code(final byte[] codes, final int length, final int index, int offset, int fullLength) {
    return index >= length ? UNKNOWN_RESIDUE : codes[index];
  }

  @Override
  public byte code(byte[] codes, int length, int index) {
    return code(codes, length, index, 0, length);
  }
  @Override
  public int phase() {
    return 0;
  }

  @Override
  public int calculateFirstValid(int offset, int length, int fullLength) {
    return 0;
  }

  @Override
  public int calculateLastValid(int offset, int length, int fullLength) {
    return length;
  }

}

