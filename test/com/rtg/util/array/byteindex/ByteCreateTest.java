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
package com.rtg.util.array.byteindex;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test Create
 */
public class ByteCreateTest extends TestCase {

  private static final long FREE_LIMIT = 2L * Integer.MAX_VALUE + 200000000L; //allow 200m of freeboard

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(ByteCreateTest.class);
  }

  public static Test suite() {
    return new TestSuite(ByteCreateTest.class);
  }

  /**
   * Constructor for CreateTest.
   */
  public ByteCreateTest(final String arg0) {
    super(arg0);
  }

  public void testBad() {
    try {
      ByteCreate.createIndex(-1);
      fail("NegativeArraySizeException expected");
    } catch (final NegativeArraySizeException e) {
      assertEquals("Negative length=-1", e.getMessage()); //expected
    }

    final ByteIndex index = ByteCreate.createIndex(0);
    index.integrity();
  }

  public void test() {
    final ByteIndex a = ByteCreate.createIndex(10);
    assertEquals(10, a.length());
    assertTrue(a instanceof ByteArray);

    //Only run if there is enough memory not to nuke everything
    System.gc();
    final long mem = Runtime.getRuntime().maxMemory();
    //System.err.println("FREE_LIMIT=" + FREE_LIMIT + " mem=" + mem);
    if (mem > FREE_LIMIT) {
      final ByteIndex b = ByteCreate.createIndex(ByteIndex.MAX_LENGTH);
      assertEquals(ByteIndex.MAX_LENGTH, b.length());
      assertTrue(b instanceof ByteArray);

      final ByteIndex c = ByteCreate.createIndex(ByteIndex.MAX_LENGTH + 1L);
      assertEquals(ByteIndex.MAX_LENGTH + 1L, c.length());
      assertTrue(c instanceof ByteChunks);
    }
    System.gc();
  }
}


