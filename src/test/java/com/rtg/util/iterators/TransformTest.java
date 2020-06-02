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

package com.rtg.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

/**
 */
public class TransformTest extends TestCase {

  private static final MockTransform MOCK_TRANSFORM = new MockTransform();

  private static final MockInt MOCK_INT = new MockInt();

  private static class MockTransform extends Transform<String, Integer> {
    @Override
    public Integer trans(String x) {
      return Integer.valueOf(x);
    }
  }

  private static class MockInt extends Transform<Integer, Integer> {
    @Override
    public Integer trans(Integer x) {
      return -x;
    }
  }

  static <X> void check(Iterator<X> exp, Iterator<X> actual) {
    while (exp.hasNext()) {
      assertTrue(actual.hasNext());
      assertEquals(exp.next(), actual.next());
    }
    assertFalse(actual.hasNext());
  }

  public void test() {
    final Iterator<String> it = Transform.array2Iterator(new String[] {"1", "3", "2"});
    final Iterator<Integer> trans = MOCK_TRANSFORM.trans(it);
    final Iterator<Integer> exp = Transform.array2Iterator(new Integer[] {1, 3, 2});
    check(exp , trans);
  }

  public void testArray2IteratorEmpty() {
    final Iterator<String> act = Transform.array2Iterator(new String[0]);
    assertFalse(act.hasNext());
    try {
      act.next();
      fail();
    } catch (final NoSuchElementException e) {
      //expected
    }
  }

  public void testCompose() {
    final Transform<String, Integer> comp = Transform.compose(MOCK_TRANSFORM, MOCK_INT);
    assertEquals(Integer.valueOf(-1), comp.trans("1"));
  }
}
