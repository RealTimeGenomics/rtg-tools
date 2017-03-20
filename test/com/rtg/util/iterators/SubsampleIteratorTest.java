/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.util.iterators;

import java.util.Iterator;

import com.rtg.util.PortableRandom;

import junit.framework.TestCase;

/**
 */
public class SubsampleIteratorTest extends TestCase {

  public void testEmpty() {
    assertFalse(new SubsampleIterator<>(new ArrayToIterator<>(new String[0]), new PortableRandom(), 1.0).hasNext());
    assertFalse(new SubsampleIterator<>(new ArrayToIterator<>(new Integer[0]), new PortableRandom(), 0.0).hasNext());
  }

  public void testSubsample() {
    Iterator<String> act = new SubsampleIterator<>(new ArrayToIterator<>(getStrings(20)), new PortableRandom(2), 0);
    assertEquals("", getString(act));

    act = new SubsampleIterator<>(new ArrayToIterator<>(getStrings(20)), new PortableRandom(2), 0.25);
    assertEquals("6 7 10 13 19 ", getString(act));

    act = new SubsampleIterator<>(new ArrayToIterator<>(getStrings(20)), new PortableRandom(2), 0.75);
    assertEquals("0 2 6 7 8 10 11 13 14 15 17 18 19 ", getString(act));

    act = new SubsampleIterator<>(new ArrayToIterator<>(getStrings(20)), new PortableRandom(), 1.0);
    assertEquals("0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 ", getString(act));
  }

  private String getString(Iterator<?> it) {
    final StringBuilder sb = new StringBuilder();
    while (it.hasNext()) {
      sb.append(it.next()).append(' ');
    }
    return sb.toString();
  }

  protected String[] getStrings(int num) {
    final String[] res = new String[num];
    for (int i = 0; i < num; i++) {
      res[i] = String.valueOf(i);
    }
    return res;
  }
}
