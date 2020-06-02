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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.rtg.mode.DnaUtils;
import com.rtg.util.BasicLinkedListNode;
import com.rtg.util.Utils;
import com.rtg.util.intervals.Range;
import com.rtg.visualization.DisplayHelper;

/**
 * One half of a path that reconciles two sequences of variants.
 */
public final class HalfPath implements Comparable<HalfPath> {

  private final HaplotypePlayback[] mHaplotypes;

  private BasicLinkedListNode<OrientedVariant> mIncluded;
  private BasicLinkedListNode<Variant> mExcluded;

  private int mVariantIndex = -1;  // Index of last variant added
  private int mVariantEndPosition; // End of last variant added
  private int mIncludedVariantEndPosition = 0; // Last variant included

  /**
   * Construct an empty <code>HalfPath</code>
   * @param haplotypes the number of haplotypes to generate
   * @param template the template on which the half path resides
   */
  HalfPath(int haplotypes, byte[] template) {
    mHaplotypes = new HaplotypePlayback[haplotypes];
    for (int i = 0; i < mHaplotypes.length; i++) {
      mHaplotypes[i] = new HaplotypePlayback(template);
    }
  }

  /**
   * Construct a child <code>HalfPath</code>
   * @param path the parent half path
   */
  HalfPath(HalfPath path) {
    mIncluded = path.mIncluded;
    mExcluded = path.mExcluded;
    mHaplotypes = new HaplotypePlayback[path.mHaplotypes.length];
    for (int i = 0; i < mHaplotypes.length; i++) {
      mHaplotypes[i] = path.mHaplotypes[i].copy();
    }
    mVariantEndPosition = path.mVariantEndPosition;
    mVariantIndex = path.mVariantIndex;
    mIncludedVariantEndPosition = path.mIncludedVariantEndPosition;
  }

  String dumpHaplotypes() {
    return dumpHaplotypes(new Range(0, mHaplotypes[0].mTemplate.length));
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
    for (int i = 0; i < mHaplotypes.length; i++) {
      final int hap = i;
      sb.append(i == 0 ? ' ' : "|");
      sb.append(replayAll(included.stream().map(v -> v.allele(hap)).collect(Collectors.toList()), region));
    }
    return sb.toString();
  }

