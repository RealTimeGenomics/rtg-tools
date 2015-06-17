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

import com.rtg.util.test.RandomByteGenerator;
import com.rtg.util.diagnostic.Diagnostic;

import junit.framework.TestCase;

/**
 * Class for testing the Object Chunks implementation that is capable of
 * holding more than the maximum integer worth of data.
 */
public class ObjectChunksRegression extends TestCase {

  private static final long NUM_ELEMENTS = 2L * Integer.MAX_VALUE + 9000L;
  private static final int RANGE = 128;

  /**
   * Test the common index implementation
   */
  public void testIndex() {
    Diagnostic.setLogStream();
    doTest(RANGE, NUM_ELEMENTS);
  }

  private void doTest(int range, long elements) {
    final RandomByteGenerator value = new RandomByteGenerator(range);

    final ObjectChunks<Byte> index = new ObjectChunks<>(elements);
    assertEquals(elements, index.length());

    for (long l = 0; l < elements; l++) {
      index.set(l, value.nextValue());
    }

    value.reset();

    for (long l = 0; l < elements; l++) {
      assertEquals(Byte.valueOf(value.nextValue()), index.get(l));
    }
  }
}
