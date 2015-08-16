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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusComparator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;

/**
 * Processes baseline and called variants in chromosome order, so they can be interleaved into a single output stream if required.
 */
@TestClass("com.rtg.vcf.eval.DefaultEvalSynchronizerTest")
public abstract class MergingEvalSynchronizer extends EvalSynchronizer {

  private static final Comparator<SequenceNameLocus> NATURAL_COMPARATOR = new SequenceNameLocusComparator();

  protected VcfRecord mBrv;
  protected VariantId mBv;
  protected VcfRecord mCrv;
  protected VariantId mCv;
  protected int mBid;
  protected int mCid;

  /**
   * Constructor
   * @param baseLineFile file containing the baseline VCF records
   * @param callsFile file containing the call VCF records
   * @param variants returns separate sets of variants for each chromosome being processed
   * @param ranges the ranges that variants are being read from
   */
  public MergingEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges) {
    super(baseLineFile, callsFile, variants, ranges);
  }

  @Override
  void writeInternal(String sequenceName, Collection<? extends VariantId> baseline, Collection<? extends VariantId> calls) throws IOException {
    final ReferenceRanges<String> subRanges = mRanges.forSequence(sequenceName);
    try (final VcfReader br = VcfReader.openVcfReader(mBaseLineFile, subRanges);
         final VcfReader cr = VcfReader.openVcfReader(mCallsFile, subRanges)) {
      final Iterator<? extends VariantId> bit = baseline.iterator();
      final Iterator<? extends VariantId> cit = calls.iterator();
      mBv = null;
      mBrv = null;
      mBid = 0;
      mCv = null;
      mCrv = null;
      mCid = 0;
      while (true) {
        // Advance each iterator if need be
        if (mBv == null && bit.hasNext()) {
          mBv = bit.next();
        }
        if (mBrv == null && br.hasNext()) {
          mBrv = br.next();
          mBid++;
          resetBaselineRecordFields(mBrv);
        }
        if (mCv == null && cit.hasNext()) {
          mCv = cit.next();
        }
        if (mCrv == null && cr.hasNext()) {
          mCrv = cr.next();
          mCid++;
          resetCallRecordFields(mCrv);
        }

        if (mBrv == null && mCrv == null) { // Finished
          break;
        } else if (mBrv == null) {
          processCall();
        } else if (mCrv == null) {
          processBaseline();
        } else { // Compare positions to work out which to process
          final int order = NATURAL_COMPARATOR.compare(mBrv, mCrv);
          if (order < 0) {
            processBaseline();
          } else if (order > 0) {
            processCall();
          } else {
            processBoth();
          }
        }
      }
    }
  }

  /** Deal with the current called record. We have no baseline record at this start and end position */
  private void processCall() throws IOException {
    assert mCrv != null;
    if (mCv == null || mCv.getId() != mCid) {
      handleUnknownCall();
      mCrv = null;
    } else {
      assert mCv.getId() == mCid;
      handleKnownCall();
      mCv = null;
      mCrv = null;
    }
  }

  /** Deal with the current baseline record. We have no call record at this start and end position */
  private void processBaseline() throws IOException {
    assert mBrv != null;
    if (mBv == null || mBv.getId() != mBid) {
      handleUnknownBaseline();
      mBrv = null;
    } else {
      handleKnownBaseline();
      mBv = null;
      mBrv = null;
    }
  }

  /** Deal with the case where we have both call and baseline records with matching start and end position */
  private void processBoth() throws IOException {
    assert mCrv.getEnd() == mBrv.getEnd();
    if (mCv == null || mCv.getId() != mCid) {
      handleUnknownCall();
      mCrv = null;
    } else {
      handleKnownBoth();
      mCv = null;
      mCrv = null;
    }
  }

  @Override
  void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    // Do nothing
  }

  /**
   * Called immediately after each baseline VCF record is read.
   * @param rec the newly read record
   */
  protected abstract void resetBaselineRecordFields(VcfRecord rec);

  /**
   * Called immediately after each call VCF record is read.
   * @param rec the newly read record
   */
  protected abstract void resetCallRecordFields(VcfRecord rec);

  /**
   * Process a baseline record where we can not associate a matching baseline variant.
   * Usually where a variant was skipped during loading, for example failed or same-as-ref calls.
   * @throws IOException if an error occurs during result writing
   */
  protected abstract void handleUnknownBaseline() throws IOException;

  /**
   * Process a call record where we can not associate a matching call variant.
   * Usually where a variant was skipped during loading, for example failed or same-as-ref calls.
   * @throws IOException if an error occurs during result writing
   */
  protected abstract void handleUnknownCall() throws IOException;

  /**
   * Deal with the current called record and its matching called variant.
   * We have no baseline record at this start and end position
   * @throws IOException if an error occurs during result writing
   */
  protected abstract void handleKnownCall() throws IOException;

  /**
   * Deal with the current baseline record and its matching baseline variant.
   * We have no call record at this start and end position
   * @throws IOException if an error occurs during result writing
   */
  protected abstract void handleKnownBaseline() throws IOException;

  /**
   * Deal with the case where we have both call and baseline records with the
   * same start and end position, and their matching variants.
   * After this function the current call record and variant will be cleared, but the baseline will not.
   * If the implementation is finished with the baseline record and variant, they should be explicitly cleared.
   * @throws IOException if an error occurs during result writing
   */
  protected abstract void handleKnownBoth() throws IOException;
}
