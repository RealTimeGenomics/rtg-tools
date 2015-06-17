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
package com.rtg.util.array.objectindex;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test Chunks
 */
public class ObjectChunksTest extends AbstractObjectIndexTest {

  private static final int CHUNK_BITS = 29;

  public static Test suite() {
    return new TestSuite(ObjectChunksTest.class);
  }

  /**
   * Constructor for ChunksTest.
   */
  public ObjectChunksTest(final String arg0) {
    super(arg0);
  }

  @Override
  protected ObjectIndex<Integer> create(final long length) {
    return new ObjectChunks<>(length);
  }

  @Override
  protected ObjectIndex<Integer> create(final long length, final int bits) {
    return new ObjectChunks<>(length, bits);
  }

  public void testChunkSize() {
    final ObjectChunks<Integer> dc = new ObjectChunks<>(100L);
    dc.integrity();
    assertEquals(100, dc.length());
    assertEquals(1L << 28, dc.chunkSize());
  }

  public void testTooLong() {
    if (Runtime.getRuntime().freeMemory() < 4000000000L) {
      try {
        new ObjectChunks<Integer>((1L << CHUNK_BITS) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail("RuntimeException expected");
      } catch (final RuntimeException e) {
        //expected
      }
      try {
        new ObjectChunks<Integer>((1L << CHUNK_BITS - 1) * Integer.MAX_VALUE + 1L, CHUNK_BITS);
        fail("OutOfMemoryError expected");
      } catch (final OutOfMemoryError e) {
        //expected
      }
    }
  }

  /** Tests allocations of chunks and formula for computing how many chunks there are. */
  public void testEdgeCase() {
      final int bits = 3;
      final int size = 1 << bits;
      final ObjectIndex<Integer> ix0 = new ObjectChunks<>(size - 1, bits);
      ix0.integrity();
      final ObjectIndex<Integer> ix1 = new ObjectChunks<>(size, bits);
      ix1.integrity();
      final ObjectIndex<Integer> ix2 = new ObjectChunks<>(size + 1, bits);
      ix2.integrity();
  }
}
