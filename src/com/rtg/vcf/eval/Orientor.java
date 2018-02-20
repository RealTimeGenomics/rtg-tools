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

/**
 * Produces various orientations onto haplotypes of a variant
 */
public interface Orientor {

  /**
   * Gets a list of variant orientations
   * @param variant the variant
   * @return the oriented variants
   */
  OrientedVariant[] orientations(Variant variant);

  /**
   * Produces orientations corresponding to the possible diploid phasings from the
   * GT-derived variant.
   * Path finding will require matching both alleles.
   */
  Orientor UNPHASED = new Orientor() {
    @Override
    public String toString() {
      return "unphased";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
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
   * Orientor that obeys global phasing if present in the GT-derived variant.
   */
  final class PhasedOrientor implements Orientor {
    final boolean mInvert;
    PhasedOrientor(boolean invert) {
      mInvert = invert;
    }
    @Override
    public String toString() {
      return "phase-" + (mInvert ? "inverting" : "obeying");
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      if (gv.alleleA() != gv.alleleB()) {
        if (variant.isPhased()) {
          return new OrientedVariant[]{
            mInvert
              ? new OrientedVariant(variant, false, gv.alleleB(), gv.alleleA())
              : new OrientedVariant(variant, true, gv.alleleA(), gv.alleleB()),
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
  }

  /**
   * Produces orientations corresponding to the possible diploid phasings from the
   * GT-derived variant, obeying global phasing if present.
   * Path finding will require matching both alleles.
   */
  Orientor PHASED = new PhasedOrientor(false);

  /**
   * Produces orientations corresponding to the possible diploid phasings from the
   * GT-derived variant, obeying but inverting global phasing if present.
   * Path finding will require matching both alleles.
   */
  Orientor PHASE_INVERTED = new PhasedOrientor(true);

  /**
   * Produces orientations corresponding to the possible haploid genotypes that employ any
   * of the ALTs from the GT-derived variant.
   * Path finding will match any variants where there are any non-ref allele matches (as
   * long as they can be played into a single haplotype).
   */
  Orientor SQUASH_GT = new Orientor() {
    @Override
    public String toString() {
      return "squash";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      final boolean aVar = gv.alleleA() > 0 && variant.allele(gv.alleleA()) != null;
      final boolean bVar = gv.alleleB() > 0 && variant.allele(gv.alleleB()) != null;
      assert aVar || bVar; // Must be at least one replayable variant allele
      final int la = aVar ? gv.alleleA() : gv.alleleB();
      final int lb = bVar ? gv.alleleB() : gv.alleleA();
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
   * Produces orientations corresponding to the possible diploid genotypes that employ any
   * of the ALTs used by the GT-derived variant.
   * Path finding will match any variants where there are any non-ref allele matches (as
   * long as they can be played into two haplotypes)
   */
  Orientor ALLELE_GT = new Orientor() {
    @Override
    public String toString() {
      return "allele-gt";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      final boolean aVar = gv.alleleA() > 0 && variant.allele(gv.alleleA()) != null;
      final boolean bVar = gv.alleleB() > 0 && variant.allele(gv.alleleB()) != null;
      assert aVar || bVar; // Must be at least one replayable variant allele
      final int la = aVar ? gv.alleleA() : gv.alleleB();
      final int lb = bVar ? gv.alleleB() : gv.alleleA();
      if (la == lb) { // { 0/1, 1/0, 1/1 }   ->   0/1 + 1/0 + 1/1
        return new OrientedVariant[]{
          new OrientedVariant(variant, true, 0, la),
          new OrientedVariant(variant, true, la, 0),
          new OrientedVariant(variant, la),
        };
      } else { // { 1/2, 2/1 }  ->   0/1 + 0/2 + 1/0 + 2/0 + 1/2 + 2/1
        return new OrientedVariant[]{
          new OrientedVariant(variant, true, 0, la),
          new OrientedVariant(variant, true, 0, lb),
          new OrientedVariant(variant, true, la, 0),
          new OrientedVariant(variant, true, lb, 0),
          new OrientedVariant(variant, true, la, lb),
          new OrientedVariant(variant, true, lb, la),
        };
      }
    }
  };

  /**
   * Produces orientations corresponding to the possible haploid ALTs from the
   * population-alleles-derived variant.
   */
  Orientor HAPLOID_POP = new Orientor() {
    @Override
    public String toString() {
      return "squash-all";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final OrientedVariant[] pos = new OrientedVariant[variant.numAlleles() - 1];
      for (int i = 0; i < pos.length; ++i) {
        pos[i] = new OrientedVariant(variant, i + 1);
      }
      return pos;
    }
  };

  /**
   * Produces orientations corresponding to all the possible diploid genotypes allowed by the
   * population-alleles-derived variant.
   * With twists to help with sample recoding.
   */
  Orientor DIPLOID_POP = new Orientor() {
    @Override
    public String toString() {
      return "diploid-all";
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final boolean explicitHalfCall = variant.allele(-1) != null;
      final ArrayList<OrientedVariant> pos = new ArrayList<>(variant.numAlleles() * variant.numAlleles() - 1);
      for (int i = 1; i < variant.numAlleles(); ++i) {
        for (int j = -1; j < i; ++j) {
          pos.add(new OrientedVariant(variant, true, i, j));
          pos.add(new OrientedVariant(variant, false, j, i));
          if (j == -1 && !explicitHalfCall) {
            ++j; // Jump from . to first allele, so skips will match as missing
          }
        }
        pos.add(new OrientedVariant(variant, true, i, i));
      }
      return pos.toArray(new OrientedVariant[pos.size()]);
    }
  };
}
