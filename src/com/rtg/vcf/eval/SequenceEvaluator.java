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
import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.reader.SequencesReader;
import com.rtg.util.Constants;
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

  private static final boolean DUMP_BEST_PATH = GlobalFlags.isSet(ToolsGlobalFlags.VCFEVAL_DUMP_BEST_PATH);
  private static final boolean SHORT_CIRCUIT_EMPTY = true; // Boolean.getBoolean("vcfeval.no.short-circuit-empty");

  private final EvalSynchronizer mSynchronize;
  private final SequencesReader mTemplate;
  private final Map<String, Long> mNameMap;
  private final List<Pair<Orientor, Orientor>> mOrientors;
  private final PathFinder.Config mPathFinderConfig = new PathFinder.Config();

  SequenceEvaluator(EvalSynchronizer variantSets, Map<String, Long> nameMap, SequencesReader template) {
    this(variantSets, nameMap, template, Collections.singletonList(new Pair<>(Orientor.UNPHASED, Orientor.UNPHASED)));
  }

  SequenceEvaluator(EvalSynchronizer variantSets, Map<String, Long> nameMap, SequencesReader template, List<Pair<Orientor, Orientor>> orientors) {
    assert orientors.size() == 1 || orientors.size() == 2;
    mSynchronize = variantSets;
    mTemplate = template;
    mNameMap = nameMap;
    mOrientors = orientors;
  }

  @Override
  public void run() throws IOException {
    try (SequencesReader tr = mTemplate.copy()) {
      final Pair<String, Map<VariantSetType, List<Variant>>> setPair = mSynchronize.nextSet();
      if (setPair == null) {
        return;
      }
      final String currentName = setPair.getA();
      final Long sequenceId = mNameMap.get(currentName);
      if (sequenceId == null) {
        throw new NoTalkbackSlimException("Sequence " + currentName + " is not contained in the reference.");
      }
      final byte[] template = tr.read(sequenceId);
      final Map<VariantSetType, List<Variant>> set = setPair.getB();
      final Collection<Variant> baseLineCalls = set.get(VariantSetType.BASELINE);
      final Collection<Variant> calledCalls = set.get(VariantSetType.CALLS);
      Diagnostic.developerLog("Sequence: " + currentName + " has " + baseLineCalls.size() + " baseline variants");
      Diagnostic.developerLog("Sequence: " + currentName + " has " + calledCalls.size() + " called variants");

      if (SHORT_CIRCUIT_EMPTY && (baseLineCalls.isEmpty() || calledCalls.isEmpty())) {
        setStatus(baseLineCalls, VariantId.STATUS_NO_MATCH);
        setStatus(calledCalls, VariantId.STATUS_NO_MATCH);
        mSynchronize.write(currentName, baseLineCalls, calledCalls, Collections.emptyList(), Collections.emptyList());
      } else {

        //find the best path for variant calls
        Pair<Orientor, Orientor> op = mOrientors.get(0);
        final PathFinder f = new PathFinder(template, currentName, baseLineCalls, calledCalls, op.getA(), op.getB(), mPathFinderConfig);

        final Path best = f.bestPath();
        if (best == null) {
          // Add some more info to the message so we can try to track this one down.
          final StringBuilder msg = new StringBuilder("After path finding on " + currentName + ", the best path was null!");
          msg.append("\nThis is an interesting situation -- please send example VCF files to " + Constants.SUPPORT_EMAIL_ADDR);
          if (baseLineCalls.size() < 50) {
            msg.append("\nb: ").append(baseLineCalls.toString());
          }
          if (calledCalls.size() < 50) {
            msg.append("\nc: ").append(calledCalls.toString());
          }
          throw new NullPointerException(msg.toString());
        }

        if (DUMP_BEST_PATH) {
          System.out.println("#### " + best);
          final List<Integer> syncPoints = best.getSyncPoints();
          final Range interesting = new SequenceNameLocusSimple(currentName, syncPoints.isEmpty() ? 0 : syncPoints.get(0), Math.max(best.mBaselinePath.getVariantEndPosition(), best.mCalledPath.getVariantEndPosition()) + 1);
          final HalfPath empty = new Path(op.getA().haplotypes(), template).mBaselinePath;
          System.out.println("#### Template " + empty.dumpHaplotypes(interesting));
          System.out.println("#### Baseline " + best.mBaselinePath.dumpHaplotypes(interesting));
          System.out.println("#### Call     " + best.mCalledPath.dumpHaplotypes(interesting));
        }

        final PhasingEvaluator.PhasingResult misPhasings = PhasingEvaluator.countMisphasings(best);
        mSynchronize.addPhasingCounts(misPhasings.mMisPhasings, misPhasings.mCorrectPhasings, misPhasings.mUnphaseable);

        final List<OrientedVariant> truePositives = best.getCalledIncluded();
        final List<OrientedVariant> baselineTruePositives = best.getBaselineIncluded();
        Path.calculateWeights(best, truePositives, baselineTruePositives);

        List<Variant> falsePositives = best.getCalledExcluded();
        List<Variant> falseNegatives = best.getBaselineExcluded();
        List<OrientedVariant> halfPositives = Collections.emptyList();
        List<OrientedVariant> baselineHalfPositives = Collections.emptyList();

        Path bestHap = null;
        if (mOrientors.size() == 2) {  // Run haploid pathfinding on the FP / FN to find common alleles
          op = mOrientors.get(1);
          final PathFinder f2 = new PathFinder(template, currentName, falseNegatives, falsePositives, op.getA(), op.getB(), mPathFinderConfig);
          bestHap = f2.bestPath();

          halfPositives = bestHap.getCalledIncluded();
          baselineHalfPositives = bestHap.getBaselineIncluded();
          Path.calculateWeights(bestHap, halfPositives, baselineHalfPositives);
          falsePositives = bestHap.getCalledExcluded();
          falseNegatives = bestHap.getBaselineExcluded();
        }

        Diagnostic.developerLog("Post-processing variant result lists for " + currentName);
        final List<VariantId> baseline = mergeVariants(baseLineCalls, baselineTruePositives, baselineHalfPositives, falseNegatives);
        final List<VariantId> calls = mergeVariants(calledCalls, truePositives, halfPositives, falsePositives);

        mSynchronize.write(currentName, baseline, calls, best.getSyncPoints(), bestHap != null ? bestHap.getSyncPoints() : Collections.emptyList());
      }
    }
  }

  private static List<VariantId> mergeVariants(Collection<Variant> allVariants, List<OrientedVariant> included, List<OrientedVariant> partial, List<Variant> excluded) {
    setStatus(included, VariantId.STATUS_GT_MATCH);
    setStatus(partial, VariantId.STATUS_ALLELE_MATCH);
    setStatus(excluded, VariantId.STATUS_NO_MATCH);
    final List<VariantId> merged = new ArrayList<>(included.size() + partial.size() + excluded.size());
    merged.addAll(included);
    merged.addAll(partial);
    merged.addAll(excluded);
    // Sort by ID to ensure the ordering matches what is needed for variant writing and skipped variant insertion
    merged.sort(Variant.ID_COMPARATOR);
    // Merge any variants that were unable to be processed during path finding due to too-hard regions
    return insertSkipped(merged, allVariants);
  }

  private static void setStatus(Collection<? extends VariantId> variants, byte status) {
    for (VariantId v : variants) {
      v.setStatus(status);
    }
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
        inVar.setStatus(VariantId.STATUS_SKIPPED);
        result.add(inVar);
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
