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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.rtg.util.BasicLinkedListNode;
import com.rtg.util.Pair;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Record a path that reconciles two sequences of variants. Also provides
 * method to find a best path.
 */
public final class Path implements Comparable<Path> {

  final HalfPath mCalledPath;
  final HalfPath mBaselinePath;
  final BasicLinkedListNode<Integer> mSyncPointList;
  int mCSinceSync;
  int mBSinceSync;

  static class SyncPoint implements Comparable<SyncPoint> {
    private final int mPos;
    private final double mCalledTPCount;
    private final double mBaselineTPCount;

    public SyncPoint(int pos, int calledCounts, int baselineCounts) {
      mPos = pos;
      mCalledTPCount = calledCounts;
      mBaselineTPCount = baselineCounts;
    }

    int getPos() {
      return mPos;
    }

    @Override
    public String toString() {
      return "SyncPoint [mPos=" + (mPos + 1) + ", mCalledTPCount=" + mCalledTPCount + ", mBaselineTPCount=" + mBaselineTPCount + "]";
    }

    @Override
    public int hashCode() {
      return mPos;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof SyncPoint && compareTo((SyncPoint) obj) == 0;
    }

    @Override
    public int compareTo(SyncPoint o) {
      return Integer.compare(mPos, o.mPos);
    }
  }

  /**
   * Construct a path object for the specified template
   *
   * @param template the template sequence to build a path for.
   */
  Path(byte[] template) {
    mCalledPath = new HalfPath(template);
    mBaselinePath = new HalfPath(template);
    mSyncPointList = null;
  }


  Path(Path parent, BasicLinkedListNode<Integer> syncPoints) {
    mCalledPath = new HalfPath(parent.mCalledPath);
    mBaselinePath = new HalfPath(parent.mBaselinePath);
    mSyncPointList = syncPoints;
    mCSinceSync = parent.mCSinceSync;
    mBSinceSync = parent.mBSinceSync;
  }

  /**
   * @return true if the path has finished
   */
  public boolean finished() {
    return mCalledPath.finished() && mBaselinePath.finished();
  }

  /**
   * @return true if all positions are the same
   */
  public boolean inSync() {
    if (mCalledPath.compareHaplotypePositions() != 0) {
      return false;
    }
    if (mBaselinePath.compareHaplotypePositions() != 0) {
      return false;
    }
    if (mCalledPath.getPosition() != mBaselinePath.getPosition()) {
      return false;
    }
    if (mCalledPath.getPosition() < mCalledPath.getVariantEndPosition()) {
      return false;
    }
    if (mBaselinePath.getPosition() < mBaselinePath.getVariantEndPosition()) {
      return false;
    }
    if (!mCalledPath.isOnTemplate()) {
      return false;
    }
    if (!mBaselinePath.isOnTemplate()) {
      return false;
    }
    return true;
  }

  /**
   * @return the set of variants included on call side
   */
  public List<OrientedVariant> getCalledIncluded() {
    return toReversedList(mCalledPath.getIncluded());
  }

  /**
   * @return the set of variants excluded on call side
   */
  public List<Variant> getCalledExcluded() {
    return toReversedList(mCalledPath.getExcluded());
  }

  /**
   * @return the set of variants included on baseline side
   */
  public List<OrientedVariant> getBaselineIncluded() {
    return toReversedList(mBaselinePath.getIncluded());
  }

  /**
   * @return the set of variants excluded on baseline side
   */
  public List<Variant> getBaselineExcluded() {
    return toReversedList(mBaselinePath.getExcluded());
  }

  private static <T> List<T> toReversedList(BasicLinkedListNode<T> input) {
    if (input == null) {
      return new ArrayList<>();
    }
    final List<T> list = new ArrayList<>(input.size());
    for (final T orientedVariant : input) {
      list.add(orientedVariant);
    }
    Collections.reverse(list);
    return list;
  }

  void include(boolean side, OrientedVariant var, int varIndex) {
    if (side) {
      mCalledPath.include(var, varIndex);
      mCSinceSync++;
    } else {
      mBaselinePath.include(var, varIndex);
      mBSinceSync++;
    }
  }

  void exclude(boolean side, Variant var, int varIndex) {
    if (side) {
      mCalledPath.exclude(var, varIndex);
    } else {
      mBaselinePath.exclude(var, varIndex);
    }
  }

  Collection<Path> addVariant(boolean side, Variant var, int varIndex, Orientor orientor) {
    final ArrayList<Path> paths = new ArrayList<>();
    final BasicLinkedListNode<Integer> syncPoints;
    if (this.inSync()) {
      syncPoints = new BasicLinkedListNode<>(mCalledPath.getPosition(), mSyncPointList);
      mCSinceSync = 0;
      mBSinceSync = 0;
    } else {
      syncPoints = mSyncPointList;
    }

    // Create a path extension that excludes this variant
    final Path exclude = new Path(this, syncPoints);
    exclude.exclude(side, var, varIndex);
    paths.add(exclude);

    // Create a path extension that includes this variant in the possible phases
    for (OrientedVariant o : orientor.orientations(var)) {
      if ((side ? mCalledPath : mBaselinePath).isNew(o)) {
        final Path include = new Path(this, syncPoints);
        include.include(side, o, varIndex);
        paths.add(include);
      }
    }

    return paths;
  }

