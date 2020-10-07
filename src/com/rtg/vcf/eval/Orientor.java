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
import java.util.Arrays;

import com.rtg.util.Permutation;

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
   * @return the number of haplotypes that oriented variants will contain
   */
  int haplotypes();

  /**
   * Produces orientations corresponding to the possible phasings from the
   * GT-derived variant.
   * Path finding will require matching all called alleles.
   */
  final class UnphasedOrientor implements Orientor {
    final int mHaplotypes;
    UnphasedOrientor(int haplotypes) {
      mHaplotypes = haplotypes;
    }
    @Override
    public String toString() {
      return "unphased (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return mHaplotypes;
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      assert gv.ploidy() == mHaplotypes;
      if (mHaplotypes == 2) { // Use old method for diploid, for now
        if (gv.alleleA() != gv.alleleB()) {
          // If the variant is heterozygous we need both phases
          return new OrientedVariant[]{
            new OrientedVariant(variant, true, gv.alleleA(), gv.alleleB()),
            new OrientedVariant(variant, false, gv.alleleB(), gv.alleleA())
          };
        } else {
          // Homozygous / haploid
          return new OrientedVariant[]{
            new OrientedVariant(variant, true, gv.alleleA(), gv.alleleA())
          };
        }
      } else {
        final Permutation p = new Permutation(gv.alleleIds());
        final ArrayList<OrientedVariant> o = new ArrayList<>();
        int[] r;
        while ((r = p.next()) != null) {
          o.add(new OrientedVariant(variant, false, r.clone()));
        }
        return o.toArray(new OrientedVariant[0]);
      }
    }
  }

  /**
   * Produces orientations corresponding to the possible phasings from the
   * GT-derived variant, obeying global phasing if present. Global phasing can be
   * taken as-is, or inverted. Path finding will require matching all the called alleles.
   */
  final class PhasedOrientor implements Orientor {
    final boolean mInvert;
    final int mHaplotypes;
    final UnphasedOrientor mUnphased;

    /**
     * Constructor
     * @param invert if true, invert the provided phasing
     * @param haplotypes number of haplotypes
     */
    PhasedOrientor(boolean invert, int haplotypes) {
      mInvert = invert;
      mHaplotypes = haplotypes;
      mUnphased = new UnphasedOrientor(mHaplotypes);
    }
    @Override
    public String toString() {
      return "phase-" + (mInvert ? "inverting" : "obeying") + " (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return mHaplotypes;
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      if (variant.isPhased()) {
        if (mInvert) {
          int i = gv.alleleIds().length;
          final int[] reversed = new int[i];
          for (int a : gv.alleleIds()) {
            reversed[--i] = a;
          }
          return new OrientedVariant[]{new OrientedVariant(variant, false, reversed)};
        } else {
          return new OrientedVariant[]{new OrientedVariant(variant, true, gv.alleleIds())};
        }
      } else {
        return mUnphased.orientations(variant);
      }
    }
  }

  /**
   * Produces orientations corresponding to the possible haploid genotypes that employ any
   * of the ALTs from the GT-derived variant.
   * Path finding will match any variants where there are any non-ref allele matches (as
   * long as they can be played into a single haplotype).
   */
  Orientor SQUASH_GT = new Orientor() {
    @Override
    public String toString() {
      return "squash (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return 1;
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;

      // Get the unique replayable ALTs
      final int[] altIds = gv.alleleIds().clone();
      Arrays.sort(altIds);
      int numAlts = 0;
      int prevId = 0;
      for (final int id : altIds) {
        if ((id > prevId) && variant.allele(id) != null) {
          altIds[numAlts++] = id;
          prevId = id;
        }
      }
      assert numAlts > 0;

      final OrientedVariant[] pos = new OrientedVariant[numAlts];
      for (int i = 0; i < numAlts; i++) {
        pos[i] = new OrientedVariant(variant, altIds[i]);
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
      return "allele-gt (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return 2;
    }
    @Override
    public OrientedVariant[] orientations(Variant variant) {
      final GtIdVariant gv = (GtIdVariant) variant;
      assert gv.alleleIds().length == 2;
      final boolean aVar = gv.alleleA() > 0 && variant.allele(gv.alleleA()) != null;
      final boolean bVar = gv.alleleB() > 0 && variant.allele(gv.alleleB()) != null;
      assert aVar || bVar; // Must be at least one replayable variant allele
      final int la = aVar ? gv.alleleA() : gv.alleleB();
      final int lb = bVar ? gv.alleleB() : gv.alleleA();
      if (la == lb) { // { 0/1, 1/0, 1/1 }   ->   0/1 + 1/0 + 1/1
        return new OrientedVariant[]{
          new OrientedVariant(variant, false, 0, la),
          new OrientedVariant(variant, false, la, 0),
          new OrientedVariant(variant, false, la, la),
        };
      } else { // { 1/2, 2/1 }  ->   0/1 + 0/2 + 1/0 + 2/0 + 1/2 + 2/1
        return new OrientedVariant[]{
          new OrientedVariant(variant, false, 0, la),
          new OrientedVariant(variant, false, 0, lb),
          new OrientedVariant(variant, false, la, 0),
          new OrientedVariant(variant, false, lb, 0),
          new OrientedVariant(variant, false, la, lb),
          new OrientedVariant(variant, false, lb, la),
          // For completeness we should add: (but they may actually be superfluous in the context of other variants)
          // new OrientedVariant(variant, la),
          // new OrientedVariant(variant, lb),
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
      return "squash-all (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return 1;
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
      return "diploid-all (" + haplotypes() + " haplotypes)";
    }
    @Override
    public int haplotypes() {
      return 2;
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
      return pos.toArray(new OrientedVariant[0]);
    }
  };
}
