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

/**
 * Enum for type of number in <code>VCF</code> meta lines
 */
public enum VcfNumberType {

  /** if specified number of values applies to field */
  INTEGER(false, ""),
  /** if one value per alternate allele */
  ALTS(true, "A"),
  /** if one value per all possible alleles (ref and alts) */
  REF_ALTS(true, "R"),
  /** if one value per genotype */
  GENOTYPES(true, "G"),
  /** if number of values varies or is unknown */
  UNKNOWN(true, ".");

  private final boolean mFixed;
  private final String mToString;

  private VcfNumberType(boolean fixedString, String str) {
    mFixed = fixedString;
    mToString = str;
  }

  /**
   * @return true if {@link VcfNumberType#toString()} will return numbers output value, false if should use {@link VcfNumber#toString()} instead
   */
  public boolean isFixedString() {
    return mFixed;
  }

  @Override
  public String toString() {
    return mFixed ? mToString : super.toString();
  }

}
