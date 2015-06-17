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

package com.rtg.reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.rtg.reader.SequencesReader;
import com.rtg.reference.ReferenceGenome.DefaultFallback;
import com.rtg.util.Pair;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.intervals.SequenceNameLocus;

/**
 * Provides fast lookup to reference genome information for each of the possible various sex values.
 *
 */
public class SexMemo {

  private final ReferenceGenome[] mReferences;

  // Per sex, per reference sequence containing PAR regions, a list of those regions with their effective ploidy
  private final MyFrickenMap[] mParMap;

  /**
   * Contain a list of PAR regions along with their effective ploidy.
   */
  public static class EffectivePloidyList extends ArrayList<Pair<RegionRestriction, Ploidy>> { }

  static class MyFrickenMap extends HashMap<String, EffectivePloidyList> { }


  /**
   * Remember information from command line and reference file to enable ploidy of a sequence to be determined given the
   * sex of a sample.
   * @param reader sequences reader
   * @param ploidy default ploidy
   * @throws IOException whenever.
   */
  public SexMemo(final SequencesReader reader, final DefaultFallback ploidy) throws IOException {
    final Sex[] sexValues = Sex.values();
    mReferences = new ReferenceGenome[sexValues.length];
    mParMap = new MyFrickenMap[sexValues.length];
    for (final Sex sex : sexValues) {
      final ReferenceGenome referenceGenome = reader == null ? null : new ReferenceGenome(reader, sex, ploidy);
      mReferences[sex.ordinal()] = referenceGenome;
      final MyFrickenMap parmap = new MyFrickenMap();

      if (referenceGenome != null) {
        for (ReferenceSequence rs : referenceGenome.sequences()) {
          if (rs.hasDuplicates()) {
            final String refName = rs.name();
            final EffectivePloidyList result = new EffectivePloidyList();
            for (Pair<RegionRestriction, RegionRestriction> dup : rs.duplicates()) {
              final RegionRestriction r = dup.getA();
              final RegionRestriction s = dup.getB();
              if (refName.equals(r.getSequenceName())) {
                result.add(new Pair<>(r, Ploidy.DIPLOID));
              }
              if (refName.equals(s.getSequenceName())) {
                result.add(new Pair<>(s, Ploidy.NONE));
              }
            }
            parmap.put(refName, result);
          }
        }
      }
      mParMap[sex.ordinal()] = parmap;
    }
  }

  /**
   * Get the reference genome information for the supplied sex
   * @param sex the sex of interest
   * @return the reference genome information
   */
  public ReferenceGenome referenceGenome(Sex sex) {
    return mReferences[sex.ordinal()];
  }


  /**
   * Ploidy to be used during calling for this sex/reference/position combination.
   *
   * @param sex of the sample.
   * @param refName name of the sequence
   * @param pos the position we care about (important for PAR handling)
   * @return the ploidy of this sequence for this sex
   */
  public Ploidy getEffectivePloidy(final Sex sex, String refName, int pos) {
    final ReferenceGenome referenceGenome = mReferences[sex.ordinal()];
    if (referenceGenome == null) {
      return Ploidy.DIPLOID; // default for diploid when no referecne file
    }
    final ReferenceSequence rs =  referenceGenome.sequence(refName);
    if (rs == null) {
      return Ploidy.NONE;
    }

    // Check PAR regions, within a PAR, the first sequence gets them both
    final EffectivePloidyList parList = getParEffectivePloidy(sex, refName);
    if (parList != null) {
      for (Pair<RegionRestriction, Ploidy> pair : parList) {
        if (pair.getA().contains(refName, pos)) {
          return pair.getB();
        }
      }
    }

    // For our callers we are treating Polyploid as haploid
    if (rs.ploidy() == Ploidy.POLYPLOID) {
      return Ploidy.HAPLOID;
    }
    return rs.ploidy();
  }

    /**
      * Ploidy to be used during calling for this sex/reference combination, ignoring PAR regions.
      * @param sex of the sample.
      * @param refName name of the sequence.
      * @return the ploidy of this sequence for this sex
      */
  public Ploidy getEffectivePloidy(final Sex sex, final String refName) {
    final Ploidy real = getRealPloidy(sex, refName);
    // For our callers we are treating Polyploid as haploid
    if (real == Ploidy.POLYPLOID) {
      return Ploidy.HAPLOID;
    }
    return real;
  }

  /**
   * Actual ploidy of the reference for the specified sex at the specified position.
   * This method is aware of PAR regions
   * @param sex of the sample.
   * @param refName name of the sequence.
   * @param pos the position we care about (important for PAR handling)
   * @return the ploidy of this sequence for this sex at this position
   */
  public Ploidy getRealPloidy(final Sex sex, final String refName, int pos) {
    final ReferenceGenome referenceGenome = mReferences[sex.ordinal()];
    if (referenceGenome == null) {
      return Ploidy.DIPLOID; // default for diploid when no referecne file
    }
    final ReferenceSequence rs =  referenceGenome.sequence(refName);
    if (rs == null) {
      return Ploidy.NONE;
    }

    // Check PAR regions
    if (rs.hasDuplicates()) {
      for (Pair<RegionRestriction, RegionRestriction> dup : rs.duplicates()) {
        if (dup.getA().contains(refName, pos)
            || dup.getB().contains(refName, pos)) {
          return Ploidy.DIPLOID;
        }
      }
    }
    return rs.ploidy();
  }

  /**
   * Actual ploidy of the reference chromosome for the specified sex. This method does not
   * consider PAR regions.
   * @param sex of the sample.
   * @param refName name of the sequence.
   * @return the ploidy of this sequence for this sex
   */
  public Ploidy getRealPloidy(final Sex sex, final String refName) {
    final ReferenceGenome referenceGenome = mReferences[sex.ordinal()];
    if (referenceGenome == null) {
      return Ploidy.DIPLOID; // default for diploid when no referecne file
    }
    final ReferenceSequence rs =  referenceGenome.sequence(refName);
    if (rs == null) {
      return Ploidy.NONE;
    }
    return rs.ploidy();
  }

  /**
   * Gets the set of PAR region effective ploidy for the current sex / reference. This
   * is the effective ploidy, as one side of the duplication will be assigned to be diploid and the other none.
   * @param sex the sex of the individual
   * @param refName the name of the sequence
   * @return a list of the regions or null if no PAR regions are relevant
   */
  public EffectivePloidyList getParEffectivePloidy(final Sex sex, String refName) {
    return mParMap[sex.ordinal()].get(refName);
  }

  /**
   * Get the point at which the genome transitions into / out of the PAR region
   * @param sex the sex of the individual
   * @param region the region in which to search for a PAR boundary.
   * @return the position of the transition, or -1 if no PAR region is within the specified region
   */
  public int getParBoundary(final Sex sex, SequenceNameLocus region) {
    final EffectivePloidyList parList = getParEffectivePloidy(sex, region.getSequenceName());
    if (parList != null) {
      for (Pair<RegionRestriction, Ploidy> parSite : parList) {
        final RegionRestriction r = parSite.getA();
        if (r.getLength() < region.getLength()) {
          throw new RuntimeException("PAR regions smaller than chunk size (" + region.getLength() + ") are not supported: " + r.toString());
        }
        if (region.contains(r.getSequenceName(), r.getStart())) {
          return r.getStart();
        } else if (region.contains(r.getSequenceName(), r.getEnd())) {
          return r.getEnd();
        }
      }
    }
    return -1;
  }
}
