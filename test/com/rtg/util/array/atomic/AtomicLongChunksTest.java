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

package com.rtg.util.array.atomic;

import junit.framework.TestCase;

/**
 * Test
 */
public class AtomicLongChunksTest extends TestCase {

  public void test() {
    final AtomicLongChunks ic = new AtomicLongChunks(42);
    set(0, ic);
    check(ic);

    assertFalse(ic.compareAndSet(20, 20, 40));
    assertEquals(21, ic.get(20));
    assertTrue(ic.compareAndSet(20, 21, 40));
    assertEquals(40, ic.get(20));
    assertEquals(64, ic.fieldBits());
  }

  private static void set(final int start, final AtomicLongChunks lc) {
    for (int i = start; i < lc.length(); ++i) {
      lc.set(i, i + 1);
    }
  }

  private static void check(final AtomicLongChunks lc) {
    for (int i = 0; i < lc.length(); ++i) {
      assertEquals(i + 1, lc.get(i));
    }
  }
}