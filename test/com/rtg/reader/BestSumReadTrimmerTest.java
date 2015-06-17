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

import junit.framework.TestCase;

/**
 */
public class BestSumReadTrimmerTest extends TestCase {

  public void testVeryGoodQual() {
    final byte[] quals = {73, 73, 73, 73, 73, 73, 73, 73, 73};
    final BestSumReadTrimmer bsrt = new BestSumReadTrimmer(15);

    assertEquals(quals.length, bsrt.getTrimPosition(quals, quals.length));
    assertEquals(8, bsrt.getTrimPosition(quals, 8));
  }

  public void testHalfGoodQual() {
    final byte[] quals = {73, 73, 73, 73, 13, 13, 13, 13};
    BestSumReadTrimmer bsrt = new BestSumReadTrimmer(15);

    assertEquals(4, bsrt.getTrimPosition(quals, quals.length));
    assertEquals(4, bsrt.getTrimPosition(quals, 6));
    bsrt = new BestSumReadTrimmer(14);
    assertEquals(4, bsrt.getTrimPosition(quals, 6));
    bsrt = new BestSumReadTrimmer(13);
    assertEquals(6, bsrt.getTrimPosition(quals, 6));
  }

  public void testGoodBadGoodOk() {
    final byte[] quals = {73, 73, 73, 73, 13, 13, 13, 13, 20, 20, 20, 20};
    BestSumReadTrimmer bsrt = new BestSumReadTrimmer(15);

    assertEquals(quals.length, bsrt.getTrimPosition(quals, quals.length));
    assertEquals(quals.length - 1, bsrt.getTrimPosition(quals, quals.length - 1));
    assertEquals(quals.length - 2, bsrt.getTrimPosition(quals, quals.length - 2));
    assertEquals(quals.length - 3, bsrt.getTrimPosition(quals, quals.length - 3));  //passes because the last value is gt threshold
    assertEquals(4, bsrt.getTrimPosition(quals, 6));
    bsrt = new BestSumReadTrimmer(1);
    assertEquals(quals.length, bsrt.getTrimPosition(quals, quals.length));
    assertEquals(8, bsrt.getTrimPosition(quals, 8));
    bsrt = new BestSumReadTrimmer(14);
    assertEquals(4, bsrt.getTrimPosition(quals, 6));
    bsrt = new BestSumReadTrimmer(13);
    assertEquals(6, bsrt.getTrimPosition(quals, 6));
    bsrt = new BestSumReadTrimmer(23);
    assertEquals(4, bsrt.getTrimPosition(quals, 8));
  }

  public void testZeroLength() {
    final BestSumReadTrimmer bsrt = new BestSumReadTrimmer(65535);
    bsrt.getTrimPosition(new byte[0], 1);
  }

}
