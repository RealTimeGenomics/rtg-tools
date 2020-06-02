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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.rtg.mode.DnaUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 */
public final class HaplotypePlayback implements Integrity, Comparable<HaplotypePlayback> {

  private static final int INVALID = -1;

  /** Sorted list of alleles yet to be processed. */
  private final Queue<Allele> mAlleles;

  final byte[] mTemplate;

  /** Position in template (start of current allele if one is active). 0 based.*/
  private int mTemplatePosition = -1;

  /** Position in allele. INVALID if not currently in allele. 0 based. */
  private int mPositionInAllele;

  private int mLastAlleleEnd = -1;

  /** Allele currently in or the next one. */
  private Allele mNextAllele;
  private boolean mFinished;

  HaplotypePlayback(final byte[] template) {
    mAlleles = new LinkedList<>();
    mTemplate = template;
    mNextAllele = null;
    mPositionInAllele = INVALID;
    assert globalIntegrity();
  }

  private HaplotypePlayback(HaplotypePlayback old) {
    mAlleles = new LinkedList<>(old.mAlleles);
    mTemplate = old.mTemplate;
    mNextAllele = old.mNextAllele;
    mTemplatePosition = old.mTemplatePosition;
    mPositionInAllele = old.mPositionInAllele;
    mLastAlleleEnd = old.mLastAlleleEnd;
    mFinished = old.mFinished;
    assert globalIntegrity();
  }

  /**
   * Add an <code>Allele</code> to this <code>HaplotypePlayback</code>
   * @param a the <code>Allele</code> to add
   */
  public void add(Allele a) {
    if (a == null) {  // Allow null allele to indicate no playback
      return;
    }
    assert a.getStart() > mTemplatePosition;
    if (a.getStart() == a.getEnd() && a.nt().length == 0) { // Adding the opposite side of a pure insert is redundant
      return;
    }
    mLastAlleleEnd = a.getEnd();
    if (mNextAllele == null) {
      mNextAllele = a;
    } else {
      mAlleles.add(a);
    }
    assert integrity();
  }

  /**
   * @param allele the allele to check
   * @return true if the allele can be added to this HaplotypePlayback
   */
  public boolean isNew(Allele allele) {
    return allele == null || allele.getStart() >= mLastAlleleEnd;
  }

  /**
   * Get the current nucleotide in this playback. Taken from template or allele.
   * @return the current nucleotide in this playback.
   */
  byte nt() {
    if (mPositionInAllele == INVALID) {
      return mTemplatePosition < mTemplate.length ? mTemplate[mTemplatePosition] : DnaUtils.UNKNOWN_RESIDUE;
    }
    return mNextAllele.nt()[mPositionInAllele];
  }

  /**
   * Test if there are more nucleotides available.
   * @return true if there are more nucleotides available.
   */
  boolean hasNext() {
    return mTemplatePosition < mTemplate.length - 1;
  }

  void step() {
    if (hasNext()) {
      next();
    } else {
      mFinished = true;
    }
  }

  boolean finished() {
    return mFinished;
  }

