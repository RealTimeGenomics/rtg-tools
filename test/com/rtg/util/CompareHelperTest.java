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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 */
public class CompareHelperTest extends TestCase {
  public void testDefault() {
    assertEquals(0, new CompareHelper().result());
  }

  public void testChaining() {
    final CompareHelper ch = new CompareHelper();
    assertEquals(ch, ch.compare(3, 3));
    assertEquals(0, ch.result());
    assertEquals(ch, ch.compare("Foo", "Foo")
        .compare(5.0, 8.0)
        .compare(5.0, 5.0));
    assertEquals(Double.compare(5.0, 8.0),  ch.result());
  }

  public void testString() {
    assertEquals("foo".compareTo("Foo")  , new CompareHelper()
        .compare(30, 30)
        .compare("foo", "Foo")
        .compare(10.0, 50.0)
        .result()
    );
  }
  public void testListCompare() {
    final List<Integer> first = Arrays.asList(1, 2, 3, 4);
    final List<Integer> second = Arrays.asList(1, 2, 3, 4, 5);
    final List<Integer> third = Arrays.asList(1, 2, 4, 4, 5);
    final List<Integer> fourth = Arrays.asList(-1, 2, 4, 4, 5);
    assertEquals(0, new CompareHelper().compareList(first, first).result());
    assertEquals(0, new CompareHelper().compareList(second, second).result());
    assertEquals(0, new CompareHelper().compareToString(5, 5).result());
    assertEquals(Integer.compare(4, 5), new CompareHelper().compareList(first, second).result());
    assertEquals(Integer.compare(3, 4), new CompareHelper().compareList(first, third).result());
    assertEquals(Integer.compare(1, -1), new CompareHelper().compareList(first, fourth).result());
  }

  public void testListCompareAlreadyDifferent() {
    final List<Integer> first = Arrays.asList(3, 2, 3, 4);
    final List<Integer> second = Arrays.asList(1, 2, 3, 4);
    assertEquals("foo".compareTo("bar"), new CompareHelper().compare("foo", "bar").compareList(first, second).result());
  }
}
