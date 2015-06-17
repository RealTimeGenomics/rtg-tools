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
package com.rtg.util.bytecompression;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

/**
 * Tests for the corresponding class
 *
 */

public class SingleByteArrayTest extends TestCase {

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(SingleByteArrayTest.class);
  }

  public void test() throws Exception {
    final SingleByteArray sba = new SingleByteArray(10);
    assertEquals(10, sba.length());
    sba.set(2, new byte[]{3}, 1);
    assertEquals(3, MultiByteArrayTest.get1(sba, 2));

    final byte[] b = new byte[3];
    sba.get(b, 1, 2);
    assertEquals(0, b[0]);
    assertEquals(3, b[1]);
    assertEquals(0, b[2]);
    assertEquals(0, sba.get(0));
    assertEquals(0, sba.get(1));
    assertEquals(3, sba.get(2));
    try {
      sba.load(new ByteArrayInputStream(b), 0, 55);
      fail("expected index out of bounds exception");
    } catch (final IndexOutOfBoundsException ioe) {
      //expected
    }
    sba.load(new ByteArrayInputStream(b), 0, 2);
  }

}
