/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.alignment;

import java.util.Arrays;

import com.rtg.util.Pair;

/**
 * A slice within a partition.
 */
public class Slice extends Pair<Integer, String[]> {

  /**
   * A slice within a partition.
   * @param offset offset within the partition
   * @param alleles alleles of the slice starting with the reference
   */
  Slice(final int offset, final String... alleles) {
    super(offset, alleles);
  }

  /**
   * Get the offset of this slice within the partition.
   * @return the offset
   */
  public int getOffset() {
    return getA();
  }

  /**
   * Get the alleles of this slice, with the reference allele first.
   * @return alleles
   */
  public String[] getAlleles() {
    return getB();
  }

  private boolean isUnpeelable() {
    int min = Integer.MAX_VALUE;
    int max = -1;
    for (final String a : getAlleles()) {
      min = Math.min(min, a.length());
      max = Math.max(max, a.length());
    }
    return min <= 0 || min >= max;
  }

  /**
   * Return a score for peeling this slice on the left.  Will be -1 if no
   * peeling makes sense, otherwise the count of the alleles matching
   * reference.
   * @return left-peeling score or -1
   */
  int peelLeftScore() {
    if (isUnpeelable()) {
      return -1;
    }
    final String[] alleles = getAlleles();
    final char c = alleles[0].charAt(0);
    int cnt = 0;
    for (int k = 1; k < alleles.length; ++k) {
      if (alleles[k].charAt(0) == c) {
        ++cnt;
      }
    }
    return cnt;
  }

  /**
   * Return a score for peeling this slice on the right.  Will be -1 if no
   * peeling makes sense, otherwise the count of the alleles matching
   * reference.
   * @return right-peeling score or -1
   */
  int peelRightScore() {
    if (isUnpeelable()) {
      return -1;
    }
    final String[] alleles = getAlleles();
    final char c = alleles[0].charAt(0);
    int cnt = 0;
    for (int k = 1; k < alleles.length; ++k) {
      if (alleles[k].charAt(alleles[k].length() - 1) == c) {
        ++cnt;
      }
    }
    return cnt;
  }

  @Override
  public String toString() {
    return getOffset() + " " + Arrays.toString(getAlleles());
  }
}
