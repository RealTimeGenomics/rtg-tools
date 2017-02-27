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

import java.util.Arrays;

import com.rtg.mode.DnaUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.test.RandomDna;

import junit.framework.TestCase;

/**
 */
public class BestSumReadTrimmerTest extends TestCase {

  private byte[] reverse(byte[] arrIn, int length) {
    final byte[] arr = Arrays.copyOf(arrIn, Math.min(arrIn.length, length));
    for (int i = 0; i < arr.length / 2; i++) {
      final byte temp = arr[i];
      arr[i] = arr[arr.length - i - 1];
      arr[arr.length - i - 1] = temp;
    }
    return arr;
  }

  private void checkBothDirs(int threshold, int expect, byte[] read, byte[] quals, int length) {
    final BestSumReadTrimmer bsrte = new BestSumReadTrimmer(threshold);
    final BestSumReadTrimmer bsrts = new BestSumReadTrimmer(threshold, true);

    assertEquals(expect, bsrte.trimRead(read, quals, length));
    assertEquals(expect, bsrts.trimRead(read, reverse(quals, length), length));
  }

  public void testVeryGoodQual() {
    final byte[] quals = {73, 73, 73, 73, 73, 73, 73, 73, 73};
    final byte[] read = DnaUtils.encodeString(RandomDna.random(quals.length, new PortableRandom(42)));
    checkBothDirs(15, quals.length, read, quals, quals.length);
    checkBothDirs(15, 8, read, quals, 8);
  }

  public void testHalfGoodQual() {
    final byte[] quals = {73, 73, 73, 73, 13, 13, 13, 13};
    final byte[] read = DnaUtils.encodeString(RandomDna.random(quals.length, new PortableRandom(42)));
    checkBothDirs(15, 4, read, quals, quals.length);
    checkBothDirs(15, 4, read, quals, 6);
    checkBothDirs(14, 4, read, quals, 6);
    checkBothDirs(13, 6, read, quals, 6);
  }

  public void testGoodBadGoodOk() {
    final byte[] quals = {73, 73, 73, 73, 13, 13, 13, 13, 20, 20, 20, 20};
    final byte[] read = DnaUtils.encodeString(RandomDna.random(quals.length, new PortableRandom(42)));
    checkBothDirs(15, quals.length, read, quals, quals.length);
    checkBothDirs(15, quals.length - 1, read, quals, quals.length - 1);
    checkBothDirs(15, quals.length - 2, read, quals, quals.length - 2);
    checkBothDirs(15, quals.length - 3, read, quals, quals.length - 3);  //passes because the last value is gt threshold
    checkBothDirs(15, 4, read, quals, 6);

    checkBothDirs(1, quals.length, read, quals, quals.length);
    checkBothDirs(1, 8, read, quals, 8);
    checkBothDirs(14, 4, read, quals, 6);
    checkBothDirs(13, 6, read, quals, 6);
    checkBothDirs(23, 4, read, quals, 8);
  }

  public void testZeroLength() {
    checkBothDirs(65535, 1, new byte[0], new byte[0], 1);
  }

}
