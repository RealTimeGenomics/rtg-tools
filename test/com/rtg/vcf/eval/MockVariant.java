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

package com.rtg.vcf.eval;

/**
 */
public class MockVariant extends Variant {

  static byte[][] toPreds(byte[] plus, byte[] minus) {
    if (minus == null) {
      return new byte[][] {null, plus};
    } else {
      return new byte[][] {null, plus, minus};
    }
  }

  static Allele[] toAlleles(String seq, int start, int end, byte[][] alleles) {
    final Allele[] result = new Allele[alleles.length];
    for (int i = 0; i < result.length; ++i) {
      result[i] = alleles[i] == null ? null : new Allele(seq, start, end, alleles[i]);
    }
    return result;
  }


  /**
   * Assumes not phased
   * @param start one-based start position of mutation
   * @param end one-based end position of mutation
   * @param plus nucleotides on the plus strand
   * @param minus nucleotides on the minus strand
   */
  public MockVariant(int start, int end, byte[] plus, byte[] minus) {
    this(start, end, plus, minus, 0);
  }

  /**
   * Assumes not phased
   * @param start one-based start position of mutation
   * @param end one-based end position of mutation
   * @param plus nucleotides on the plus strand
   * @param minus nucleotides on the minus strand
   * @param id the variant id
   */
  public MockVariant(int start, int end, byte[] plus, byte[] minus, int id) {
    this(start, end, toPreds(plus, minus), id);
  }

  /**
   * Assumes not phased
   * @param start one-based start position of mutation
   * @param end one-based end position of mutation
   * @param alleles the alleles
   * @param id the variant id
   */
  public MockVariant(int start, int end, byte[][] alleles, int id) {
    super(id, "", start - 1, end - 1, toAlleles("", start - 1, end - 1, alleles), false);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getStart() + 1).append(":").append(getEnd() + 1).append(" ");
    for (int i = 0; i < numAlleles(); ++i) {
      if (i > 0) {
        sb.append(":");
      }
      sb.append(alleleStr(i));
    }
    return sb.toString();
  }
}
