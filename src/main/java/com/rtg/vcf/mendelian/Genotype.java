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

  enum TriState {
    TRUE,
    FALSE,
    MAYBE
  }

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

  public TriState contains(int allele) {
    if (allele == -1) {
      return TriState.MAYBE;
    }
    TriState result = TriState.FALSE;
    for (int a : mAlleles) {
      if (a == allele) {
        return TriState.TRUE;
      } else if (a == -1) {
        result = TriState.MAYBE;
      }
    }
    return result;
  }

  public boolean homozygous() {
    return VcfUtils.isHomozygous(mAlleles);
  }

  public boolean multiallelic() {
    for (int a : mAlleles) {
      if (a > 1) {
        return true;
      }
    }
    return false;
  }

  public boolean incomplete() {
    for (int a : mAlleles) {
      if (a == -1) {
        return true;
      }
    }
    return false;
  }

  public String toString() {
    return VcfUtils.joinGt(false, mAlleles);
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

  static class GenotypeComparator implements Comparator<Genotype>, Serializable {
    @Override
    public int compare(Genotype a, Genotype b) {
      if (a.mAlleles.length < b.mAlleles.length) {
        return -1;
      } else if (a.mAlleles.length > b.mAlleles.length) {
        return 1;
      }
      for (int i = 0; i < a.mAlleles.length && i < b.mAlleles.length; ++i) {
        if (a.mAlleles[i] < b.mAlleles[i]) {
          return -1;
        } else if (a.mAlleles[i] > b.mAlleles[i]) {
          return 1;
        }
      }
      return 0;
    }
  }

}
