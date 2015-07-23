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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.rtg.util.Pair;
import com.rtg.util.ProgramState;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;

/**
 * When running VcfEvalTask in multithreading fashion keep reading and output in order for each chromosome being evaluated.
 */
class EvalSynchronizer {
  private final Queue<String> mNames = new LinkedList<>();
  private final VariantSet mVariantSet;

  private final RocContainer mRoc;
  private final VcfWriter mTpCalls;
  private final VcfWriter mTpBase;
  private final VcfWriter mFp;
  private final VcfWriter mFn;
  private final File mBaseLineFile;
  private final File mCallsFile;
  private final Object mVariantLock = new Object();
  private final Object mPhasingLock = new Object();
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;
  int mTruePositives = 0;
  int mFalseNegatives = 0;
  int mFalsePositives = 0;

  /**
   * @param vs the set of variants to evaluate
   * @param tpCalls True positives are written to this
   * @param fp false positives are written here
   * @param fn false negatives are written here
   * @param tpBase True positives (baseline version) are written here, may be null.
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param roc the container for ROC data
   */
  EvalSynchronizer(VariantSet vs, VcfWriter tpCalls, VcfWriter fp, VcfWriter fn, VcfWriter tpBase, File baseLineFile, File callsFile, RocContainer roc) {
    mVariantSet = vs;
    mTpCalls = tpCalls;
    mTpBase = tpBase;
    mFp = fp;
    mFn = fn;
    mBaseLineFile = baseLineFile;
    mCallsFile = callsFile;
    mRoc = roc;
  }

  /**
   * Dump all of the variants in a Collection to an output stream
   * @param out where to write the variants
   * @param variants a collection of variants
   * @param vcfReader the VCF reader to get the VCF records from for output
   * @param <T> Type of variant
   * @throws IOException IO exceptions require too many comments.
   */
  private static <T extends SequenceNameLocus> void writeVariants(VcfWriter out, VcfReader vcfReader, Collection<T> variants) throws IOException {
    if (variants == null) {
      return;
    }
    for (final SequenceNameLocus v : variants) {
      final DetectedVariant dv = (DetectedVariant) (v instanceof OrientedVariant ? ((OrientedVariant) v).variant() : v);
      VcfRecord rec = null;
      while (vcfReader.hasNext()) {
        final VcfRecord r = vcfReader.next();

        // XXX This loop should be applying the same filtering criteria that were applied when the variants were initially loaded
        // Currently if the input had multiple records with the same position and ref allele (some of which were ignored during initial
        // loading), the first one will be output, which may not be correct.

        final boolean hasPreviousNt = VcfUtils.hasRedundantFirstNucleotide(r);
        final SequenceNameLocusSimple adjusted = new SequenceNameLocusSimple(r.getSequenceName(), r.getStart() + (hasPreviousNt ? 1 : 0), r.getEnd());
        if (DetectedVariant.NATURAL_COMPARATOR.compare(adjusted, dv) == 0) {
          rec = r;
          break;
        }
      }
      if (rec != null) {
        out.write(rec);
      } else {
        throw new SlimException("Variant object \"" + v.toString() + "\"" + " does not have a corresponding VCF record in given reader");
      }
    }
  }

  /**
   * Loads a set from the underlying variant set with synchronization.
   * @return the next pair of sequence name and variant set or null if no more remain
   * @throws IOException when IO fails
   */
  synchronized Pair<String, Map<VariantSetType, List<DetectedVariant>>> nextSet() throws IOException {
    final Pair<String, Map<VariantSetType, List<DetectedVariant>>> set = mVariantSet.nextSet();
    if (set == null) {
      return null;
    }
    final String name = set.getA();
    synchronized (mNames) {
      mNames.add(name);
    }
    return set;
  }

  void addRocLine(RocLine line, DetectedVariant v) {
    synchronized (mVariantLock) {
      mRoc.addRocLine(line.mPrimarySortValue, line.mWeight, v);
    }
  }
  void addPhasings(int misPhasings, int correctPhasings, int unphasable) {
    synchronized (mPhasingLock) {
      mMisPhasings += misPhasings;
      mUnphasable += unphasable;
      mCorrectPhasings += correctPhasings;
    }
  }

  /**
   * Write the sets of variants to the appropriate output files. Will block until all previous sequences have been written by other threads.
   *
   * @param sequenceName current sequence name
   * @param tp True positive variant calls (calls, equiv with baseline)
   * @param fp False positive variant calls (calls, not in baseline)
   * @param fn False negative variant calls (baseline, not in calls)
   * @param tpbase True positive variant calls (baseline, equiv with calls)
   * @throws IOException when IO fails
   */
  void write(String sequenceName, Collection<OrientedVariant> tp, Collection<? extends SequenceNameLocus> fp, Collection<? extends SequenceNameLocus> fn, Collection<OrientedVariant> tpbase) throws IOException {
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
    if (tp != null) {
      try (final VcfReader callsReader = VcfReader.openVcfReader(mCallsFile, new RegionRestriction(sequenceName))) {
        writeVariants(mTpCalls, callsReader, tp);
      }
    }
    if (fp != null) {
      try (final VcfReader callsReader = VcfReader.openVcfReader(mCallsFile, new RegionRestriction(sequenceName))) {
        writeVariants(mFp, callsReader, fp);
      }
    }
    if (mTpBase != null && tpbase != null) {
      try (final VcfReader baselineReader = VcfReader.openVcfReader(mBaseLineFile, new RegionRestriction(sequenceName))) {
        writeVariants(mTpBase, baselineReader, tpbase);
      }
    }
    if (fn != null) {
      try (final VcfReader baselineReader = VcfReader.openVcfReader(mBaseLineFile, new RegionRestriction(sequenceName))) {
        writeVariants(mFn, baselineReader, fn);
      }
    }
    synchronized (mNames) {
      // We are done with a sequence so take it off the queue
      mNames.remove();
      mNames.notifyAll();
    }
  }

  /**
   * Increment the count of variants evaluated in all sequences across threads.
   * @param truePositives increase the count of true positives by this much
   * @param falsePositives increase the count of false positives by this much
   * @param falseNegatives increase the count of false negatives by this much
   */
  void addVariants(int truePositives, int falsePositives, int falseNegatives) {
    synchronized (mVariantLock) {
      mTruePositives += truePositives;
      mFalseNegatives += falseNegatives;
      mFalsePositives += falsePositives;
      Diagnostic.developerLog("Number of baseline variants processed: " + (mTruePositives + mFalseNegatives));
    }
  }

  int getUnphasable() {
    return mUnphasable;
  }

  int getMisPhasings() {
    return mMisPhasings;
  }
  int getCorrectPhasings() {
    return mCorrectPhasings;
  }
}
