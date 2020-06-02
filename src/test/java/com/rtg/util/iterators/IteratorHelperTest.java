/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.util.iterators;

import java.util.Iterator;

import junit.framework.TestCase;

/**
 */
public class IteratorHelperTest extends TestCase {

  private static class MockIterator extends IteratorHelper<Integer> {
    private final int mThreshold;
    private final int[] mX;
    private int mIndex = 0;

    MockIterator(int threshold, int... x) {
      mThreshold = threshold;
      mX = x;
    }

    @Override
    protected void step() {
      ++mIndex;
    }

    @Override
    protected boolean isOK() {
      return mX[mIndex] >= mThreshold;
    }

    @Override
    protected boolean atEnd() {
      return mIndex >= mX.length;
    }

    @Override
    protected Integer current() {
      return mX[mIndex];
    }
  }

  public void test() {
    final Iterator<Integer> it = new MockIterator(5, 1, 6, 7, 1, 2, 3, 10);
    assertTrue(it.hasNext());
    assertEquals(Integer.valueOf(6), it.next());
    assertTrue(it.hasNext());
    assertEquals(Integer.valueOf(7), it.next());
    assertTrue(it.hasNext());
    assertEquals(Integer.valueOf(10), it.next());
    assertFalse(it.hasNext());
  }

  public void test1() {
    final Iterator<Integer> it = new MockIterator(5, 6, 7, 1, 2, 3);
    assertTrue(it.hasNext());
    assertEquals(Integer.valueOf(6), it.next());
    assertTrue(it.hasNext());
    assertEquals(Integer.valueOf(7), it.next());
    assertFalse(it.hasNext());
  }

  public void testEmpty() {
    final Iterator<Integer> it = new MockIterator(15, 1, 6, 7, 1, 2, 3, 10);
    assertFalse(it.hasNext());
  }

  public void testEmpty1() {
    final Iterator<Integer> it = new MockIterator(15);
    assertFalse(it.hasNext());
  }
}
