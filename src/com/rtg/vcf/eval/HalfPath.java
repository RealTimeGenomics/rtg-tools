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

import static com.rtg.util.StringUtils.LS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rtg.mode.DnaUtils;
import com.rtg.util.BasicLinkedListNode;
import com.rtg.util.Utils;
import com.rtg.util.intervals.Range;

/**
 * One half of a path that reconciles two sequences of variants.
 */
public class HalfPath implements Comparable<HalfPath> {

  private final HaplotypePlayback mHaplotypeA;
  private HaplotypePlayback mHaplotypeB;

  private BasicLinkedListNode<OrientedVariant> mIncluded;
  private BasicLinkedListNode<Variant> mExcluded;

  private int mVariantIndex = -1;  // Index of last variant added
  private int mVariantEndPosition; // End of last variant added
  private int mIncludedVariantEndPosition = 0; // Last variant included

  private boolean mFinishedTypeA;
  private boolean mFinishedTypeB;

  /**
   * Construct an empty <code>HalfPath</code>
   * @param template the template on which the half path resides
   */
  HalfPath(byte[] template) {
    mHaplotypeA = new HaplotypePlayback(template);
    mHaplotypeB = null;
  }

  /**
   * Construct a child <code>HalfPath</code>
   * @param path the parent half path
   */
  HalfPath(HalfPath path) {
    mIncluded = path.mIncluded;
    mExcluded = path.mExcluded;
    mHaplotypeA = path.mHaplotypeA.copy();
    mHaplotypeB = path.mHaplotypeB == null ? null : path.mHaplotypeB.copy();
    mVariantEndPosition = path.mVariantEndPosition;
    mVariantIndex = path.mVariantIndex;
    mIncludedVariantEndPosition = path.mIncludedVariantEndPosition;
    mFinishedTypeA = path.mFinishedTypeA;
    mFinishedTypeB = path.mFinishedTypeB;
  }

  String dumpHaplotypes() {
    return dumpHaplotypes(new Range(0, mHaplotypeA.mTemplate.length));
  }

  String dumpHaplotypes(Range region) {
    final List<OrientedVariant> included = new ArrayList<>();
    if (mIncluded != null) {
      for (final OrientedVariant v : mIncluded) {
        // Note, this only includes variants that are contained within the requested range, otherwise we cannot
        // determine the start and end position of the haplotypes (which may be mid variant playback).
        if (v.getEnd() <= region.getEnd() && v.getStart() >= region.getStart()) {
          included.add(v);
        }
      }
    }
    Collections.reverse(included);

    final StringBuilder sb = new StringBuilder(region.toString());
    sb.append(' ');
    sb.append(replayAll(included, region));

    if (mHaplotypeB != null) {
      for (int i = 0; i < included.size(); i++) {
        included.set(i, included.get(i).other());
      }
      sb.append("|").append(replayAll(included, region));
    }
    return sb.toString();
  }

  // Return the full haplotype sequence with respect to the given variants
  private String replayAll(List<OrientedVariant> included, Range region) {
    final StringBuilder sb = new StringBuilder();
    final HaplotypePlayback haplotype = new HaplotypePlayback(mHaplotypeA.mTemplate);
    for (OrientedVariant v : included) {
      haplotype.addVariant(v);
    }
    if (region.getStart() > 0) {
      haplotype.moveForward(region.getStart() - 1);
    }
    while (haplotype.hasNext()) {
      haplotype.next();
      if (haplotype.templatePosition() >= region.getEnd()) {
        break;
      }
      if (haplotype.templatePosition() >= region.getStart()) {
        sb.append(!haplotype.isOnTemplate() ? DnaUtils.getBaseLower(haplotype.nt()) : DnaUtils.getBase(haplotype.nt()));
      }
    }
    return sb.toString();
  }

  /**
   * @param var the variant to check
   * @return true if the variant starts later than the end of the last included one.
   */
  public boolean isNew(OrientedVariant var) {
    if (var.getStart() >= mIncludedVariantEndPosition) {
      return true; // Can be added without considering overlapping
    }
    // Check if we can include this variant by overlapping
    return mHaplotypeA.isNew(var) && (mHaplotypeB == null || mHaplotypeB.isNew(var.other()));
  }

  void exclude(Variant var, int varIndex) {
    assert varIndex > mVariantIndex;
    mExcluded = new BasicLinkedListNode<>(var, mExcluded);
    mVariantEndPosition = Math.max(mVariantEndPosition, var.getEnd());
    mVariantIndex = varIndex;
    //mLastVariant = var;
  }

  void include(OrientedVariant var, int varIndex) {
    assert varIndex > mVariantIndex;
    mIncluded = new BasicLinkedListNode<>(var, mIncluded);
    mVariantEndPosition = Math.max(mVariantEndPosition, var.getEnd());
    mVariantIndex = varIndex;
    mIncludedVariantEndPosition = Math.max(mIncludedVariantEndPosition, var.variant().getEnd());

    // Lazy creation of the B haplotype
    if (mHaplotypeB == null && var.isHeterozygous()) {
      mHaplotypeB = mHaplotypeA.copy();
    }

    mHaplotypeA.addVariant(var);

    if (mHaplotypeB != null) {
      // Add the other side of the heterozygous diploid variant.
      mHaplotypeB.addVariant(var.other());
    }
  }

  /**
   * Test whether a deficit of variant bases are upstream in the queue in order to perform a step.
   * @return false indicates that no variants need to be immediately enqueued
   */
  boolean wantsFutureVariantBases() {
    return mHaplotypeA.wantsFutureVariantBases() || (mHaplotypeB != null && mHaplotypeB.wantsFutureVariantBases());
  }

  void step() {
    haplotypeAStep();
    haplotypeBStep();
  }

