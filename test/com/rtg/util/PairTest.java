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
package com.rtg.util;

import junit.framework.TestCase;

/**
 */
public class PairTest extends TestCase {

  public final void test() {
    final Pair<String, Integer> p = new Pair<>("abc", 0);
    assertEquals("abc:0", p.toString());
    assertEquals("abc", p.getA());
    assertEquals(Integer.valueOf(0), p.getB());
    assertEquals(p, p);
    assertFalse(p.equals(null));
    assertFalse(p.equals("abc"));

    final Pair<String, Integer> q = new Pair<>("abc", 0);
    assertEquals(p, q);
    assertEquals(p.hashCode(), q.hashCode());
    assertEquals(1388221096, p.hashCode()); // a regression test - hard to work out what it will be
  }

  public final void testEquals() {
    final Object[][] groups = {
      {new Pair<>("", 0), new Pair<>("", 0) },
      {new Pair<>("", 1), new Pair<>("", 1) },
      {new Pair<>("a", 0), new Pair<>("a", 0) },
      {new Pair<>("a", 1), new Pair<>("a", 1) },
    };
    TestUtils.equalsHashTest(groups);
  }

  public final void testBad() {

    try {
      new Pair<String, String>(null, "");
      fail("NullPointerException expected");
    } catch (final NullPointerException e) {
      //expeted
    }
    try {
      new Pair<String, String>("", null);
      fail("NullPointerException expected");
    } catch (final NullPointerException e) {
      //expeted
    }
  }
}

