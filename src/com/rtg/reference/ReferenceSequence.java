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


import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import com.rtg.util.Pair;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.StringUtils;

/**
 * Information about a single sequence in a reference genome.
 */
public class ReferenceSequence {

  private final Queue<Pair<RegionRestriction, RegionRestriction>> mDuplicates = new LinkedList<>();

  private final boolean mLinear;
  private final boolean mSpecified;

  private final Ploidy mPloidy;

  private final String mName;

  private final String mHaploidComplementName;

  private final long mSequenceLength;

  /**
   * @param specified true iff this is an explicitly specified sequence
   * @param linear true iff the sequence is linear (as opposed to circular).
   * @param ploidy the number of copies expected of this sequence.
   * @param name of sequence.
   * @param hapName name of mate sequence if part of a haploid complement, otherwise null.
   * @param length of sequence in nucleotides.
   */
  ReferenceSequence(final boolean specified, final boolean linear, final Ploidy ploidy, final String name, String hapName, final int length) {
    if (name == null) {
      throw new NullPointerException();
    }
    if (ploidy == null) {
      throw new NullPointerException();
    }
    if (name.matches("\\s+")) {
      throw new IllegalArgumentException("Name cannot contain whitespace");
    }
    if ((hapName != null) && (ploidy != Ploidy.HAPLOID)) {
      throw new IllegalArgumentException("Ploidy must be hapoid when using complement sequences");
    }
    mSpecified = specified;
    mLinear = linear;
    mPloidy = ploidy;
    mName = name;
    mHaploidComplementName = hapName;
    mSequenceLength = length;
  }

  /**
   * @return true iff the sequence is linear.
   */
  public boolean isLinear() {
    return mLinear;
  }

  /**
   * @return true iff the sequence is explicitly specified in the reference file (i.e. not default).
   */
  public boolean isSpecified() {
    return mSpecified;
  }

  /**
   * @return the ploidy for the sequence.
   */
  public Ploidy ploidy() {
    return mPloidy;
  }

  /**
   * @param pos the position in the sequence
   * @return the effective ploidy at the given position considering PAR regions
   */
  public Ploidy effectivePloidy(int pos) {
    for (Pair<RegionRestriction, RegionRestriction> region : mDuplicates) {
      if (region.getA().contains(mName, pos)) {
        return Ploidy.DIPLOID;
      } else if (region.getB().contains(mName, pos)) {
        return Ploidy.NONE;
      }
    }
    return mPloidy;
  }

  /**
   * @return the name of the sequence.
   */
  public String name() {
    return mName;
  }

  /**
   * @return the name of the haploid complement to this sequence, if any.
   */
  public String haploidComplementName() {
    return mHaploidComplementName;
  }

  /**
   * @return true if this sequence involves duplicate regions.
   */
  public boolean hasDuplicates() {
    return !mDuplicates.isEmpty();
  }

  /**
   * Get pairs that describe pseudo-autosomal regions.
   * These are thought of as sequences that are duplicates of each other.
   * @return all the duplicate region pairs.
   */
  public Collection<Pair<RegionRestriction, RegionRestriction>> duplicates() {
    return mDuplicates;
  }

  /**
   * Add a duplicate to list of duplicates.
   * @param duplicate to be added.
   * @throws IllegalArgumentException if the duplicate is not consistent with the current reference sequence.
   */
  void addDuplicate(final Pair<RegionRestriction, RegionRestriction> duplicate) {
    if (mPloidy != Ploidy.HAPLOID) {
      throw new IllegalArgumentException("Duplicate specified for sequence that isn't haploid.");
    }
    if (!duplicate.getA().getSequenceName().equals(mName) && !duplicate.getB().getSequenceName().equals(mName)) {
      throw new IllegalArgumentException("Duplicate specified for incorrect sequence.");
    }

    //make sure none of them overlap (O(N^2) loop - should be small).
    final RegionRestriction checki =  duplicate.getA().getSequenceName().equals(mName) ? duplicate.getA() : duplicate.getB();
    final int sti = checki.getStart();
    final int eni = checki.getEnd();
    for (final Pair<RegionRestriction, RegionRestriction> other : mDuplicates) {
      final RegionRestriction checkj = other.getA().getSequenceName().equals(mName) ? other.getA() : other.getB();
      final int stj = checkj.getStart();
      final int enj = checkj.getEnd();
      if (!(eni <= stj || enj <= sti)) {
        throw new IllegalArgumentException("Overlapping regions detected: " + checki + ", and " + checkj);
      }
    }

    mDuplicates.add(duplicate);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(mName);
    sb.append(" ").append(mPloidy);
    sb.append(" ").append(mLinear ? "linear" : "circular");
    sb.append(" ").append(mSequenceLength);
    if (mHaploidComplementName != null) {
      sb.append(" ~=").append(mHaploidComplementName);
    }
    sb.append(StringUtils.LS);
    for (final Pair<RegionRestriction, RegionRestriction> duplicate : mDuplicates) {
      sb.append("    ");
      sb.append(duplicate.getA());
      sb.append("  ");
      sb.append(duplicate.getB());
      sb.append(StringUtils.LS);
    }
    return sb.toString();
  }

}
