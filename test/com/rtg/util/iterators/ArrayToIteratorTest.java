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

import junit.framework.TestCase;

/**
 */
public class ArrayToIteratorTest extends TestCase {

  private static final MockTransform MOCK_TRANSFORM = new MockTransform();

  private static class MockTransform extends Transform<String, Integer> {
    @Override
    public Integer trans(String x) {
      return Integer.valueOf(x);
    }
  }

  public void testEmpty() {
    final Iterator<String> it = Transform.array2Iterator(new String[0]);
    final Iterator<Integer> trans = MOCK_TRANSFORM.trans(it);
    final Iterator<Integer> exp = Transform.array2Iterator(new Integer[0]);
    TransformTest.check(exp , trans);
  }

  public void testArray2Iterator() {
    final Iterator<String> act = Transform.array2Iterator(new String[] {"1", "3", "2"});
    assertTrue(act.hasNext());
    assertEquals("1", act.next());
    assertTrue(act.hasNext());
    assertEquals("3", act.next());
    assertTrue(act.hasNext());
    assertEquals("2", act.next());
    assertFalse(act.hasNext());
  }
}
