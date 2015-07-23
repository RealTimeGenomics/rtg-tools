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
 * A nucleotide sequence that can be in forward or reverse complement.
 */
public abstract class BidirectionalFrame implements Frame, PseudoEnum {
  /**
   * Normal forward direction.
   */
  public static final BidirectionalFrame FORWARD = new BidirectionalFrame(0, "FORWARD", "F") {
    @Override
    public byte code(final byte[] codes, final int length, final int index, int offset, int fullLength) {
      if (index >= length) {
        throw new RuntimeException("length=" + length + " index=" + index);
      }
      return codes[index];
    }
    @Override
    public boolean isForward() {
      return true;
    }

    @Override
    public Frame getReverse() {
      return REVERSE;
    }


  };

  /**
   * Reverse complement.
   */
  public static final BidirectionalFrame REVERSE = new BidirectionalFrame(1, "REVERSE", "R") {
    @Override
    public byte code(final byte[] codes, final int length, final int index, int offset, int fullLength) {
      if (index < 0) {
        throw new RuntimeException("length=" + length + " index=" + index);
      }
      final byte x = codes[length - index - 1];
      return complement(x);
    }
    @Override
    public boolean isForward() {
      return false;
    }

    @Override
    public Frame getReverse() {
      return FORWARD;
    }
  };

  /**
   * Compute the complement of a nucleotide expressed as an underlying code.
   * @param b the code value to be complemented.
   * @return the complement.
   */
  static byte complement(final byte b) {
    if (b == 0) {
      return 0; //unknown
    }
    return (byte) (5 - b);
  }

  /**
   * Get the frame corresponding to the integer value.
   * @param value int value
   * @return the frame
   */
  static BidirectionalFrame frameFromOrdinal(final int value) {
    return values()[value];
  }

  private final String mDisplay;
  private final String mName;
  private final int mOrdinal;

  private BidirectionalFrame(final int ordinal, final String name, final String display) {
    mDisplay = display;
    mName = name;
    mOrdinal = ordinal;
  }

  @Override
  public byte code(byte[] codes, int length, int index) {
    return code(codes, length, index, 0, length);
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

  private static final EnumHelper<BidirectionalFrame> HELPER = new EnumHelper<>(BidirectionalFrame.class, new BidirectionalFrame[] {FORWARD, REVERSE});

  /**
   * @return list of the enum values
   */
  public static BidirectionalFrame[] values() {
    return HELPER.values();
  }

  /**
   * see {@link java.lang.Enum#valueOf(Class, String)}
   * @param str the name of the enum
   * @return the enum value
   */
  public static BidirectionalFrame valueOf(final String str) {
    return HELPER.valueOf(str);
  }

  @Override
  public String display() {
    return mDisplay;
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

