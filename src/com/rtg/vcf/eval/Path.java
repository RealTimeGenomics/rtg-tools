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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.rtg.util.BasicLinkedListNode;
import com.rtg.util.Pair;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.IntegralAbstract;

/**
 * Record a path that reconciles two sequences of variants. Also provides
 * method to find a best path.
 *
 */
public class Path extends IntegralAbstract implements Comparable<Path> {

  final HalfPath mCalledPath;
  final HalfPath mBaselinePath;
  //private final byte[] mTemplate;

  final BasicLinkedListNode<Integer> mSyncPointList;

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
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof SyncPoint)) {
        return false;
      }
      return this.compareTo((SyncPoint) obj) == 0;
    }

    @Override
    public int compareTo(SyncPoint o) {
      if (this.mPos < o.mPos) {
        return -1;
      } else if (this.mPos > o.mPos) {
        return 1;
      }
      return 0;
    }

  }

  /**
   * Construct a path object for the specified template
   *
   * @param template the template sequence to build a path for.
   */
  Path(byte[] template) {
    //mTemplate = template;
    mCalledPath = new HalfPath(template);
    mBaselinePath = new HalfPath(template);
    mSyncPointList = null;
  }


  Path(Path parent, BasicLinkedListNode<Integer> syncPoints) {
    mCalledPath = new HalfPath(parent.mCalledPath);
    mBaselinePath = new HalfPath(parent.mBaselinePath);
    //mTemplate = parent.mTemplate;
    mSyncPointList = syncPoints;
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

  @Override
  public void toString(StringBuilder sb) {
  }

  @Override
  public boolean integrity() {
    return true;
  }
  private static List<OrientedVariant> getIncluded(HalfPath path) {
    final List<OrientedVariant> list = new ArrayList<>();
    if (path.getIncluded() == null) {
      return list;
    }
    for (final OrientedVariant orientedVariant : path.getIncluded()) {
      list.add(0, orientedVariant);
    }
    return list;
  }

  private static List<Variant> getExcluded(HalfPath path) {
    final List<Variant> list = new ArrayList<>();
    if (path.getExcluded() == null) {
      return list;
    }
    for (final Variant variant : path.getExcluded()) {
      list.add(0, variant);
    }
    return list;
  }

  /**
   * @return the set of variants included on A side
   */
  public List<OrientedVariant> getCalledIncluded() {
    return getIncluded(mCalledPath);
  }

  /**
   * @return the set of variants excluded on A side
   */
  public List<Variant> getCalledExcluded() {
    return getExcluded(mCalledPath);
  }

  /**
   * @return the set of variants included on B side
   */
  public List<OrientedVariant> getBaselineIncluded() {
    return getIncluded(mBaselinePath);
  }

  /**
   * @return the set of variants excluded on B side
   */
  public List<Variant> getBaselineExcluded() {
    return getExcluded(mBaselinePath);
  }

  void include(boolean side, OrientedVariant var, int varIndex) {
    if (side) {
      mCalledPath.include(var, varIndex);
    } else {
      mBaselinePath.include(var, varIndex);
    }
  }

  void exclude(boolean side, Variant var, int varIndex) {
    if (side) {
      mCalledPath.exclude(var, varIndex);
    } else {
      mBaselinePath.exclude(var, varIndex);
    }
  }

  Collection<Path> addVariant(boolean side, Variant var, int varIndex) {
    final ArrayList<Path> paths = new ArrayList<>();
    final BasicLinkedListNode<Integer> syncPoints;
    if (this.inSync()) {
      syncPoints = new BasicLinkedListNode<>(this.mCalledPath.getPosition(), this.mSyncPointList);
    } else {
      syncPoints = this.mSyncPointList;
    }

    // Create a path extension that excludes this variant
    final Path exclude = new Path(this, syncPoints);
    exclude.exclude(side, var, varIndex);
    paths.add(exclude);

    // Create a path extension that includes this variant in the default phase
    final Path include = new Path(this, syncPoints);
    include.include(side, new OrientedVariant(var, true), varIndex);
    assert !include.equals(exclude);
    assert include.compareTo(exclude) != 0;
    paths.add(include);

    // If the variant is heterozygous we need to also add the variant in the alternate phase
    if (var.ntAlleleB() != null) {
      final Path hetero = new Path(this, syncPoints);
      hetero.include(side, new OrientedVariant(var, false), varIndex);
      paths.add(hetero);
    }

    return paths;
  }

  Collection<Path> addAVariant(Variant var, int varIndex) {
    return addVariant(true, var, varIndex);
  }

  Collection<Path> addBVariant(Variant var, int varIndex) {
    return addVariant(false, var, varIndex);
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
    if (!mCalledPath.finishedHaplotypeA() && !mBaselinePath.finishedHaplotypeA() && mCalledPath.nextHaplotypeABase() != mBaselinePath.nextHaplotypeABase()) {
      return false;
    }
    if (!mCalledPath.finishedHaplotypeB() && !mBaselinePath.finishedHaplotypeB() && mCalledPath.nextHaplotypeBBase() != mBaselinePath.nextHaplotypeBBase()) {
      return false;
    }
    return true;
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
    if (o == null) {
      return false;
    }

    if (!o.getClass().equals(getClass())) {
      return false;
    }
    return compareTo((Path) o) == 0;
  }

  @Override
  public int hashCode() {
    return Utils.hash(new Object[] {mCalledPath, mBaselinePath});
  }

  @Override
  public String toString() {
    return "Path:" + LS + "baseline=" + mBaselinePath + "called=" + mCalledPath + "syncpoints(0-based)=" + HalfPath.listToString(this.mSyncPointList);
  }
  public List<SyncPoint> getSyncPoints() {
    return getSyncPointsList(mSyncPointList, getCalledIncluded(), getBaselineIncluded());
  }

  // Counts the baseline and called variants between each sync point
  static List<SyncPoint> getSyncPointsList(final BasicLinkedListNode<Integer> syncpoints, final List<OrientedVariant> baseLine, final List<OrientedVariant> called) {
    // Push all the sync points onto a stack so we can process in reverse order.
    final Stack<Integer> stack = new Stack<>();
    for (final Integer syncpoint : syncpoints) {
      stack.push(syncpoint);
    }
    final List<SyncPoint> list = new ArrayList<>();
    int basePos = 0;
    int callPos = 0;
    while (!stack.isEmpty()) {
      final int loc = stack.pop();
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
   *       weight = number of TP in baseline / number of TP in called
   *
   * this will assure that the total number of TP we output will always reflect number of TP in baseline file
   *
   * return 2 lists first for TP and second for FP, we create FP calls when current call is included but does not correspond to a TP (this can happen when two calls cancel each other out when replayed)
   *
   * @param best best path
   * @param calledTruePositives called true positives
   * @param baseLineTruePositives baseline true positives
   *
   * @return pair of called true positive and false positives
   */
  static Pair<List<OrientedVariant>, List<OrientedVariant>> calculateWeights(final Path best, final List<OrientedVariant> calledTruePositives, final List<OrientedVariant> baseLineTruePositives) {
    assert best.mSyncPointList.size() >= 1;
    final List<SyncPoint> syncpoints = getSyncPointsList(best.mSyncPointList, baseLineTruePositives, calledTruePositives);
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
