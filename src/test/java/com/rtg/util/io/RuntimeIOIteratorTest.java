/*
 * Copyright (c) 2020. Real Time Genomics Limited.
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
package com.rtg.util.io;


import java.io.IOException;
import java.util.function.Consumer;

import htsjdk.samtools.util.RuntimeIOException;
import junit.framework.TestCase;

public class RuntimeIOIteratorTest extends TestCase {

  private static class MockIOIterator implements IOIterator<String> {
    private final String[] mArr;
    private int mPos = 0;
    MockIOIterator(String... arr) {
      mArr = arr;
    }
    @Override
    public boolean hasNext() {
      return mPos < mArr.length;
    }

    @Override
    public String next() throws IOException {
      if (mArr[mPos] == null) {
        throw new IOException("no element at " + mPos);
      }
      return mArr[mPos++];
    }

    @Override
    public void forEach(Consumer<? super String> action) throws IOException {
      IOIterator.forEach(this, action);
    }

    @Override
    public void close() { }
  }

  public void testIterator() throws IOException {
    final MockIOIterator mi = new MockIOIterator("a", "b", "c");
    StringBuilder sb = new StringBuilder();
    mi.forEach(sb::append);
    assertEquals("abc", sb.toString());

    final RuntimeIOIterator<String> ri = new RuntimeIOIterator<>(new MockIOIterator("a", "b", "c"));
    sb = new StringBuilder();
    ri.forEach(sb::append);
    assertEquals("abc", sb.toString());
  }

  public void testException() {
    final MockIOIterator mi = new MockIOIterator("a", null, "c");
    final StringBuilder sb = new StringBuilder();
    try {
      mi.forEach(sb::append);
      fail();
    } catch (IOException e) {
      assertEquals(1, mi.mPos);
    }
    assertEquals("a", sb.toString());
  }

  public void testExceptionWrap() {
    final MockIOIterator mi = new MockIOIterator("a", null, "c");
    final RuntimeIOIterator<String> ri = new RuntimeIOIterator<>(mi);
    final StringBuilder sb = new StringBuilder();
    try {
      ri.forEach(sb::append);
      fail();
    } catch (RuntimeIOException e) {
      assertEquals(1, mi.mPos);
    }
    assertEquals("a", sb.toString());
  }
}
