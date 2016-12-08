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
package com.rtg.util.array.shortindex;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Test Array
 */
public class ShortArrayTest extends AbstractShortIndexTest {

  @Override
  protected ShortIndex create(final long length) {
    return new ShortArray(length);
  }

  @Override
  protected ShortIndex create(final long length, final int bits) {
    //ignore bits
    return new ShortArray(length);
  }


  public void testSerial() throws IOException {
    final ShortArray la = new ShortArray(10);
    for (int i = 0; i < 10; ++i) {
      la.set(i, i * 4 + 7);
    }
    final ByteArrayOutputStream out =  new ByteArrayOutputStream();
    la.save(new ObjectOutputStream(out));
    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    final ShortIndex index2 = ShortCreate.loadIndex(new ObjectInputStream(in));
    assertTrue(index2 instanceof ShortArray);
    assertEquals(la.length(), index2.length());
    for (int i = 0; i < 10; ++i) {
      assertEquals(la.get(i), index2.get(i));
    }
  }
}

