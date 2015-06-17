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

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Test Chunks
 */
public class LongIndexTest extends LongChunksTest {

  @Override
  protected LongIndex create(final long length) {
    return new LongChunks(length, 5);
  }

  public void testStuff() {

    try {
      new LongIndex(-1L) {

        @Override
        public void set(final long index, final long value) { }

        @Override
        public long get(final long index) {
          return 0;
        }

        @Override
        public boolean safeFromWordTearing() {
          return true;
        }

        @Override
        public void save(ObjectOutputStream dos) throws IOException {
          throw new UnsupportedOperationException("Not implemented yet");
        }
      };
      fail();
    } catch (final NegativeArraySizeException nase) {
      assertEquals("length=-1", nase.getMessage());
    }

    final LongIndex li = new LongIndex(4) {
      @Override
      public void set(final long index, final long value) { }
      @Override
      public long get(final long index) {
        return 5;
      }

      @Override
      public boolean safeFromWordTearing() {
        return true;
      }

      @Override
      public void save(ObjectOutputStream dos) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
      }
    };
    li.set(1, 5);
    assertEquals(5L, li.get(1));
    assert li.integrity();
  }
}