  void haplotypeAStep() {
    if (mHaplotypeB == null) {
      mHaplotypeB = mHaplotypeA.copy();
    }
    if (mHaplotypeA.hasNext()) {
      mHaplotypeA.next();
    } else {
      mFinishedTypeA = true;
    }
  }

  void haplotypeBStep() {
    if (mHaplotypeB == null) {
      mHaplotypeB = mHaplotypeA.copy();
    }
    if (mHaplotypeB.hasNext()) {
      mHaplotypeB.next();
    } else {
      mFinishedTypeB = true;
    }
  }

  /**
   * @return true if the half path has reached the end of the template on both haplotypes
   */
  public boolean finished() {
    return mFinishedTypeB && mFinishedTypeA;
  }

  /**
   * @return true if the half path has reached the end of the template on the A haplotype
   */
  public boolean finishedHaplotypeA() {
    return mFinishedTypeA;
  }

  /**
   * @return true if the half path has reached the end of the template on the B haplotype
   */
  public boolean finishedHaplotypeB() {
    return mFinishedTypeB;
  }

  public BasicLinkedListNode<OrientedVariant> getIncluded() {
    return mIncluded;
  }

  public BasicLinkedListNode<Variant> getExcluded() {
    return mExcluded;
  }

  /**
   * Get variant index.
   * @return Returns the variant index.
   */
  public int getVariantIndex() {
    return mVariantIndex;
  }

  /**
   * Set variant index.
   * @param index the new variant index.
   */
  public void setVariantIndex(int index) {
    mVariantIndex = index;
  }

  /**
   * Get variant end position of last variant added (included or excluded).
   * @return Returns the variant end position.
   */
  public int getVariantEndPosition() {
    return mVariantEndPosition;
  }

  /**
   * Retrieve the leading reference so we can add the next variants
   * @return the leading reference
   */
  public int getPosition() {
    if (mHaplotypeB == null) {
      return mHaplotypeA.templatePosition();
    }
    if (mHaplotypeA.templatePosition() > mHaplotypeB.templatePosition()) {
      return mHaplotypeA.templatePosition();
    } else {
      return mHaplotypeB.templatePosition();
    }
  }

  /**
   * @return the next base on the A haplotype
   */
  public byte nextHaplotypeABase() {
    return nextBase(true);
  }

  /**
   * @return the next base on the B haplotype
   */
  public byte nextHaplotypeBBase() {
    return nextBase(false);
  }

  private byte nextBase(boolean haplotypeA) {
    if (haplotypeA || mHaplotypeB == null) {
      return mHaplotypeA.nt();
    } else {
      return mHaplotypeB.nt();
    }
  }
  void moveForward(int position) {
    if (mHaplotypeB != null) {
      mHaplotypeB.moveForward(position);
    }
    mHaplotypeA.moveForward(position);
  }

  boolean matches(HalfPath other) {
    if (!finishedHaplotypeA() && !other.finishedHaplotypeA() && nextHaplotypeABase() != other.nextHaplotypeABase()) {
      return false;
    }
    if (!finishedHaplotypeB() && !other.finishedHaplotypeB() && nextHaplotypeBBase() != other.nextHaplotypeBBase()) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(HalfPath that) {
    final int c0 = this.mHaplotypeA.templatePosition() - that.mHaplotypeA.templatePosition();
    if (c0 != 0) {
      return c0;
    }

    if (this.mHaplotypeB == null && that.mHaplotypeB != null) {
      return -1;
    }
    if (this.mHaplotypeB != null && that.mHaplotypeB == null) {
      return +1;
    }
    final int plus = this.mHaplotypeA.compareTo(that.mHaplotypeA);
    if (this.mHaplotypeB == null || plus != 0) {
      return plus;
    }
    return this.mHaplotypeB.compareTo(that.mHaplotypeB);
  }

  /**
   * performs a compare of the template position of both haplotypes
   * @return &lt; 0 if the B haplotype is leading, &gt; 0  if A haplotype is leading, 0 if in the same place
   */
  public int compareHaplotypePositions() {
    if (mHaplotypeB == null) {
      return 0;
    }
    return mHaplotypeA.templatePosition() - mHaplotypeB.templatePosition();
  }

  /**
   * Check whether this half path is fully on the template (i.e. no haplotypes are within a variant)
   * @return true if the haplotypes are on the template.
   */
  public boolean isOnTemplate() {
    if (mHaplotypeB == null) {
      return mHaplotypeA.isOnTemplate();
    }
    return mHaplotypeA.isOnTemplate() && mHaplotypeB.isOnTemplate();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof HalfPath && compareTo((HalfPath) obj) == 0;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mHaplotypeB == null ? 0 : mHaplotypeB.hashCode(), mHaplotypeA.hashCode(), mVariantEndPosition);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(" +:");
    sb.append(mHaplotypeA.templatePosition() + 1);
    if (!mHaplotypeA.isOnTemplate()) {
      sb.append('^').append(mHaplotypeA.positionInVariant());
    }
    if (mHaplotypeB != null) {
      sb.append(" -:");
      sb.append(mHaplotypeB.templatePosition() + 1);
      if (!mHaplotypeB.isOnTemplate()) {
        sb.append('^').append(mHaplotypeB.positionInVariant());
      }
    }
    sb.append(LS);
    sb.append("included:");
    sb.append(listToString(mIncluded));
    sb.append(LS);
    sb.append("excluded:");
    sb.append(listToString(mExcluded));
    sb.append(LS);
    return sb.toString();
  }

  static <T> String listToString(BasicLinkedListNode<T> list) {
    final StringBuilder sb = new StringBuilder();
    sb.append("[");
    String join = "";
    if (list != null) {
      for (final T v : list) {
        sb.append(join);
        sb.append(v.toString());
        join = ", ";
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
