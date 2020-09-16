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

import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.BasicLinkedListNode;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Find the path through the two sequences of variants that best reconciles them.
 */
@TestClass("com.rtg.vcf.eval.PathTest")
public final class PathFinder {

  static final boolean TRACE = GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_TRACE);

  // Bundles up some configuration variables that can be re-used between runs of the path finding
  static final class Config {

    final PathPreference mPathSelector;
    final int mMaxComplexity;
    final int mMaxIterations;
    final boolean mFlagAlternates;
    final boolean mPruneNoOps;

    /**
     * Construct a default configuration
     */
    Config() {
      this(getPathPreference(),
        GlobalFlags.getIntegerValue(ToolsGlobalFlags.VCFEVAL_MAX_PATHS),
        GlobalFlags.getIntegerValue(ToolsGlobalFlags.VCFEVAL_MAX_ITERATIONS),
        GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_FLAG_ALTERNATES),
        GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_PRUNE_NO_OPS));
    }

    /**
     * Constructor using explicitly selected configuration
     * @param selector path selection criteria
     * @param maxComplexity threshold on number of concurrently active paths
     * @param maxIterations threshold on number of iterations since the last sync point
     * @param flagAlternates if set, mark variants matched in alternative paths
     * @param pruneNoOps if set, drop paths that contain obvious no-ops (where variants on one side are equivalent to zero variants on the other side)
     */
    Config(PathPreference selector, int maxComplexity, int maxIterations, boolean flagAlternates, boolean pruneNoOps) {
      mMaxComplexity = maxComplexity;
      mMaxIterations = maxIterations;
      mPathSelector = selector;
      mFlagAlternates = flagAlternates;
      mPruneNoOps = pruneNoOps;
    }

    private static PathPreference getPathPreference() {
      final String mode = GlobalFlags.getStringValue(ToolsGlobalFlags.VCFEVAL_MAXIMIZE_MODE); // What to maximize when comparing paths
      Diagnostic.developerLog("Path finder maximisation: " + mode);
      final PathPreference maximiseMode;
      switch (mode) {
        case "calls-min-base":
          maximiseMode = new MaxCallsMinBaseline();
          break;
        case "default":
        case "sum":
        default:
          maximiseMode = new MaxSumBoth();
      }
      return maximiseMode;
    }
  }

  private final Config mConfig;
  private final byte[] mTemplate;
  private final String mTemplateName;
  private final Variant[] mCalledVariants;
  private final Variant[] mBaseLineVariants;
  private final Orientor mBaselineOrientor;
  private final Orientor mCallOrientor;
  private int mCurrentMaxPos;

  <T extends Variant> PathFinder(byte[] template, String templateName, Collection<T> baseLineVariants, Collection<T> calledVariants, Orientor baselineOrientor, Orientor callOrientor, Config config) {
    if (baselineOrientor.haplotypes() != callOrientor.haplotypes()) {
      throw new IllegalArgumentException("Number of haplotypes produced by baseline and calls must match!"
        + " (" + baselineOrientor.haplotypes() + " != " + callOrientor.haplotypes() + ")");
    }
    mTemplate = template;
    mTemplateName = templateName;
    mConfig = config;
    mCallOrientor = callOrientor;
    mBaselineOrientor = baselineOrientor;
    mCalledVariants = calledVariants.toArray(new Variant[0]);
    Arrays.sort(mCalledVariants, Variant.NATURAL_COMPARATOR);

    mBaseLineVariants = baseLineVariants.toArray(new Variant[0]);
    Arrays.sort(mBaseLineVariants, Variant.NATURAL_COMPARATOR);
  }

  Path bestPath() {
    Diagnostic.developerLog("Starting path-finding with " + mBaselineOrientor.haplotypes() + " haplotypes on sequence " + mTemplateName + " using " + mBaselineOrientor + "," + mCallOrientor);
    // make it easy to find variants
    final TreeSet<Path> sortedPaths = new TreeSet<>();
    sortedPaths.add(new Path(mBaselineOrientor.haplotypes(), mTemplate));
    Path best = null;
    int maxPaths = 0;
    String maxPathsRegion = "";
    int currentIterations = 0;
    int currentMaxIterations = 0;
    int currentMax = 0;
    mCurrentMaxPos = 0;
    Path lastSyncPath = null;
    int lastSyncPos = 0;
    while (sortedPaths.size() > 0) {
      currentMax = Math.max(currentMax, sortedPaths.size());
      currentMaxIterations = Math.max(currentMaxIterations, currentIterations++);
      Path head = sortedPaths.pollFirst();
      if (TRACE) {
        System.err.println("Size: " + (sortedPaths.size() + 1) + " Range:" + (lastSyncPos + 1) + "-" + (mCurrentMaxPos + 1) + " LocalIterations: " + currentIterations + " " + mConfig.mMaxComplexity + "\n");
        System.err.println("Head is " + head);
      }
      if (sortedPaths.size() == 0) { // Only one path currently in play
        final int currentSyncPos = head.mCalledPath.getPosition();
        if (currentMax > maxPaths) {
          maxPathsRegion = mTemplateName + ":" + (lastSyncPos + 1) + "-" + (currentSyncPos + 1);
          maxPaths = currentMax;
          Diagnostic.developerLog("Maximum path complexity now " + maxPaths + ", at " + maxPathsRegion + " with "  + currentIterations + " iterations");
        }
        currentMax = 0;
        currentIterations = 0;
        lastSyncPos = currentSyncPos;
        lastSyncPath = head;
      } else if (sortedPaths.size() > mConfig.mMaxComplexity || currentIterations > mConfig.mMaxIterations) {
        Diagnostic.warning("Evaluation too complex (" + sortedPaths.size() + " unresolved paths, " + currentIterations + " iterations) at reference region " + mTemplateName + ":" + (lastSyncPos + 1) + "-" + (mCurrentMaxPos + 2) + ". Variants in this region will not be included in results.");
        sortedPaths.clear();    // Drop all paths currently in play
        currentIterations = 0;
        head = lastSyncPath;    // Create new head containing path up until last sync point
        skipVariantsTo(head.mCalledPath, mCalledVariants, mCurrentMaxPos + 1);
        skipVariantsTo(head.mBaselinePath, mBaseLineVariants, mCurrentMaxPos + 1);
      }
      if (enqueueVariant(sortedPaths, head, true)) {
        continue;
      }
      if (enqueueVariant(sortedPaths, head, false)) {
        continue;
      }

      head.step();
      if (TRACE) {
        System.err.println("Stepped " + head);
      }

      if (head.inSync()) {
        if (prune(head)) {
          continue;
        }
        skipToNextVariant(head);
        if (TRACE) {
          System.err.println("Skipped " + head);
        }
      }

      if (head.matches()) {
        if (head.finished()) {
          if (prune(head)) {
            continue;
          }
          if (TRACE) {
            System.err.println("Finish  " + head);
          }
          final Path finished = new Path(head, new BasicLinkedListNode<>(head.mCalledPath.getPosition(), head.mSyncPointList));
          best = best == null ? finished : mConfig.mPathSelector.better(best, finished);
        } else {
          addIfBetter(head, sortedPaths);
        }
      } else {
        if (TRACE) {
          System.err.println("Head mismatch, discard");
        }
      }
    }
    //System.err.println("Best: " + best);
    Diagnostic.userLog("Reference " + mTemplateName + " had maximum path complexity of " + maxPaths + " at " + maxPathsRegion);
    return best;
  }

  private boolean prune(Path head) {
    assert head.matches();
    if (mConfig.mPruneNoOps && head.hasNoOp()) {
      Diagnostic.developerLog("Discard no-op path with (" + head.mBSinceSync + "," + head.mCSinceSync + ") at " + head.mCalledPath.getPosition());
      if (TRACE) {
        System.err.println("Pruned  " + head);
      }
      return true;
    }
    if (mConfig.mFlagAlternates) {
      head.flagIncluded();
    }
    return false;
  }

  private boolean enqueueVariant(TreeSet<Path> paths, Path head, boolean side) {
    final Variant[] variants = side ? mCalledVariants : mBaseLineVariants;
    final HalfPath halfPath = side ? head.mCalledPath : head.mBaselinePath;
    final int aVarIndex = nextVariant(halfPath, variants);
    if (aVarIndex != -1) {
      final Variant aVar = variants[aVarIndex];
      mCurrentMaxPos = Math.max(mCurrentMaxPos, aVar.getStart());
      //Adding a new variant to side
      if (TRACE) {
        System.err.println("Add alternatives to " + (side ? "called " : "baseline ") + aVar);
      }
      final Orientor orientor = side ? mCallOrientor : mBaselineOrientor;
      addIfBetter(head.addVariant(side, aVar, aVarIndex, orientor), paths);
      return true;
    }
    return false;
  }

  /**
   * Move the half path to the specified position, ignoring any intervening variants.
   * @param path the half path to skip
   * @param variants all variants
   * @param maxPos reference position to move to.
   */
  private void skipVariantsTo(HalfPath path, Variant[] variants, int maxPos) {
    int varIndex = path.getVariantIndex();
    while (varIndex < variants.length && (varIndex == -1 || variants[varIndex].getStart() < maxPos)) {
      ++varIndex;
    }
    --varIndex;
    Diagnostic.developerLog("Skipped to maxPos: " + maxPos + ". Variant index " + path.getVariantIndex() + " -> " + varIndex);
    path.setVariantIndex(varIndex);
    path.moveForward(Math.min(maxPos, mTemplate.length - 1));
  }


  /**
   * Move the path to just before the next variant for either side
   * @param head the path to skip forward
   */
  private void skipToNextVariant(final Path head) {
    final int aNext = futureVariantPos(head.mCalledPath, mCalledVariants);
    final int bNext = futureVariantPos(head.mBaselinePath, mBaseLineVariants);
    final int lastTemplatePos = mTemplate.length - 1;
    //System.err.println("Skip to next variant " + lastTemplatePos);
    // -1 because we want to be before the position
    final int nextPos = Math.min(Math.min(aNext, bNext), lastTemplatePos) - 1;
    assert head.mCalledPath.getPosition() == head.mBaselinePath.getPosition();
    if (nextPos > head.mCalledPath.getPosition()) {
      head.moveForward(nextPos);
    }
  }

  /* Gets the next upstream variant position */
  int futureVariantPos(HalfPath path, Variant[] variants) {
    final int nextIdx = path.getVariantIndex() + 1;
    if (nextIdx >= variants.length) {
      return mTemplate.length - 1;
    } else {
      return variants[nextIdx].getStart();
    }
  }

  /* Gets the index of the next variant if it should be enqueued to the supplied HalfPath at the current position,
   or -1 if there is none to be enqueued at the current position */
  static int nextVariant(HalfPath path, Variant[] variants) {
    final int nextIdx = path.getVariantIndex() + 1;
    if (nextIdx >= variants.length) {
      return -1;
    }
    final Variant nextVar = variants[nextIdx];
    if (nextVar.getStart() <= path.getPosition() + 1) {
      return nextIdx;
    }
    if (path.wantsFutureVariantBases() && nextVar.getStart() <= path.getVariantEndPosition()) {
      return nextIdx;
    }
    return -1;
  }

  private void addIfBetter(Collection<Path> add, TreeSet<Path> sortedPaths) {
    for (final Path p : add) {
      addIfBetter(p, sortedPaths);
    }
  }

  private void addIfBetter(Path add, TreeSet<Path> sortedPaths) {
    if (TRACE) {
      System.err.println("Adding  " + add);
    }
    if (sortedPaths.contains(add)) {
      final Path other = sortedPaths.floor(add);
      final Path best = mConfig.mPathSelector.better(add, other);
      if (best == add) {
        sortedPaths.remove(other);
        sortedPaths.add(best);
        if (TRACE) {
          System.err.println("Replace " + other);
        }
        if (mConfig.mFlagAlternates) {
          if (!other.inSync()) {
            add.linkEquivalent(other);
          }
        }
      } else {
        if (TRACE) {
          System.err.println("Prefer  " + other);
        }
        if (mConfig.mFlagAlternates) {
          if (!add.inSync()) {
            other.linkEquivalent(add);
          }
        }
      }
    } else {
      sortedPaths.add(add);
    }
  }
}
