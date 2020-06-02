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

package com.rtg.reader;

import com.rtg.util.test.RandomByteGenerator;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 * Base test class for testing the various <code>ByteArray</code> implementations
 * that can have large amounts of data contained within them.
 */
public abstract class AbstractByteArrayRegression extends TestCase {

  private static final int RANGE = 128;
  private static final long NUM_ELEMENTS = 10L * Integer.MAX_VALUE + 9000L;

  protected abstract ByteArray createByteArray(int range, long elements);

  /**
   * Test the byte array
   */
  public void testByteArrayImplementation() {
    Diagnostic.setLogStream();
    doTest(RANGE, NUM_ELEMENTS);
  }

  private void doTest(int range, long elements) {
    final RandomByteGenerator value = new RandomByteGenerator(range);
    final byte[] buffer = new byte[1024];
    final ByteArray byteArray = createByteArray(range, elements);
    assertEquals(elements, byteArray.length());
    long lastOffset = 0;
    for (long l = 0; l < elements;) {
      int i = 0;
      for (; i < buffer.length && l < elements; ++i, ++l) {
        buffer[i] = value.nextValue();
      }
      byteArray.set(lastOffset, buffer, i);
      lastOffset = l;
    }

    value.reset();

//  final long labelChunkSize = NUM_ELEMENTS / 1000L;
//  long nextLabelOutput = labelChunkSize;
    byte val = -1;
    for (long l = 0; l < elements;) {
      final int read = (int) Math.min(buffer.length, elements - l);
      byteArray.get(buffer, l, read);
      for (int i = 0; i < read; ++i, ++l) {
        val = value.nextValue();
        assertEquals(val, buffer[i]);
      }
//      if (l >= nextLabelOutput) {
//        System.err.println("Elements Read: " + l);
//        nextLabelOutput += labelChunkSize;
//      }
    }
    assertEquals(val, byteArray.get(elements - 1));
  }
}
