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
package com.rtg.vcf.header;

import com.rtg.util.Utils;

/**
 * Encapsulate value of number field from <code>VCF</code> meta information lines
 */
public class VcfNumber {
  private final int mNumber;
  private final VcfNumberType mType;

  /** Singleton for variable numbered variant types */
  public static final VcfNumber DOT = new VcfNumber(".");
  /** Singleton for alt allele valued variant types */
  public static final VcfNumber ALTS = new VcfNumber("A");
  /** Singleton for all allele valued variant types */
  public static final VcfNumber REF_ALTS = new VcfNumber("R");
  /** Singleton for genotype valued variant types */
  public static final VcfNumber GENOTYPES = new VcfNumber("G");

  /**
   * @param number number field from a meta line
   */
  public VcfNumber(String number) {
    switch (number) {
      case "A":
        mType = VcfNumberType.ALTS;
        mNumber = -1;
        break;
      case "R":
        mType = VcfNumberType.REF_ALTS;
        mNumber = -1;
        break;
      case "G":
        mType = VcfNumberType.GENOTYPES;
        mNumber = -1;
        break;
      case ".":
        mType = VcfNumberType.UNKNOWN;
        mNumber = -1;
        break;
      default:
        mType = VcfNumberType.INTEGER;
        mNumber = Integer.parseInt(number);
        break;
    }
  }
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof VcfNumber)) {
      return false;
    }
    final VcfNumber other = (VcfNumber) obj;
    switch (mType) {
      case ALTS:
      case GENOTYPES:
      case REF_ALTS:
      case UNKNOWN:
        return mType == other.mType;
      case INTEGER:
        return other.mType == VcfNumberType.INTEGER && mNumber == other.mNumber;
      default:
        throw new IllegalArgumentException("Type: " + mType + " is not supported");
    }
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mType.ordinal(), mNumber);
  }

  /**
   * @return the numbers type
   */
  public VcfNumberType getNumberType() {
    return mType;
  }

  /**
   * @return if type is {@link VcfNumberType#INTEGER} then its value, otherwise -1
   */
  public int getNumber() {
    return mNumber;
  }

  @Override
  public String toString() {
    return mType.isFixedString() ? mType.toString() : Integer.toString(mNumber);
  }

}
