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
package com.rtg.util.array.longindex;


import junit.framework.TestCase;

/**
 * Test Create
 */
public class LongCreateTest extends TestCase {

  private static final long FREE_LIMIT = 8L * Integer.MAX_VALUE + 1000000000L; //allow 1000m of freeboard

  public void testBad() {
    try {
      LongCreate.createIndex(-1L);
      fail("NegativeArraySizeException expected");
    } catch (final NegativeArraySizeException e) {
      //expected
      assertEquals("Negative length=-1", e.getMessage());
    }

    final LongIndex index = LongCreate.createIndex(0);
    index.integrity();
  }

  public void test() {
    final LongIndex a = LongCreate.createIndex(10);
    assertEquals(10, a.length());
    assertTrue(a instanceof LongArray);

    //Only run if there is enough memory not to nuke everything
    System.gc();
    final long mem = Runtime.getRuntime().freeMemory();
    //System.err.println("FREE_LIMIT=" + FREE_LIMIT + " mem=" + mem);
    if (mem > FREE_LIMIT && Runtime.getRuntime().freeMemory() < 4000000000L) {
      // SAI: There can be some JVM overheads that prevent Integer.MAX_VALUE lengths
      final int safeLength = Integer.MAX_VALUE - 5;
      final LongIndex b = LongCreate.createIndex(safeLength);
      assertEquals(safeLength, b.length());
      assertTrue(b instanceof LongArray);
      System.gc();

      final LongIndex c = LongCreate.createIndex(Integer.MAX_VALUE + 1L);
      assertEquals(Integer.MAX_VALUE + 1L, c.length());
      assertTrue(c instanceof LongChunks);
      System.gc();
    }
  }
}
