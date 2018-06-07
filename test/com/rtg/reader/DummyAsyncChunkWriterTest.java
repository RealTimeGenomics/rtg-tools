/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.reader;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 */
public class DummyAsyncChunkWriterTest {
  static class MockAbstractAsyncChunkWriter extends AbstractAsyncChunkWriter<Integer> {
    List<Integer> mIntegers = new ArrayList<>();

    MockAbstractAsyncChunkWriter(int queueSize, List<Integer> integers) {
      super(queueSize);
      mIntegers = integers;
    }

    @Override
    protected String getThreadNamePrefix() {
      return "Thread";
    }

    @Override
    protected void synchronouslyWrite(Integer item) {
      mIntegers.add(item);
    }

    @Override
    protected void synchronouslyClose() {
    }
  }

  @Test
  public void testAccept() {
    final List<Integer> ints = new ArrayList<>();
    try (AbstractAsyncChunkWriter<Integer> writer = new MockAbstractAsyncChunkWriter(10, ints)) {
      writer.accept(Arrays.asList(1, 2, 3, 4));
      writer.accept(Arrays.asList(14, 13, 12, 11));
    }

    assertEquals(Arrays.asList(1, 2, 3, 4, 14, 13, 12, 11), ints);
  }

}
