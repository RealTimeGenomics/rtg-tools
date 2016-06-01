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

  private static final boolean TRACE = false;

  private static final int MAX_COMPLEXITY = GlobalFlags.getIntegerValue(ToolsGlobalFlags.VCFEVAL_MAX_PATHS); // Threshold on number of unresolved paths
  private static final int MAX_ITERATIONS = GlobalFlags.getIntegerValue(ToolsGlobalFlags.VCFEVAL_MAX_ITERATIONS);  // Threshold on number of iterations since last sync point

  private final byte[] mTemplate;
  private final String mTemplateName;
  private final Variant[] mCalledVariants;
  private final Variant[] mBaseLineVariants;
  private final PathPreference mMaximizeMode;
  private final Orientor mBaselineOrientor;
  private final Orientor mCallOrientor;
  private int mCurrentMaxPos;

  <T extends Variant> PathFinder(byte[] template, String templateName, Collection<T> baseLineVariants, Collection<T> calledVariants, Orientor baselineOrientor, Orientor callOrientor, PathPreference maximizeMode) {
    mTemplate = template;
    mTemplateName = templateName;
    mMaximizeMode = maximizeMode;
    mCallOrientor = callOrientor;
    mBaselineOrientor = baselineOrientor;
    mCalledVariants = calledVariants.toArray(new Variant[calledVariants.size()]);
    Arrays.sort(mCalledVariants, Variant.NATURAL_COMPARATOR);

    mBaseLineVariants = baseLineVariants.toArray(new Variant[baseLineVariants.size()]);
    Arrays.sort(mBaseLineVariants, Variant.NATURAL_COMPARATOR);
  }

  /**
   * Find the path through the two sequences of variants that best reconciles
   * them.
   *
   * @param template original reference sequence.
   * @param templateName name of the current reference sequence
   * @param calledVariants first sequence of variants to be applied to the template.
   * @param baseLineVariants second sequence of variants to be applied to the template.
   * @param <T> the type parameter
   * @return the best path (non-null).
   */
  public static <T extends Variant> Path bestPath(byte[] template, String templateName, Collection<T> calledVariants, Collection<T> baseLineVariants) {
    return new PathFinder(template, templateName, baseLineVariants, calledVariants, Orientor.UNPHASED, Orientor.UNPHASED, getPathPreference()).bestPath();
  }

  static PathPreference getPathPreference() {
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
        maximiseMode = new SumBoth();
    }
    return maximiseMode;
  }

  Path bestPath() {
    Diagnostic.developerLog("Starting path-finding for " + mTemplateName + " using " + mBaselineOrientor + "," + mCallOrientor);
    // make it easy to find variants
    final TreeSet<Path> sortedPaths = new TreeSet<>();
    sortedPaths.add(new Path(mTemplate));
    Path best = null;
    int maxPaths = 0;
    String maxPathsRegion = "";
    int currentIterations = 0;
    int currentMaxIterations = 0;
    int currentMax = 0;
    mCurrentMaxPos = 0;
    Path lastSyncPath = null;
    int lastSyncPos = 0;
    String lastWarnMessage = null;
    while (sortedPaths.size() > 0) {
      currentMax = Math.max(currentMax, sortedPaths.size());
      currentMaxIterations = Math.max(currentMaxIterations, currentIterations++);
      Path head = sortedPaths.pollFirst();
      if (TRACE) {
        System.err.println("Size: " + (sortedPaths.size() + 1) + " Range:" + (lastSyncPos + 1) + "-" + (mCurrentMaxPos + 1) + " LocalIterations: " + currentIterations + "\n\nHead: " + head);
      }
      if (sortedPaths.size() == 0) { // Only one path currently in play
        if (lastWarnMessage != null) { // Issue a warning if we encountered problems during the previous region
          Diagnostic.warning(lastWarnMessage);
          lastWarnMessage = null;
        }
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
      } else if (sortedPaths.size() > MAX_COMPLEXITY || currentIterations > MAX_ITERATIONS) {
        lastWarnMessage = "Evaluation too complex (" + sortedPaths.size() + " unresolved paths, " + currentIterations + " iterations) at reference region " + mTemplateName + ":" + (lastSyncPos + 1) + "-" + (mCurrentMaxPos + 2) + ". Variants in this region will not be included in results.";
        sortedPaths.clear();    // Drop all paths currently in play
        currentIterations = 0;
        head = lastSyncPath;    // Create new head containing path up until last sync point
        skipVariantsTo(head.mCalledPath, mCalledVariants, mCurrentMaxPos + 1);
        skipVariantsTo(head.mBaselinePath, mBaseLineVariants, mCurrentMaxPos + 1);
      }
      if (head.finished()) {
        // Path is done. Remember the best
        final BasicLinkedListNode<Integer> syncPoints = new BasicLinkedListNode<>(head.mCalledPath.getPosition(), head.mSyncPointList);
        best = mMaximizeMode.better(best, new Path(head, syncPoints));
        continue;
      }
      if (enqueueVariant(sortedPaths, head, true)) {
        continue;
      }
      if (enqueueVariant(sortedPaths, head, false)) {
        continue;
      }

      head.step();

      if (head.inSync()) {
        skipToNextVariant(head);
        if (TRACE) {
          System.err.println("In sync, skipping: " + head);
        }
      } else {
        if (TRACE) {
          System.err.println("Not in sync");
        }
      }

      if (head.matches()) {
        if (TRACE) {
          System.err.println("Head matches, keeping");
        }
        addIfBetter(head, sortedPaths);
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

  private boolean enqueueVariant(TreeSet<Path> paths, Path head, boolean side) {
    final Variant[] variants = side ? mCalledVariants : mBaseLineVariants;
    final HalfPath halfPath = side ? head.mCalledPath : head.mBaselinePath;
    final Orientor orientor = side ? mCallOrientor : mBaselineOrientor;
    final int aVarIndex = nextVariant(halfPath, variants);
    if (aVarIndex != -1) {
      final Variant aVar = variants[aVarIndex];
      mCurrentMaxPos = Math.max(mCurrentMaxPos, aVar.getStart());
      //Adding a new variant to side
      if (TRACE) {
        System.err.println("Add alternatives to " + (side ? "called " : "baseline ") + aVar);
      }
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
      varIndex++;
    }
    varIndex--;
    Diagnostic.developerLog("Skipped to maxPos: " + maxPos + ". Variant index " + path.getVariantIndex() + " -> " + varIndex);
    path.setVariantIndex(varIndex);
    path.moveForward(maxPos);
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
    if (sortedPaths.contains(add)) {
      final Path other = sortedPaths.floor(add);
      final Path best = mMaximizeMode.better(add, other);
      if (best == add) {
        sortedPaths.remove(other);
        sortedPaths.add(best);
      }
    } else {
      sortedPaths.add(add);
    }
  }

  interface PathPreference {
    Path better(Path first, Path second);
  }

  static final class SumBoth implements PathPreference {
    private static boolean hasNoOp(Path path) {
      return (path.mBSinceSync == 0 && path.mCSinceSync > 0)
        || (path.mCSinceSync == 0 && path.mBSinceSync > 0);
    }

    @Override
    public Path better(Path first, Path second) {
      // See if we have obvious no-ops we would rather drop
      final boolean fSync = first != null && (first.inSync() || first.finished());
      final boolean sSync = second != null && (second.inSync() || second.finished());
      if (fSync && sSync) { // See if we have no-ops we would rather drop
        if (hasNoOp(first)) {
          Diagnostic.developerLog("Discard no-op path with (" + first.mBSinceSync + "," + first.mCSinceSync + ") at " + first.mCalledPath.getPosition());
          return second;
        } else if (hasNoOp(second)) {
          Diagnostic.developerLog("Discard no-op path with (" + second.mBSinceSync + "," + second.mCSinceSync + ") at " + first.mCalledPath.getPosition());
          return first;
        }
      }

      // Prefer paths that maximise total number of included variants (baseline + called)
      BasicLinkedListNode<OrientedVariant> firstIncluded = first == null ? null : first.mCalledPath.getIncluded();
      BasicLinkedListNode<OrientedVariant> secondIncluded = second == null ? null : second.mCalledPath.getIncluded();
      int firstSize = firstIncluded == null ? 0 : firstIncluded.size();
      int secondSize = secondIncluded == null ? 0 : secondIncluded.size();
      firstIncluded = first == null ? null : first.mBaselinePath.getIncluded();
      secondIncluded = second == null ? null : second.mBaselinePath.getIncluded();
      firstSize += firstIncluded == null ? 0 : firstIncluded.size();
      secondSize += secondIncluded == null ? 0 : secondIncluded.size();
      if (firstSize == secondSize) {
        // Tie break equivalently scoring paths for greater aesthetics
        if (firstIncluded != null && secondIncluded != null) {

          // Prefer solutions that minimize discrepencies between baseline and call counts since last sync point
          final int fDelta = Math.abs(first.mBSinceSync - first.mCSinceSync);
          final int sDelta = Math.abs(second.mBSinceSync - second.mCSinceSync);
          if (fDelta != sDelta) {
            return fDelta < sDelta ? first : second;
          }

          // Prefer solutions that sync more regularly (more likely to be "simpler")
          final int syncDelta = (first.mSyncPointList == null ? 0 : first.mSyncPointList.getValue()) - (second.mSyncPointList == null ? 0 : second.mSyncPointList.getValue());
          if (syncDelta != 0) {
            return syncDelta > 0 ? first : second;
          }

        /*
        Diagnostic.developerLog("Sum: Remaining break at: " + first.mBaselinePath.getPosition() + " sum = " + firstSize
            + " first = (" + first.mBSinceSync + "," + first.mCSinceSync + ")"
            + " second = (" + second.mBSinceSync + "," + second.mCSinceSync + ")"
            + " " + first.inSync() + "," + second.inSync() + " syncdelta=" + syncDelta
        );
        */

          // At this point break ties arbitrarily based on allele ordering
          return (firstIncluded.getValue().alleleId() < secondIncluded.getValue().alleleId()) ? first : second;
        }
      }
      return firstSize > secondSize ? first : second;
    }
  }

  @TestClass("com.rtg.vcf.eval.AlleleAccumulatorTest")
  static final class MaxCallsMinBaseline implements PathPreference {
    @Override
    public Path better(Path first, Path second) {
      // Prefer paths that maximise the number of called variants included
      // Better for integrating a sample into population alleles
      BasicLinkedListNode<OrientedVariant> firstIncluded = first == null ? null : first.mCalledPath.getIncluded();
      BasicLinkedListNode<OrientedVariant> secondIncluded = second == null ? null : second.mCalledPath.getIncluded();
      int firstSize = firstIncluded == null ? 0 : firstIncluded.size();
      int secondSize = secondIncluded == null ? 0 : secondIncluded.size();
      if (firstSize == secondSize) {
        firstIncluded = first == null ? null : first.mBaselinePath.getIncluded();
        secondIncluded = second == null ? null : second.mBaselinePath.getIncluded();
        firstSize = firstIncluded == null ? 0 : firstIncluded.size();
        secondSize = secondIncluded == null ? 0 : secondIncluded.size();

        if (firstSize == secondSize) {
          // At this point try to break ties arbitrarily based on allele ordering
          if (firstIncluded != null && secondIncluded != null) {
            return (firstIncluded.getValue().alleleId() < secondIncluded.getValue().alleleId()) ? first : second;
          }
        }
        return firstSize < secondSize ? first : second;
      }
      return firstSize > secondSize ? first : second;
    }
  }
}
