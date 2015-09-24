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

import com.reeltwo.jumble.annotations.TestClass;

/**
 * A Variant that offers orientations assuming the alleles array has all redundant alleles removed.
 */
@TestClass("com.rtg.vcf.eval.VariantTest")
public class CompactVariant extends Variant {


  CompactVariant(int id, String seq, int start, int end, Allele[] alleles, boolean phased) {
    super(id, seq, start, end, alleles, phased);
  }

  /**
   * @return the possible oriented variants for this variant
   */
  public OrientedVariant[] orientations() {
    if (numAlleles() == 2) {
      // If the variant is heterozygous we need both phases
      return new OrientedVariant[]{
        new OrientedVariant(this, true, 0, 1),
        new OrientedVariant(this, false, 1, 0)
      };
    } else {
      assert numAlleles() == 1;
      // Homozygous / haploid
      return new OrientedVariant[] {
        new OrientedVariant(this, 0)
      };
    }
  }
}
