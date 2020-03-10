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

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Store information about multiple intervals on a single sequence.
 * Overlapping regions are joined together into a single interval.
 */
class MergedIntervals {

  /**
   * Map of region start to region end
   * a position is within the region if the floor entries value is greater than it
   */
  final TreeMap<Integer, Integer> mIntervals = new TreeMap<>();

  /**
   * Add a new interval to the set
   * @param interval the region to add
   */
  void add(Interval interval) {
    add(interval.getStart(), interval.getEnd());
  }

  /**
   * Add a new region to the set
   * @param start 0-based inclusive start position of the region
   * @param end 0-based exclusive end position of the region
   */
  void add(int start, int end) {
    final Map.Entry<Integer, Integer> floor = mIntervals.floorEntry(start);
    final Map.Entry<Integer, Integer> endFloor = mIntervals.floorEntry(end);
    final int actualStart;
    final int actualEnd;
    if (floor != null && start >= floor.getKey() && end <= floor.getValue()) {
      //Don't bother with regions already fully contained
      return;
    }
    //Get Start for Region
    if (floor == null || start > floor.getValue()) {
      actualStart = start;
    } else {
      actualStart = Math.min(floor.getKey(), start);
    }
    //Get End for Region
    if (endFloor == null) {
      actualEnd = end;
    } else {
      actualEnd = Math.max(endFloor.getValue(), end);
    }
    removeContainedBy(actualStart, actualEnd);
    mIntervals.put(actualStart, actualEnd);
  }

  /**
   * Subtract a region from the set
   * @param interval the region to subtract
   */
  void subtract(Interval interval) {
    subtract(interval.getStart(), interval.getEnd());
  }

  /**
   * Subtract a region from the set
   * @param start 0-based inclusive start position of the region
   * @param end 0-based exclusive end position of the region
   */
  void subtract(int start, int end) {
    final Map.Entry<Integer, Integer> floor = mIntervals.floorEntry(start);
    final Map.Entry<Integer, Integer> endFloor = mIntervals.lowerEntry(end);
    if (endFloor == null) {
      return; // query is to the left of all regions.
    }
    if (floor != null && floor.getKey() < end && floor.getValue() > start) {
      // We have a region that overlaps query start
      if (floor.getValue() > end) {
        // Region completely encompasses query, split
        mIntervals.put(end, floor.getValue());
      }
      mIntervals.put(floor.getKey(), start); // Truncate to start
    } else if (endFloor.getKey() < end && endFloor.getValue() > end) {
      // We have a region that overlaps end
      mIntervals.put(end, endFloor.getValue());
    }
    removeContainedBy(start, end);
  }

  //Remove any existing regions completely within start and end
  private void removeContainedBy(int start, int end) {
    Map.Entry<Integer, Integer> overlappedRegion;
    while ((overlappedRegion = mIntervals.lowerEntry(end)) != null) {
      if (overlappedRegion.getKey() < start) {
        break;
      }
      mIntervals.remove(overlappedRegion.getKey());
    }
  }

  /**
   * @return a mask that can be used for fast containment tests
   */
  public BitSet mask() {
    final BitSet mask = new BitSet();
    for (Map.Entry<Integer, Integer> region : mIntervals.entrySet()) {
      mask.set(region.getKey(), region.getValue());
    }
    return mask;
  }

  /**
   * @param pos zero based position within the sequence
   * @return true if the position provided falls within the intervals
   */
  boolean enclosed(int pos) {
    final Map.Entry<Integer, Integer> floor = mIntervals.floorEntry(pos);
    return floor != null && floor.getValue() > pos;
  }

  /**
   * @param start zero based position within the sequence
   * @param end zero based position within the sequence
   * @return true if the position provided falls entirely within the intervals
   */
  boolean enclosed(int start, int end) {
    final Map.Entry<Integer, Integer> floor = mIntervals.lowerEntry(end);
    if (floor == null) {
      return false;
    } else if (end > floor.getValue()) {
      return false;
    } else if (start < floor.getKey()) {
      return false;
    }
    return true;
  }

  /**
   * @param start zero based start position within the sequence
   * @param end zero based end position within the sequence
   * @return true if the range specified is overlapped by the intervals
   */
  boolean overlapped(int start, int end) {
    final Map.Entry<Integer, Integer> floor = mIntervals.lowerEntry(end);
    if (floor == null) {
      return false;
    } else {
      return start < floor.getValue() && end > floor.getKey();
    }
  }

  /**
   * Work out the total length of all intervals
   * @return the total length covered by regions
   */
  public int totalLength() {
    int total = 0;
    for (Map.Entry<Integer, Integer> region : mIntervals.entrySet()) {
      total += region.getValue() - region.getKey();
    }
    return total;
  }

  /** @return the number of regions */
  public int size() {
    return mIntervals.size();
  }
}
