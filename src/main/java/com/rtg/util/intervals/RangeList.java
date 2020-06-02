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

package com.rtg.util.intervals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A structure that holds multiple, possibly overlapping ranges that can be searched for a point within the ranges.
 *
 * Makes use of a segment tree <code>http://en.wikipedia.org/wiki/Segment_tree</code> to store and efficiently look up meta-data within ranges.
 * The input meta-data are split and merged to form a single set of non-overlapping meta-data ranges.
 * A continuous set of ranges (with and without meta-data) is stored in the ranges array.
 * A simple binary search mechanism is used to look up the meta-data for a given location.
 */
public class RangeList<T> {

  /**
   * Range that links to other ranges which enclose it.
   */
  public static final class RangeView<T> extends Range {

    private List<RangeMeta<T>> mEnclosingRanges = null;

    private RangeView(int start, int end) {
      super(start, end);
    }

    /** @return true if this range has enclosing ranges */
    public boolean hasRanges() {
      return mEnclosingRanges != null;
    }

    /** @return a list of the meta information from all ranges spanning this range */
    public List<T> getMeta() {
      if (mEnclosingRanges == null) {
        return null;
      }
      return mEnclosingRanges.stream().map(RangeMeta::getMeta).collect(Collectors.toList());
    }

    private void addEnclosingRange(RangeMeta<T> range) {
      assert range.getStart() <= getStart() && range.getEnd() >= getEnd();
      if (mEnclosingRanges == null) {
        mEnclosingRanges = new ArrayList<>();
      }
      mEnclosingRanges.add(range);
    }

    /**
     * @return the list of ranges as originally specified that are covered by this range
     */
    public List<RangeMeta<T>> getEnclosingRanges() {
      return mEnclosingRanges;
    }
  }

  private final List<RangeView<T>> mRanges;
  private final List<RangeView<T>> mNonEmptyRanges;

  /**
   * Convenience constructor.
   * @param range a meta-data range to store for searching.
   */
  public RangeList(RangeMeta<T> range) {
    this(Collections.singletonList(range));
  }

  /**
   * Constructor.
   * @param ranges the list of meta-data ranges to store for searching.
   */
  public RangeList(List<RangeMeta<T>> ranges) {
    if (ranges == null || ranges.isEmpty()) {
      mRanges = new ArrayList<>(1);
      mRanges.add(new RangeView<>(Integer.MIN_VALUE, Integer.MAX_VALUE));
    } else {
      // get list of range boundaries
      mRanges = getRangeViews(ranges);

      // load input ranges into the non-overlapping views
      for (final RangeMeta<T> range : ranges) {
        int index = findFullRangeIndex(range.getStart());
        while (index < mRanges.size() && mRanges.get(index).getEnd() <= range.getEnd()) {
          mRanges.get(index).addEnclosingRange(range);
          ++index;
        }
      }
    }
    mNonEmptyRanges = new ArrayList<>();
    for (final RangeView<T> range : mRanges) {
      if (range.hasRanges()) {
        mNonEmptyRanges.add(range);
      }
    }
  }

  // Create a list of non-overlapping RangeView objects
  private static <U> List<RangeView<U>> getRangeViews(List<RangeMeta<U>> ranges) {
    final HashSet<Integer> pivots = new HashSet<>();
    for (final RangeMeta<?> range : ranges) {
      pivots.add(range.getStart());
      pivots.add(range.getEnd());
    }
    final int[] pivots2 = new int[pivots.size()];
    int i2 = 0;
    for (final Integer x : pivots) {
      pivots2[i2] = x;
      ++i2;
    }
    Arrays.sort(pivots2);

    // set up continuous non-overlapping ranges for -inf to +inf
    final List<RangeView<U>> views = new ArrayList<>(pivots2.length + 1);
    if (pivots2[0] != Integer.MIN_VALUE) {
      views.add(new RangeView<>(Integer.MIN_VALUE, pivots2[0]));
    }
    for (int i = 1; i < pivots2.length; ++i) {
      views.add(new RangeView<>(pivots2[i - 1], pivots2[i]));
    }
    if (pivots2[pivots2.length - 1] != Integer.MAX_VALUE) {
      views.add(new RangeView<>(pivots2[pivots2.length - 1], Integer.MAX_VALUE));
    }
    return views;
  }

  /**
   * Return the list of meta-data for a given location.  Returns null if no meta-data exists.
   * @param loc the position to look up the containing range for.
   * @return meta-data list
   */
  public List<T> find(int loc) {
    return findRange(loc).getMeta();
  }

  private RangeView<T> findRange(int loc) {
    return mRanges.get(findFullRangeIndex(loc));
  }

  /**
   * @return the list of non-empty ranges
   */
  public List<RangeView<T>> getRangeList() {
    return mNonEmptyRanges;
  }

  /**
   * @return the full list of ranges, includes ranges corresponding to empty intervals
   */
  public List<RangeView<T>> getFullRangeList() {
    return mRanges;
  }

  /**
   * Return the index of the range within the full range list containing the specified point.
   * @param loc the position to search
   * @return the index of the range entry containing the position
   */
  public final int findFullRangeIndex(int loc) {
    if (loc == Integer.MAX_VALUE) {
      return mRanges.size() - 1;
    }
    int min = 0;
    int max = mRanges.size();
    int res = (max + min) / 2;

    // binary search
    boolean found = false;
    while (!found) {
      //System.err.println(min + " " + res + " " + max + " : " + loc + " " + ranges[res]);
      final RangeView<T> range = mRanges.get(res);
      if (range.contains(loc)) {
        found = true;
      } else {
        if (loc < range.getStart()) {
          max = res;
        } else {
          min = res;
        }
        res = (max + min) / 2;
      }
    }
    return res;
  }

  @Override
  public String toString() {
    return mNonEmptyRanges.toString();
  }
}
