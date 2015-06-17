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
package com.rtg.util;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class PortableRandomTest extends TestCase {

  public void testBasics() {

    final PortableRandom pr = new PortableRandom(3);

    assertEquals(-1155099828, pr.nextInt());
    assertEquals(-1879439976, pr.nextInt());
    assertEquals(304908421, pr.nextInt());

    assertEquals(16, pr.nextInt(45));
    assertEquals(8, pr.nextInt(10));
    assertEquals(0, pr.nextInt(1));

    assertEquals(0.768156984078079, pr.nextDouble());
    assertEquals(0.22733466107144407, pr.nextDouble());
    assertEquals(0.6603196166875382, pr.nextDouble());

    assertEquals(-3566243377172859107L, pr.nextLong());
    assertEquals(550039120288444364L, pr.nextLong());
    assertEquals(-3483296404361882349L, pr.nextLong());

    assertTrue(pr.nextBoolean());
    assertTrue(pr.nextBoolean());
    assertFalse(pr.nextBoolean());

    assertEquals(1.6791616586691243, pr.nextGaussian());
    assertEquals(0.6070711938870896, pr.nextGaussian());
    assertEquals(-1.3490545500116493, pr.nextGaussian());

    final byte[] b1 = new byte[5];

    pr.nextBytes(b1);

    assertTrue(Arrays.equals(new byte[] {(byte) 0x51, (byte) 0xD5, (byte) 0xA5, (byte) 0x76, (byte) 0x00}, b1));
    pr.nextBytes(b1);
    assertTrue(Arrays.equals(new byte[] {(byte) 0xF2, (byte) 0xFF, (byte) 0x64, (byte) 0xCF, (byte) 0xF3}, b1));
    pr.nextBytes(b1);
    assertTrue(Arrays.equals(new byte[] {(byte) 0x8C, (byte) 0xDB, (byte) 0xE7, (byte) 0x67, (byte) 0x12}, b1));
  }
}
