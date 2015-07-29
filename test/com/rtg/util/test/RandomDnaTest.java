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
package com.rtg.util.test;


import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class RandomDnaTest extends TestCase {

  public void test() {
    final String s = RandomDna.random(10000);
    assertEquals(10000, s.length());
    int a = 0;
    int c = 0;
    int g = 0;
    int t = 0;
    for (int i = 0; i < 10000; i++) {
      switch (s.charAt(i)) {
      case 'A':
        a++;
        break;
      case 'C':
        c++;
        break;
      case 'G':
        g++;
        break;
      case 'T':
        t++;
        break;
      default:
        fail();
      }
    }
    assertTrue(a > 1000);
    assertTrue(c > 1000);
    assertTrue(g > 1000);
    assertTrue(t > 1000);
    assertTrue(a < 3500);
    assertTrue(c < 3500);
    assertTrue(g < 3500);
    assertTrue(t < 3500);
  }
}

