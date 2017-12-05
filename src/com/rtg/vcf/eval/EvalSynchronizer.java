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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Pair;
import com.rtg.util.ProgramState;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.VcfIterator;

/**
 * Handles vcfeval output writing, this base class ensures output is in order for each chromosome being evaluated.
 */
@TestClass("com.rtg.vcf.eval.SplitEvalSynchronizerTest")
public abstract class EvalSynchronizer implements Closeable {

  private final VariantSet mVariantSet;
  private final Queue<String> mNames = new LinkedList<>();
  private final Object mPhasingLock = new Object();

  /**
   * Constructor
   * @param variants returns separate sets of variants for each chromosome being processed
   */
  public EvalSynchronizer(VariantSet variants) {
    mVariantSet = variants;
  }

  /**
   * Gets the baseline variants on the specified reference sequence
   * @param sequenceName the sequence of interest
   * @return an iterator supplying the baseline variants on the specified sequence
   * @throws IOException when I/O fails
   */
  protected VcfIterator getBaselineVariants(String sequenceName) throws IOException {
    return mVariantSet.getBaselineVariants(sequenceName);
  }

  /**
   * Gets the called variants on the specified reference sequence
   * @param sequenceName the sequence of interest
   * @return an iterator supplying the called variants on the specified sequence
   * @throws IOException when I/O fails
   */
  protected VcfIterator getCalledVariants(String sequenceName) throws IOException {
    return mVariantSet.getCalledVariants(sequenceName);
  }

  /**
   * Loads a set from the underlying variant set with synchronization.
   * @return the next pair of sequence name and variant set or null if no more remain
   * @throws IOException when IO fails
   */
  synchronized Pair<String, Map<VariantSetType, List<Variant>>> nextSet() throws IOException {
    final Pair<String, Map<VariantSetType, List<Variant>>> set = mVariantSet.nextSet();
    if (set == null) {
      return null;
    }
    final String name = set.getA();
    synchronized (mNames) {
      mNames.add(name);
    }
    return set;
  }

  /**
   * Write the sets of variants to the appropriate output files. Will block until all previous sequences have been written by other threads.
   *
   * @param sequenceName current sequence name
   * @param baseline variants (either oriented if true positive, or variant if excluded)
   * @param calls variants (either oriented if true positive, or variant if excluded)
   * @param syncPoints the list of sync points
   * @param syncPoints2 the list of sync points from second pass (or empty list if no second pass)
   * @throws IOException when IO fails
   */
  void write(String sequenceName, Collection<? extends VariantId> baseline, Collection<? extends VariantId> calls, List<Integer> syncPoints, List<Integer> syncPoints2) throws IOException {
    Diagnostic.developerLog("Waiting to write variants for " + sequenceName);
    synchronized (mNames) {
      // wait for our turn to write results. Keeping output in order.
      while (!mNames.peek().equals(sequenceName)) {
        try {
          mNames.wait(1000);
          ProgramState.checkAbort();
        } catch (InterruptedException e) {
          ProgramState.checkAbort();
          throw new IllegalStateException("Interrupted. Unexpectedly", e);
        }
      }
    }

    Diagnostic.developerLog("Writing variants for " + sequenceName);
    writeInternal(sequenceName, baseline, calls, syncPoints, syncPoints2);
    Diagnostic.developerLog("Finished writing variants for " + sequenceName);

    synchronized (mNames) {
      // We are done with a sequence so take it off the queue
      mNames.remove();
      mNames.notifyAll();
    }
  }

  abstract void writeInternal(String sequenceName, Collection<? extends VariantId> baseline, Collection<? extends VariantId> calls, List<Integer> syncPoints, List<Integer> syncPoints2) throws IOException;

  void addPhasingCounts(int misPhasings, int correctPhasings, int unphasable) {
    synchronized (mPhasingLock) {
      addPhasingCountsInternal(misPhasings, correctPhasings, unphasable);
    }
  }

  abstract void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable);

  void finish() throws IOException {
    if (mVariantSet.getNumberOfSkippedBaselineVariants() > 0) {
      Diagnostic.warning("There were " + mVariantSet.getNumberOfSkippedBaselineVariants() + " problematic baseline variants skipped during loading (see vcfeval.log for details).");
    }
    if (mVariantSet.getNumberOfSkippedCalledVariants() > 0) {
      Diagnostic.warning("There were " + mVariantSet.getNumberOfSkippedCalledVariants() + " problematic called variants skipped during loading (see vcfeval.log for details).");
    }
  }

}
