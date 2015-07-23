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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.reader.SequencesReader;
import com.rtg.util.IORunnable;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.intervals.IntervalComparator;

/**
 * Runs all the evaluation for a single reference sequence
 */
@TestClass({"com.rtg.vcf.eval.VcfEvalTaskTest", "com.rtg.vcf.eval.PhasingEvaluatorTest"})
class SequenceEvaluator implements IORunnable {

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
    final Pair<String, Map<VariantSetType, List<DetectedVariant>>> setPair = mSynchronize.nextSet();
    if (setPair == null) {
      return;
    }
    final String currentName = setPair.getA();
    final Long sequenceId = mNameMap.get(currentName);
    if (sequenceId == null) {
      throw new NoTalkbackSlimException("Sequence " + currentName + " is not contained in the reference.");
    }
    final byte[] template = mTemplate.read(sequenceId);

    final Map<VariantSetType, List<DetectedVariant>> set = setPair.getB();

    final Collection<DetectedVariant> baseLineCalls = set.get(VariantSetType.BASELINE);
    final Collection<DetectedVariant> calledCalls = set.get(VariantSetType.CALLS);

    if (baseLineCalls == null || baseLineCalls.size() == 0) {
      mSynchronize.write(currentName, null, calledCalls, null, null);
      if (calledCalls != null) {
        Diagnostic.developerLog("Number of called variants: " + calledCalls.size());

        for (final DetectedVariant v : calledCalls) {
          mSynchronize.addRocLine(new RocLine(v.getSequenceName(), v.getStart(), v.getSortValue(), 0.0, false), v);
        }
      }
    } else if (calledCalls == null || calledCalls.size() == 0) {
      Diagnostic.developerLog("Number of baseline variants: " + baseLineCalls.size());
      mSynchronize.write(currentName, null, null, baseLineCalls, null);
    } else {

      Diagnostic.developerLog("Sequence: " + currentName + " has " + baseLineCalls.size() + " baseline variants");
      Diagnostic.developerLog("Sequence: " + currentName + " has " + calledCalls.size() + " called variants");

      //find the best path for variant calls
      final Path best = PathFinder.bestPath(template, currentName, calledCalls, baseLineCalls);

      //System.out.println(path);
      List<OrientedVariant> truePositives = best.getCalledIncluded();
      final List<OrientedVariant> baselineTruePositives = best.getBaselineIncluded();
      final List<Variant> falsePositives = best.getCalledExcluded();
      final List<Variant> falseNegatives = best.getBaselineExcluded();

      final Pair<List<OrientedVariant>, List<OrientedVariant>> calls = Path.calculateWeights(best, truePositives, baselineTruePositives);
      // this step is currently necessary as sometimes you can (rarely) have variants included in the best path
      // but they do not correspond to any variant in baseline. // E.g. two variants which when both replayed cancel each other out.
      truePositives = calls.getA();
      merge(falsePositives, calls.getB());

      mSynchronize.addVariants(baselineTruePositives.size(), falsePositives.size(), falseNegatives.size());
      Diagnostic.developerLog("Writing variants...");

      final PhasingEvaluator.PhasingResult misPhasings = PhasingEvaluator.countMisphasings(best);
      mSynchronize.addPhasings(misPhasings.mMisPhasings, misPhasings.mCorrectPhasings, misPhasings.mUnphaseable);

      mSynchronize.write(currentName, truePositives, falsePositives, falseNegatives, baselineTruePositives);
      Diagnostic.developerLog("Generating ROC data...");

      double tpTotal = 0.0;
      for (final OrientedVariant v : truePositives) {
        final DetectedVariant dv = (DetectedVariant) v.variant();
        mSynchronize.addRocLine(new RocLine(dv.getSequenceName(), dv.getStart(), dv.getSortValue(), v.getWeight(), true), dv);
        tpTotal += v.getWeight();
      }
      if (tpTotal - baselineTruePositives.size() > 0.001) {
        throw new SlimException("true positives does not match baseline number tp weighted= " + tpTotal + " baseline = " + baselineTruePositives.size());
      }
      for (final Variant v : falsePositives) {
        final DetectedVariant dv = (DetectedVariant) (v instanceof OrientedVariant ? ((OrientedVariant) v).variant() : v);
        // System.out.println(dv);
        mSynchronize.addRocLine(new RocLine(dv.getSequenceName(), dv.getStart(), dv.getSortValue(), 0.0, false), dv);
      }
    }
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