  void moveForward(int position) {
    mCalledPath.moveForward(position);
    mBaselinePath.moveForward(position);
  }

  void step() {
    if (mCalledPath.compareHaplotypePositions() > 0) {
      // make B haplotype catch up to A
      mCalledPath.haplotypeBStep();
      mBaselinePath.haplotypeBStep();
    } else if (mCalledPath.compareHaplotypePositions() < 0) {
      // make A haplotype catch up to B
      mCalledPath.haplotypeAStep();
      mBaselinePath.haplotypeAStep();
    } else {
      // step both
      mCalledPath.step();
      mBaselinePath.step();
    }
  }

  boolean matches() {
    return mCalledPath.matches(mBaselinePath);
  }

  @Override
  public int compareTo(Path o) {
    final int aComp = mCalledPath.compareTo(o.mCalledPath);
    if (aComp != 0) {
      return aComp;
    }
    return mBaselinePath.compareTo(o.mBaselinePath);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Path && compareTo((Path) o) == 0;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mCalledPath.hashCode(), mBaselinePath.hashCode());
  }

  @Override
  public String toString() {
    return "Path:" + StringUtils.LS + "baseline=" + mBaselinePath + "called=" + mCalledPath + "syncpoints(0-based)=" + HalfPath.listToString(this.mSyncPointList);
  }

  /**
   * Get the list of sync points for the path. Sync points are created when the path is in sync,
   * at the reference position immediately before where a new variant is to be added.
   * @return the sync point list
   */
  public List<Integer> getSyncPoints() {
    final List<Integer> result = new ArrayList<>();
    for (final Integer syncpoint : mSyncPointList) {
      result.add(syncpoint);
    }
    Collections.reverse(result);
    return result;
  }

  // Counts the baseline and called variants between each sync point
  private static List<SyncPoint> getSyncPointsList(final List<Integer> syncpoints, final List<OrientedVariant> baseLine, final List<OrientedVariant> called) {
    final List<SyncPoint> list = new ArrayList<>();
    int basePos = 0;
    int callPos = 0;
    for (int loc : syncpoints) {
      int baseLineCount = 0;
      int calledCount = 0;
      while (basePos < baseLine.size() && baseLine.get(basePos).getStart() <= loc) {
        baseLineCount++;
        basePos++;
      }

      while (callPos < called.size() && called.get(callPos).getStart() <= loc) {
        calledCount++;
        callPos++;
      }
      list.add(new SyncPoint(loc, calledCount, baseLineCount));
    }
    return list;
  }


  /**
   * Find a weighting for all the TP calls in a path.
   * this is done by sync points, within each <code>SyncPoint</code>
   *
   * <code>weight = number of TP in baseline / number of TP in called</code>
   *
   * this will assure that the total number of TP we output will always reflect number of TP in baseline file
   *
   * return 2 lists first for TP and second for FP, we create FP calls when current call is included but does not correspond to a TP (this can happen when two calls cancel each other out when replayed)
   *
   * @param best best path
   * @return pair of called true positive and false positives
   */
  static Pair<List<OrientedVariant>, List<OrientedVariant>> calculateWeights(final Path best) {
    return calculateWeights(best, best.getCalledIncluded(), best.getBaselineIncluded());
  }

  static Pair<List<OrientedVariant>, List<OrientedVariant>> calculateWeights(final Path best, final List<OrientedVariant> calledTruePositives, final List<OrientedVariant> baseLineTruePositives) {
    assert best.mSyncPointList.size() >= 1;
    final List<SyncPoint> syncpoints = getSyncPointsList(best.getSyncPoints(), baseLineTruePositives, calledTruePositives);
    //Diagnostic.warning("Best path sync points" + syncpoints);
    assert syncpoints.size() == best.mSyncPointList.size();

    final List<OrientedVariant> tp = new ArrayList<>();
    final List<OrientedVariant> fp = new ArrayList<>();

    final Iterator<SyncPoint> syncIterator = syncpoints.iterator();
    int syncStart = 0;
    SyncPoint syncpoint = syncIterator.next();
    for (final OrientedVariant v : calledTruePositives) {
      while (syncpoint.mPos < v.getStart() && syncIterator.hasNext()) { // Jump to sync point entry containing this variant
        syncStart = syncpoint.mPos;
        syncpoint = syncIterator.next();
      }
      if (syncpoint.mBaselineTPCount == 0) {
        Diagnostic.developerLog("Best path called variant was bumped to FP due to no baseline TP within sync region "
            + v.getSequenceName() + ":" + (syncStart + 1) + "-" + (syncpoint.mPos + 1) + "\n"
            + "Bumped variant:  " + v);
        fp.add(v);
      } else {
        v.setWeight(syncpoint.mBaselineTPCount / syncpoint.mCalledTPCount);
        tp.add(v);
      }
    }
    return new Pair<>(tp, fp);
  }

}
