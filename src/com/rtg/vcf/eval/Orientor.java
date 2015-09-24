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

import java.util.ArrayList;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Produces various orientations onto haplotypes of a variant
 */
@TestClass(value = {"com.rtg.vcf.eval.VariantFactoryTest", "com.rtg.vcf.eval.VariantTest"})
public interface Orientor {

  /**
   * Gets a list of variant orientations
   * @param variant the variant
   * @return the oriented variants
   */
  OrientedVariant[] orientations(AlleleIdVariant variant);

  /**
   * Produces orientations corresponding to the possible diploid phasings from the
   * GT-derived variant.
   * Path finding will require matching both alleles.
   */
  Orientor UNPHASED = new Orientor() {
    @Override
    public OrientedVariant[] orientations(AlleleIdVariant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      if (gv.alleleA() != gv.alleleB()) {
        // If the variant is heterozygous we need both phases
        return new OrientedVariant[]{
          new OrientedVariant(variant, true, gv.alleleA(), gv.alleleB()),
          new OrientedVariant(variant, false, gv.alleleB(), gv.alleleA())
        };
      } else {
        // Homozygous / haploid
        return new OrientedVariant[]{
          new OrientedVariant(variant, gv.alleleA())
        };
      }
    }
  };

  /**
   * Produces orientations corresponding to the possible diploid phasings from the
   * GT-derived variant, obeying global phasing if present.
   * Path finding will require matching both alleles.
   */
  Orientor PHASED = new Orientor() {
    @Override
    public OrientedVariant[] orientations(AlleleIdVariant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      if (gv.alleleA() != gv.alleleB()) {
        if (variant.isPhased()) {
          return new OrientedVariant[]{
            new OrientedVariant(variant, true, gv.alleleA(), gv.alleleB()),
          };
        } else {
          // If the variant is heterozygous we need both phases
          return new OrientedVariant[]{
            new OrientedVariant(variant, true, gv.alleleA(), gv.alleleB()),
            new OrientedVariant(variant, false, gv.alleleB(), gv.alleleA())
          };
        }
      } else {
        // Homozygous / haploid
        return new OrientedVariant[]{
          new OrientedVariant(variant, gv.alleleA())
        };
      }
    }
  };

  /**
   * Produces orientations corresponding to the possible haploid ALTs from the GT-derived variant.
   * Path finding will match any variants where there are any non-ref allele matches.
   */
  Orientor SQUASH_GT = new Orientor() {
    @Override
    public OrientedVariant[] orientations(AlleleIdVariant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      assert gv.alleleA() > 0 || gv.alleleB() > 0;
      final int la = gv.alleleA() > 0 ? gv.alleleA() : gv.alleleB();
      final int lb = gv.alleleB() > 0 ? gv.alleleB() : gv.alleleA();
      final OrientedVariant[] pos;
      if (la == lb) {
        pos = new OrientedVariant[1];
        pos[0] = new OrientedVariant(variant, la);
      } else {
        pos = new OrientedVariant[2];
        pos[0] = new OrientedVariant(variant, la);
        pos[1] = new OrientedVariant(variant, lb);
      }
      return pos;
    }
  };

  /**
   * Produces orientations corresponding to the possible haploid ALTs from the
   * population-allele-derived variant.
   */
  Orientor SQUASH_POP = new Orientor() {
    @Override
    public OrientedVariant[] orientations(AlleleIdVariant variant) {
      final OrientedVariant[] pos = new OrientedVariant[variant.numAlleles() - 1];
      for (int i = 0; i < pos.length; i++) {
        pos[i] = new OrientedVariant(variant, i + 1);
      }
      return pos;
    }
  };

  /**
   * Produces orientations corresponding to all the possible diploid genotypes allowed by the
   * population-allele-derived variant, with twists to help with sample recoding.
   */
  Orientor RECODE_POP = new Orientor() {
    @Override
    public OrientedVariant[] orientations(AlleleIdVariant variant) {
      final boolean explicitHalfCall = variant.allele(-1) != null;
      final ArrayList<OrientedVariant> pos = new ArrayList<>(variant.numAlleles() * variant.numAlleles() - 1);
      for (int i = 1; i < variant.numAlleles(); i++) {
        for (int j = -1; j < i; j++) {
          pos.add(new OrientedVariant(variant, true, i, j));
          pos.add(new OrientedVariant(variant, false, j, i));
          if (j == -1 && !explicitHalfCall) {
            j++; // Jump from . to first allele, so skips will match as missing
          }
        }
        pos.add(new OrientedVariant(variant, true, i, i));
      }
      return pos.toArray(new OrientedVariant[pos.size()]);
    }
  };
}