  /**
   * Test whether a deficit of variant bases are upstream in the queue in order to perform a step.
   * @return false indicates that no variants need to be immediately enqueued
   */
  boolean wantsFutureVariantBases() {
    if (mNextAllele == null) {
      return true;
    }
    if (mPositionInAllele != INVALID && mPositionInAllele < mNextAllele.nt().length - 1) {
      return false;
    }
    for (Allele a : mAlleles) {
      if (a.nt().length > 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Step to the next nucleotide.
   * @throws NoSuchElementException if no more nucleotides available.
   */
  void next() {
    if (!hasNext()) {
      throw new NoSuchElementException("Attempt to fetch nucleotide past the end of the template");
    }
    if (isOnTemplate()) {
      ++mTemplatePosition;
      if (mNextAllele != null && mNextAllele.getStart() == mTemplatePosition) { // Position to consume the allele
        mPositionInAllele = 0;
      }
    } else {
      assert mPositionInAllele != INVALID;
      ++mPositionInAllele;
    }
    assert mNextAllele != null || mPositionInAllele == INVALID;
    if (mNextAllele != null) {
      while (true) {
        //dont forget that variants may come back to back
        final byte[] norm = mNextAllele.nt();
        if (mPositionInAllele != norm.length) { // Haven't reached the end of the current allele.
          //this also catches the case when mPositionInAllele == INVALID
          assert integrity();
          break;
        }
        // Finished allele, so position for next baseStart consuming next allele from the queue.
        mTemplatePosition = mNextAllele.getEnd();
        mPositionInAllele = INVALID;
        if (!mAlleles.isEmpty()) {
          mNextAllele = mAlleles.poll();
        } else {
          mNextAllele = null;
          assert integrity();
          break;
        }
        if (mTemplatePosition < mNextAllele.getStart()) {
          assert integrity();
          break;
        }
        mPositionInAllele = 0;
        if (mTemplatePosition != mNextAllele.getStart()) {
          throw new SlimException("Out of order alleles during replay: pos=" + mTemplatePosition + " allele=" + mNextAllele);
        }
      }
    }
    assert integrity();
  }

  /**
   * Test if the haplotype is currently within a variant.
   * @return true iff not in a variant.
   */
  boolean isOnTemplate() {
    return mPositionInAllele == INVALID;
  }

  /**
   * Get the current position within an allele.
   * @return the current position within the current allele.
   */
  int positionInAllele() {
    return mPositionInAllele;
  }

  /**
   * Get the current position in the template. Use <code>isOnTemplate</code> to determine
   * whether the haplotype is also within a current variant.
   * 0 based and may be equal to length of template.
   * @return the current position in the template.
   */
  int templatePosition() {
    return mTemplatePosition;
  }

  /**
   * Get the Allele we are currently in
   * @return the current allele or null if we aren't in a variant
   */
  Allele currentAllele() {
    if (isOnTemplate()) {
      return null;
    }
    return mNextAllele;
  }

  /**
   * Force the template position to the first template position at or beyond "position" and the current template position which is not
   * in a variant. Force the state of any otherwise unmarked variants as <code>UNKNOWN</code>.
   * @param position to be forced to. (0 based)
   */
  void moveForward(final int position) {
    assert position >= 0 && position <= mTemplate.length;
    if (!isOnTemplate()) {
      throw new IllegalStateException("Attempt to move forward while still in a variant");
    }
    mTemplatePosition = position - 1;
    next();
    assert templatePosition() >= position && isOnTemplate();
    assert integrity();
  }


  @Override
  public String toString() {
    return "HaplotypePlayback: position=" + templatePosition() + " inPosition=" + mPositionInAllele + LS
    + "current:" + nullToString(mNextAllele) + LS
    + "future:" + mAlleles + LS;

  }

  private String nullToString(final Object obj) {
    if (obj == null) {
      return null;
    }
    return obj.toString();
  }

  @Override
  public boolean globalIntegrity() {
    Exam.assertTrue(integrity());
    return true;
  }

  @Override
  public boolean integrity() {
    Exam.assertNotNull(mAlleles);
    Exam.assertNotNull(mTemplate);
    Exam.assertTrue(-1 <= mTemplatePosition && mTemplatePosition <= mTemplate.length);
    if (mNextAllele == null) {
      Exam.assertTrue(mAlleles.isEmpty());
    }
    if (mPositionInAllele == HaplotypePlayback.INVALID) {
      Exam.assertTrue(toString(), mNextAllele == null || mTemplatePosition <= mNextAllele.getStart());
    } else {
      Exam.assertTrue(mNextAllele != null && mTemplatePosition == mNextAllele.getStart());
      if (!(0 <= mPositionInAllele && mPositionInAllele < mNextAllele.nt().length)) {
        System.err.println(this);
      }
      Exam.assertTrue(0 <= mPositionInAllele && mPositionInAllele < mNextAllele.nt().length);
    }
    return true;
  }

  /** @return a copy of this object for use in another path */
  public HaplotypePlayback copy() {
    return new HaplotypePlayback(this);
  }

  boolean matches(HaplotypePlayback other) {
    return finished() || other.finished() || nt() == other.nt();
  }

  /**
   * Performs a comparison of the alleles in this path that aren't yet resolved.
   * @param that the HaplotypePlayback to compare to.
   * @return an indication that this is further advanced along its path or not.
   */
  @Override
  public int compareTo(HaplotypePlayback that) {
    final int position = this.templatePosition() - that.templatePosition();
    if (position != 0) {
      return position;
    }
    if (this.mNextAllele == null) {
      if (that.mNextAllele == null) {
        return 0;
      }
      return -1;
    }
    if (that.mNextAllele == null) {
      return 1;
    }

    final int current = this.mNextAllele.compareTo(that.mNextAllele);
    if (current != 0) {
      return current;
    }
    final int varPos = this.mPositionInAllele - that.mPositionInAllele;
    if (varPos != 0) {
      return varPos;
    }
    final Iterator<Allele> thisIt = this.mAlleles.iterator();
    final Iterator<Allele> thatIt = that.mAlleles.iterator();
    while (thisIt.hasNext()) {
      if (!thatIt.hasNext()) {
        return 1;
      }
      final int future = thisIt.next().compareTo(thatIt.next());
      if (future != 0) {
        return future;
      }

    }
    if (thatIt.hasNext()) {
      return -1;
    }
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof HaplotypePlayback && compareTo((HaplotypePlayback) obj) == 0;
  }

  @Override
  public int hashCode() {
    int hash = Utils.pairHash(this.templatePosition(), mNextAllele == null ? 0 : mNextAllele.hashCode(), this.mPositionInAllele);
    for (final Allele v : this.mAlleles) {
      hash = Utils.pairHash(hash, v.hashCode());
    }
    return hash;
  }


}
