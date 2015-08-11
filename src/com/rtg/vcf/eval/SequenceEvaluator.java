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
import com.rtg.reader.SequencesReader;
import com.rtg.util.IORunnable;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.IntervalComparator;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.SequenceNameLocusSimple;

/**
 * Runs all the evaluation for a single reference sequence
 */
@TestClass({"com.rtg.vcf.eval.VcfEvalTaskTest", "com.rtg.vcf.eval.PhasingEvaluatorTest"})
class SequenceEvaluator implements IORunnable {

  private static final boolean DUMP = Boolean.getBoolean("dump-haplotypes");

  private final EvalSynchronizer mSynchronize;
  private final SequencesReader mTemplate;
  private final Map<String, Long> mNameMap;

  SequenceEvaluator(EvalSynchronizer variantSets, Map<String, Long> nameMap, SequencesReader template) {
    mSynchronize = variantSets;
    mTemplate = template;
    mNameMap = nameMap;
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
      mSynchronize.write(currentName, baseLineCalls, calledCalls);
    } else {
      //find the best path for variant calls
      final Path best = PathFinder.bestPath(template, currentName, calledCalls, baseLineCalls);

      if (DUMP) {
        System.out.println("#### " + best);
        final Range interesting = new SequenceNameLocusSimple(currentName, best.getSyncPoints().get(0).getPos(), Math.max(best.mBaselinePath.getVariantEndPosition(), best.mCalledPath.getVariantEndPosition()));
        System.out.println("#### Template " + new Path(template).mBaselinePath.dumpHaplotypes(interesting));
        System.out.println("#### Baseline " + best.mBaselinePath.dumpHaplotypes(interesting));
        System.out.println("#### Call     " + best.mCalledPath.dumpHaplotypes(interesting));
      }

      // A Bunch of postprocessing that could probably be done in bestPath
      List<OrientedVariant> truePositives = best.getCalledIncluded();
      final List<OrientedVariant> baselineTruePositives = best.getBaselineIncluded();
      final List<Variant> falsePositives = best.getCalledExcluded();
      final List<Variant> falseNegatives = best.getBaselineExcluded();

      final Pair<List<OrientedVariant>, List<OrientedVariant>> newcalls = Path.calculateWeights(best, truePositives, baselineTruePositives);
      // this step is currently necessary as sometimes you can (rarely) have variants included in the best path
      // but they do not correspond to any variant in baseline. // E.g. two variants which when both replayed cancel each other out.
      truePositives = newcalls.getA();
      merge(falsePositives, newcalls.getB());

      Diagnostic.developerLog("Merging variant result lists for " + currentName + "...");
      List<VariantId> baseline = new ArrayList<>(baselineTruePositives.size() + falseNegatives.size());
      baseline.addAll(baselineTruePositives);
      baseline.addAll(falseNegatives);
      List<VariantId> calls = new ArrayList<>(truePositives.size() + falsePositives.size());
      calls.addAll(truePositives);
      calls.addAll(falsePositives);
      // Sort by ID to ensure the ordering matches what is needed for variant writing
      Collections.sort(baseline, Variant.ID_COMPARATOR);
      Collections.sort(calls, Variant.ID_COMPARATOR);

      // Merge any variants that were unable to be processed during path finding due to too-hard regions
      Diagnostic.developerLog("Checking for variants from skipped regions for " + currentName + "...");
      baseline = insertSkipped(baseline, baseLineCalls);
      calls = insertSkipped(calls, calledCalls);

      final PhasingEvaluator.PhasingResult misPhasings = PhasingEvaluator.countMisphasings(best);
      mSynchronize.addPhasingCounts(misPhasings.mMisPhasings, misPhasings.mCorrectPhasings, misPhasings.mUnphaseable);

      Diagnostic.developerLog("Ready to write variants for " + currentName + "...");
      mSynchronize.write(currentName, baseline, calls);
    }
  }

  private boolean isSortedById(Collection<? extends VariantId> c) {
    VariantId last = null;
    for (VariantId v : c) {
      if (last != null && v.getId() <= last.getId()) {
        return false;
      }
      last = v;
    }
    return true;
  }

  private List<VariantId> insertSkipped(List<VariantId> outVars, Collection<Variant> inVars) {
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

  private void merge(List<Variant> falsePositives, List<OrientedVariant> b) {
    if (b.size() == 0) {
      return;
    }
    for (OrientedVariant v : b) {
      falsePositives.add(v.variant());
    }
    Collections.sort(falsePositives, new IntervalComparator());

  }
}
