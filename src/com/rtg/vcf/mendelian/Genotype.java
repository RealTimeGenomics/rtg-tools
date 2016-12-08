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
package com.rtg.vcf.mendelian;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import com.rtg.vcf.VcfUtils;

/**
 * Represents a called genotype, with numeric allele coding according to VCF (missing is encoded as -1). Alleles are in sorted order.
 */
class Genotype {

  static final GenotypeComparator GENOTYPE_COMPARATOR = new GenotypeComparator();

  private final int[] mAlleles;

  Genotype(String gt) {
    this(VcfUtils.splitGt(gt));
  }

  Genotype(int[] gt) {
    mAlleles = gt;
    Arrays.sort(mAlleles);
  }

  public int length() {
    return mAlleles.length;
  }

  public int get(int index) {
    return mAlleles[index];
  }

  public boolean contains(int allele) {
    for (int a : mAlleles) {
      if (a == allele) {
        return true;
      }
    }
    return false;
  }

  public boolean homozygous() {
    return mAlleles.length > 0 && mAlleles[0] == mAlleles[mAlleles.length - 1];
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (int j : mAlleles) {
      if (first) {
        sb.append(Integer.toString(j));
        first = false;
      } else {
        sb.append("/").append(Integer.toString(j));
      }
    }
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (o == null || !(o instanceof Genotype)) {
      return false;
    }
    return GENOTYPE_COMPARATOR.compare(this, (Genotype) o) == 0;
  }

  public int hashCode() {
    int res = 0;
    for (int h : mAlleles) {
      res += h;
    }
    return res;
  }

  // If this modified to be non-thread safe, then make sure to remove the static instance
  static class GenotypeComparator implements Comparator<Genotype>, Serializable {
    @Override
    public int compare(Genotype a, Genotype b) {
      for (int i = 0; i < a.mAlleles.length && i < b.mAlleles.length; ++i) {
        if (a.mAlleles[i] < b.mAlleles[i]) {
          return -1;
        } else if (a.mAlleles[i] > b.mAlleles[i]) {
          return 1;
        }
      }
      if (a.mAlleles.length < b.mAlleles.length) {
        return -1;
      } else if (a.mAlleles.length > b.mAlleles.length) {
        return 1;
      }
      return 0;
    }
  }
}
