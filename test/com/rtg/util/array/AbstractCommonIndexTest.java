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
package com.rtg.util.array;

import static com.rtg.util.StringUtils.LS;

import junit.framework.TestCase;

/**
 */
public abstract class AbstractCommonIndexTest extends TestCase {

  protected final void checkSwap(final AbstractIndex index) {
    assertTrue(index.globalIntegrity());
    for (long i = 0; i < index.length(); i++) {
      assertEquals(0, index.get(i));
    }
    // test toString() when all zeroes
    assertEquals("Index [10]" + LS, index.toString());

    index.set(3, 6);
    assertEquals(6, index.get(3));
    index.set(9, 5);
    index.swap(9, 3);
    assertEquals(0, index.get(2));
    assertEquals(5, index.get(3));
    assertEquals(0, index.get(4));
    assertEquals(0, index.get(8));
    assertEquals(6, index.get(9));

    // test toString()
    String str = index.toString().replaceAll("  *", " ");
    assertEquals("Index [10]" + LS + "[0] 0, 0, 0, 5, 0, 0, 0, 0, 0, 6" + LS, str);
    assertTrue(index.globalIntegrity());

    // test toString with a range
    StringBuilder buf = new StringBuilder();
    index.toString(buf, 7, index.length());
    str = buf.toString().replaceAll("  *", " ");
    assertEquals("[7] 0, 0, 6" + LS, str);

    // test dumpString with a range
    buf = new StringBuilder();
    index.dumpString(buf, 7, index.length());
    str = buf.toString().replaceAll("  *", " ");
    assertEquals("[7]"
        + " 00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000"
        + " 00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000"
        + " 00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000110" + LS, str);
  }
}
