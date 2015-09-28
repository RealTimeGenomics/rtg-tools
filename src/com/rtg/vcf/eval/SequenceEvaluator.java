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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.GlobalFlags;
import com.rtg.reader.SequencesReader;
import com.rtg.util.IORunnable;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.SequenceNameLocusSimple;

/**
 * Runs all the evaluation for a single reference sequence
 */
@TestClass({"com.rtg.vcf.eval.VcfEvalTaskTest", "com.rtg.vcf.eval.PhasingEvaluatorTest"})
class SequenceEvaluator implements IORunnable {

  private static final boolean DUMP_BEST_PATH = GlobalFlags.isSet(GlobalFlags.VCFEVAL_DUMP_BEST_PATH);

  private final EvalSynchronizer mSynchronize;
  private final SequencesReader mTemplate;
  private final Map<String, Long> mNameMap;
  private final Orientor mBaselineOrientor;
  private final Orientor mCallsOrientor;

  SequenceEvaluator(EvalSynchronizer variantSets, Map<String, Long> nameMap, SequencesReader template, Orientor baselineOrientor, Orientor callsOrientor) {
    mSynchronize = variantSets;
    mTemplate = template;
    mNameMap = nameMap;
    mBaselineOrientor = baselineOrientor;
    mCallsOrientor = callsOrientor;
  }

  @Override
  public void run() throws IOException {
    final Pair<String, Map<VariantSetType, List<Variant>>> setPair = mSynchronize.nextSet();
    if (setPair == null) {
      return;
    }
    final String currentName = setPair.getA();
    final Long sequenceId = mNameMap.get(currentName);
    if (sequenceId == null) {
      throw new NoTalkbackSlimException("Sequence " + currentName + " is not contained in the reference.");
    }
    final byte[] template = mTemplate.read(sequenceId);
    final Map<VariantSetType, List<Variant>> set = setPair.getB();
    final Collection<Variant> baseLineCalls = set.get(VariantSetType.BASELINE);
    final Collection<Variant> calledCalls = set.get(VariantSetType.CALLS);
    Diagnostic.developerLog("Sequence: " + currentName + " has " + baseLineCalls.size() + " baseline variants");
    Diagnostic.developerLog("Sequence: " + currentName + " has " + calledCalls.size() + " called variants");

    if (baseLineCalls.size() == 0 || calledCalls.size() == 0) {
      mSynchronize.write(currentName, baseLineCalls, calledCalls, Collections.<Integer>emptyList());
    } else {

      //find the best path for variant calls
      final PathFinder f = new PathFinder(template, currentName, calledCalls, baseLineCalls, PathFinder.getPathPreference(), mCallsOrientor, mBaselineOrientor);

      final Path best = f.bestPath();

      if (DUMP_BEST_PATH) {
        System.out.println("#### " + best);
        final List<Integer> syncPoints = best.getSyncPoints();
        final Range interesting = new SequenceNameLocusSimple(currentName, syncPoints.isEmpty() ? 0 : syncPoints.get(0), Math.max(best.mBaselinePath.getVariantEndPosition(), best.mCalledPath.getVariantEndPosition()) + 1);
        System.out.println("#### Template " + new Path(template).mBaselinePath.dumpHaplotypes(interesting));
        System.out.println("#### Baseline " + best.mBaselinePath.dumpHaplotypes(interesting));
        System.out.println("#### Call     " + best.mCalledPath.dumpHaplotypes(interesting));
      }

      Diagnostic.developerLog("Post-processing variant result lists for " + currentName + "...");
      final PathResult result = postProcess(best, baseLineCalls, calledCalls);

      final PhasingEvaluator.PhasingResult misPhasings = PhasingEvaluator.countMisphasings(best);
      mSynchronize.addPhasingCounts(misPhasings.mMisPhasings, misPhasings.mCorrectPhasings, misPhasings.mUnphaseable);

      Diagnostic.developerLog("Ready to write variants for " + currentName + "...");
      mSynchronize.write(currentName, result.mBaseline, result.mCalled, result.mSyncPoints);
    }
  }

  private static final class PathResult {
    final List<Integer> mSyncPoints;
    final List<VariantId> mBaseline;
    final List<VariantId> mCalled;

    public PathResult(List<Integer> syncPoints, List<VariantId> baseline, List<VariantId> called) {
      mSyncPoints = syncPoints;
      mBaseline = baseline;
      mCalled = called;
    }
  }

  static PathResult postProcess(Path best, Collection<Variant> baseLineCalls, Collection<Variant> calledCalls) {
    final List<OrientedVariant> truePositives = best.getCalledIncluded();
    final List<Variant> falsePositives = best.getCalledExcluded();
    final List<OrientedVariant> baselineTruePositives = best.getBaselineIncluded();
    final List<Variant> falseNegatives = best.getBaselineExcluded();

    Path.calculateWeights(best, truePositives, baselineTruePositives);

    final List<VariantId> baseline = mergeVariants(baseLineCalls, baselineTruePositives, falseNegatives);
    final List<VariantId> calls = mergeVariants(calledCalls, truePositives, falsePositives);
    return new PathResult(best.getSyncPoints(), baseline, calls);
  }

  private static List<VariantId> mergeVariants(Collection<Variant> allVariants, List<OrientedVariant> included, List<Variant> excluded) {
    List<VariantId> merged = new ArrayList<>(included.size() + excluded.size());
    merged.addAll(included);
    merged.addAll(excluded);
    // Sort by ID to ensure the ordering matches what is needed for variant writing and skipped variant insertion
    Collections.sort(merged, Variant.ID_COMPARATOR);
    // Merge any variants that were unable to be processed during path finding due to too-hard regions
    merged = insertSkipped(merged, allVariants);
    return merged;
  }

  private static List<VariantId> insertSkipped(List<VariantId> outVars, Collection<Variant> inVars) {
    assert isSortedById(inVars) : "inVars are not sorted";
    assert isSortedById(outVars) : " outVars are not sorted";
    final List<VariantId> result = new ArrayList<>(outVars.size());
    final Iterator<VariantId> outIt = outVars.iterator();
    VariantId outVar = null;
    for (Variant inVar : inVars) {
      if (outVar == null && outIt.hasNext()) {
        outVar = outIt.next();
      }
      if (outVar != null && outVar.getId() == inVar.getId()) {
        result.add(outVar);
        outVar = null;
      } else {
        result.add(new SkippedVariant(inVar));
      }
    }
    return result;
  }

  private static boolean isSortedById(Collection<? extends VariantId> c) {
    VariantId last = null;
    for (VariantId v : c) {
      if (last != null && v.getId() <= last.getId()) {
        return false;
      }
      last = v;
    }
    return true;
  }
}
