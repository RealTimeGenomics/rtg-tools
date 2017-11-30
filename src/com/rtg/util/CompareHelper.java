/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.util;

import java.util.List;

/**
 * Attempt at making comparator methods easier to write.
 * Chain calls to compare and take the result at the end.
 */
public final class CompareHelper {
  private int mCurrent = 0;

  /**
   * Call this once for each pair of values you would like to sort by
   * Result will be the first non-zero comparison
   * @param o1 first comparable object
   * @param o2 object to compare to
   * @param <T> the type of objects to compare
   * @return this for chaining purposes
   */
  public <T extends Comparable<T>> CompareHelper compare(T o1, T o2) {
    if (mCurrent != 0) {
      return this;
    }
    mCurrent = o1.compareTo(o2);
    return this;
  }

  /**
   * Sets the comparison result to the first non-zero comparison of elements at the same index in the lists
   * or compares the sizes if one list is a prefix of the other
   * @param list1 first set of elements to compare
   * @param list2 second set of elements to compare
   * @param <T> type of the list
   * @return this for chaining purposes
   */
  public <T extends Comparable<T>> CompareHelper compareList(List<T> list1, List<T> list2) {
    if (mCurrent != 0) {
      return this;
    }
    for (int i = 0; i < list1.size() && i < list2.size(); ++i) {
      mCurrent = list1.get(i).compareTo(list2.get(i));
      if (mCurrent != 0) {
        return this;
      }
    }
    mCurrent = Integer.compare(list1.size(), list2.size());
    return this;
  }

  /**
   * Sets the comparison result to the first non-zero comparison of elements at the same index in the lists
   * or compares the sizes if one list is a prefix of the other
   * @param list1 first set of elements to compare
   * @param list2 second set of elements to compare
   * @param <T> type of the list
   * @return this for chaining purposes
   */
  public <T extends Comparable<T>> CompareHelper compareArray(T[] list1, T[] list2) {
    if (mCurrent != 0) {
      return this;
    }
    for (int i = 0; i < list1.length && i < list2.length; ++i) {
      mCurrent = list1[i].compareTo(list2[i]);
      if (mCurrent != 0) {
        return this;
      }
    }
    mCurrent = Integer.compare(list1.length, list2.length);
    return this;
  }

  /**
   * Call this to compare the string representation of two objects, which is only computed if earlier
   * comparisons have all returned zero.
   * Result will be the first non-zero comparison
   * @param o1 first object
   * @param o2 object to compare to
   * @return this for chaining purposes
   */
  public CompareHelper compareToString(Object o1, Object o2) {
    if (mCurrent != 0) {
      return this;
    }
    mCurrent = o1.toString().compareTo(o2.toString());
    return this;
  }

  /**
   * Add a result of an externally called comparison.
   * @param external result of some external attribute comparison
   * @return this for chaining purposes
   */
  public CompareHelper compare(int external) {
    if (mCurrent != 0) {
      return this;
    }
    mCurrent = external;
    return this;
  }

  /**
   * @return the value of the first comparison that was not zero or zero if all comparisons were equal
   */
  public int result() {
    return mCurrent;
  }
}
