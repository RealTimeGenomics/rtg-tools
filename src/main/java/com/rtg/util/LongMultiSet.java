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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An unordered multi-set.
 * @param <E> type of elements in the multi-set.
 */
public class LongMultiSet<E> {

  protected final Map<E, LongCounter> mMap;

  /**
   * Constructor
   */
  public LongMultiSet() {
    this(new HashMap<>());
  }

  protected LongMultiSet(Map<E, LongCounter> map) {
    mMap = map;
  }

  /**
   * Add a new element to the multi-set and increment its count.
   * @param element to be added.
   * @return count after add has been completed (&ge; 1).
   */
  public long add(E element) {
    final LongCounter c0 = mMap.get(element);
    final LongCounter c;
    if (c0 == null) {
      c = new LongCounter();
      mMap.put(element, c);
    } else {
      c = c0;
    }
    c.increment();
    return c.count();
  }

  /**
   * Add a new element to the multi-set and increment its count.
   * @param element to be added.
   * @param count to increment by.
   * @return count after add has been completed (&ge; 1).
   */
  public long add(final E element, final long count) {
    assert count >= 0;
    final LongCounter c0 = mMap.get(element);
    final LongCounter c;
    if (c0 == null) {
      if (count == 0) {
        return 0;
      }
      c = new LongCounter();
      mMap.put(element, c);
    } else {
      c = c0;
    }
    c.increment(count);
    return c.count();
  }

  /**
   * Get the count for the element.
   * @param element to be fetched.
   * @return the count for the element (0 if not in the multi-set otherwise &ge; 1).
   */
  public long get(E element) {
    final LongCounter c0 = mMap.get(element);
    if (c0 == null) {
      return 0;
    } else {
      return c0.count();
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("[ ");
    int count = 0;
    int lines = 1;
    for (final Entry<E, LongCounter> entry : mMap.entrySet()) {
      if (count > 0) {
        if (count % 10 == 0) {
          sb.append(StringUtils.LS);
          ++lines;
        }
        sb.append(", ");
      }
      sb.append(entry.getKey()).append("->").append(entry.getValue().count());
      ++count;
    }
    if (lines > 1) {
      sb.append(StringUtils.LS);
    }
    sb.append(']');
    return sb.toString();
  }

}
