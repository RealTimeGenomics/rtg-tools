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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class ComposeIteratorsTest extends TestCase {

  private static class Trans extends Transform<List<Integer>, Iterator<Integer>> {
    @Override
    public Iterator<Integer> trans(List<Integer> x) {
      return x.iterator();
    }
  }

  public void test() {
    final ArrayList<List<Integer>> lists = new ArrayList<>();
    lists.add(Arrays.asList(0, 1, 2, 3, 4));
    lists.add(Arrays.asList(5));
    lists.add(new ArrayList<Integer>());
    lists.add(Arrays.asList(6, 7));
    //final Iterator<Integer> iterator = new ComposeIterators<>(lists.iterator(), trans);
    final Iterator<Integer> iterator = Transform.flatten(lists.iterator());
    for (int i = 0; i < 8; ++i) {
      assertTrue("" + i, iterator.hasNext());
      assertEquals(i, iterator.next().intValue());

    }
    assertFalse(iterator.hasNext());
  }

  public void testFlatten2() {
    final ArrayList<List<Integer>> lists = new ArrayList<>();
    lists.add(Arrays.asList(0, 1, 2, 3, 4));
    lists.add(Arrays.asList(5));
    lists.add(new ArrayList<Integer>());
    lists.add(Arrays.asList(6, 7));
    //final Iterator<Integer> iterator = new ComposeIterators<>(lists.iterator(), trans);
    final Iterator<Integer> iterator = Transform.flatten(lists.iterator(), new Trans());
    for (int i = 0; i < 8; ++i) {
      assertTrue("" + i, iterator.hasNext());
      assertEquals(i, iterator.next().intValue());

    }
    assertFalse(iterator.hasNext());
  }

  public void testEmpty() {
    final Iterator<Integer> iterator = Transform.flatten(new ArrayList<List<Integer>>().iterator(), new Trans());
    assertFalse(iterator.hasNext());
  }

  public void testEmptyInternals() {
    final ArrayList<List<Integer>> lists = new ArrayList<>();
    lists.add(new ArrayList<Integer>());
    lists.add(new ArrayList<Integer>());
    final Iterator<Integer> iterator = Transform.flatten(lists.iterator(), new Trans());
    assertFalse(iterator.hasNext());
  }

  public void testEmptyFirst() {
    final ArrayList<List<Integer>> lists = new ArrayList<>();
    lists.add(new ArrayList<Integer>());
    lists.add(Arrays.asList(0, 1, 2));
    final Iterator<Integer> iterator = Transform.flatten(lists.iterator(), new Trans());
    for (int i = 0; i < 3; ++i) {
      assertTrue("" + i, iterator.hasNext());
      assertEquals(i, iterator.next().intValue());

    }
    assertFalse(iterator.hasNext());
  }
}