  // Return the full haplotype sequence with respect to the given variants
  private String replayAll(List<Allele> included, Range region) {
    final StringBuilder sb = new StringBuilder();
    final HaplotypePlayback haplotype = new HaplotypePlayback(mHaplotypes[0].mTemplate);
    for (Allele a : included) {
      haplotype.add(a);
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
        final char c = haplotype.isOnTemplate() ? DnaUtils.getBaseLower(haplotype.nt()) : DnaUtils.getBase(haplotype.nt());
        sb.append(haplotype.isOnTemplate() ? c : DisplayHelper.DEFAULT.decorateBases(String.valueOf(c)));
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
    for (int i = 0; i < mHaplotypes.length; i++) {
      if (!mHaplotypes[i].isNew(var.allele(i))) {
        return false;
      }
    }
    return true;
  }

  void exclude(Variant var, int varIndex) {
    assert varIndex > mVariantIndex;
    mExcluded = new BasicLinkedListNode<>(var, mExcluded);
    mVariantEndPosition = Math.max(mVariantEndPosition, var.getEnd());
    mVariantIndex = varIndex;
  }

  void include(OrientedVariant var, int varIndex) {
    assert varIndex > mVariantIndex;
    assert var.alleleIds().length >= mHaplotypes.length;
    mIncluded = new BasicLinkedListNode<>(var, mIncluded);
    mVariantEndPosition = Math.max(mVariantEndPosition, var.getEnd());
    mVariantIndex = varIndex;
    mIncludedVariantEndPosition = Math.max(mIncludedVariantEndPosition, var.variant().getEnd());
    for (int i = 0; i < mHaplotypes.length; i++) {
      mHaplotypes[i].add(var.allele(i));
    }
  }

  /**
   * Test whether a deficit of variant bases are upstream in the queue in order to perform a step.
   * @return false indicates that no variants need to be immediately enqueued
   */
  boolean wantsFutureVariantBases() {
    return Arrays.stream(mHaplotypes).anyMatch(HaplotypePlayback::wantsFutureVariantBases);
  }

  /**
   * Step all haplotypes
   */
  void step() {
    for (HaplotypePlayback h : mHaplotypes) {
      h.step();
    }
  }

  /**
   * Step a single specified haplotype
   * @param hap haplotype index
   */
  void step(int hap) {
    mHaplotypes[hap].step();
  }

  /**
   * @return true if the half path has reached the end of the template on all haplotypes
   */
  public boolean finished() {
    return Arrays.stream(mHaplotypes).allMatch(HaplotypePlayback::finished);
  }

  /**
   * @param hap haplotype index
   * @return true if the half path has reached the end of the template on the specified haplotype
   */
  boolean finishedHaplotype(int hap) {
    return mHaplotypes[hap].finished();
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
   * Get variant end position of last variant included.
   * @return Returns the variant end position.
   */
  public int getIncludedVariantEndPosition() {
    return mIncludedVariantEndPosition;
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
    int m = mHaplotypes[0].templatePosition();
    for (int i = 1; i < mHaplotypes.length; i++) {
      m = Math.max(m, mHaplotypes[i].templatePosition());
    }
    return m;
  }

  /**
   * @param hap haplotype index
   * @return the next base on the specified haplotype
   */
  public byte nextHaplotypeBase(int hap) {
    return mHaplotypes[hap].nt();
  }

  void moveForward(int position) {
    for (HaplotypePlayback hap : mHaplotypes) {
      hap.moveForward(position);
    }
  }

  boolean matches(HalfPath other) {
    assert this.mHaplotypes.length == other.mHaplotypes.length;
    for (int i = 0; i < mHaplotypes.length; i++) {
      if (!mHaplotypes[i].matches(other.mHaplotypes[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int compareTo(HalfPath that) {
    assert this.mHaplotypes.length == that.mHaplotypes.length;
    for (int i = 0; i < mHaplotypes.length; i++) {
      final int plus = this.mHaplotypes[i].compareTo(that.mHaplotypes[i]);
      if (plus != 0) {
        return plus;
      }
    }
    return 0;
  }

  /**
   * performs a compare of the template positions of all haplotypes
   * @return -1 if all haplotypes are in the same place, otherwise the index of the trailing haplotype
   */
  public int trailingHaplotype() {
    int minh = 0;
    int minp = mHaplotypes[0].templatePosition();
    int maxp = mHaplotypes[0].templatePosition();
    for (int i = 1; i < mHaplotypes.length; i++) {
      final int p = mHaplotypes[i].templatePosition();
      if (p < minp) {
        minp = p;
        minh = i;
      } else if (p > minp) {
        maxp = p;
      }
    }
    return minp == maxp ? -1 : minh;
  }

  /**
   * Check whether this half path is fully on the template (i.e. no haplotypes are within a variant)
   * @return true if the haplotypes are on the template.
   */
  public boolean isOnTemplate() {
    return Arrays.stream(mHaplotypes).allMatch(HaplotypePlayback::isOnTemplate);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof HalfPath && compareTo((HalfPath) obj) == 0;
  }

  @Override
  public int hashCode() {
    int hash = mVariantEndPosition;
    for (final HaplotypePlayback haplotype : mHaplotypes) {
      hash = Utils.pairHash(hash, haplotype.hashCode());
    }
    return hash;
  }

  private String hapId(int hap) {
    switch (mHaplotypes.length) {
      case 1:
        return "x";
      case 2:
        return hap == 0 ? "^" : "v";
      default:
        return Integer.toString(hap);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < mHaplotypes.length; i++) {
      final HaplotypePlayback h = mHaplotypes[i];
      if (i > 0) {
        sb.append(":");
      }
      sb.append(h.templatePosition() + 1);
      if (!h.isOnTemplate()) {
        sb.append('.').append(h.positionInAllele());
      }
      sb.append(hapId(i));
    }

    sb.append("  ");
    sb.append(DisplayHelper.DEFAULT.decorateForeground("included:", DisplayHelper.CYAN));
    sb.append(DisplayHelper.DEFAULT.decorateForeground(BasicLinkedListNode.toReversedList(mIncluded).toString(), DisplayHelper.GREEN));
    sb.append(' ');
    sb.append(DisplayHelper.DEFAULT.decorateForeground("excluded:", DisplayHelper.CYAN));
    sb.append(DisplayHelper.DEFAULT.decorateForeground(BasicLinkedListNode.toReversedList(mExcluded).toString(), DisplayHelper.RED));
    return sb.toString();
  }

}
