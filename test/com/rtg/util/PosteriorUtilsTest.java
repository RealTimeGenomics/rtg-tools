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

import junit.framework.TestCase;

/**
 */
public class PosteriorUtilsTest extends TestCase {

  public void test() {
    assertEquals(16.17496, PosteriorUtils.phredIfy(3.7), 0.0001);
    assertEquals(3.7, PosteriorUtils.unphredIfy(16.17496), 0.0001);

    // Test cases generated using Maple
    checkPhredify(3.0103, 1.33825e-09);
    checkPhredify(3.0103, 4.20212e-09);
    checkPhredify(3.0103, 1.31946e-08);
    checkPhredify(3.0103, 4.14312e-08);
    checkPhredify(3.0103, 1.30094e-07);
    checkPhredify(3.0103, 4.08495e-07);
    checkPhredify(3.0103, 1.28267e-06);
    checkPhredify(3.01031, 4.02760e-06);
    checkPhredify(3.01033, 1.26467e-05);
    checkPhredify(3.01039, 3.97105e-05);
    checkPhredify(3.01057, 0.000124691);
    checkPhredify(3.01115, 0.00039153);
    checkPhredify(3.01297, 0.0012294);
    checkPhredify(3.01869, 0.00386032);
    checkPhredify(3.0367, 0.0121214);
    checkPhredify(3.09374, 0.0380613);
    checkPhredify(3.27757, 0.119512);
    checkPhredify(3.90119, 0.375269);
    checkPhredify(6.28286, 1.17834);
    checkPhredify(16.175, 3.7);
    checkPhredify(50.4564, 11.618);
    checkPhredify(158.433, 36.4805);
    checkPhredify(497.479, 114.549);
    checkPhredify(1562.08, 359.683);
    checkPhredify(4904.95, 1129.41);
    checkPhredify(15401.5, 3546.33);
    checkPhredify(48360.8, 11135.5);
    checkPhredify(151853, 34965.4);
    checkPhredify(476818, 109791);
    checkPhredify(1.49721e+06, 344745);
    checkPhredify(4.70124e+06, 1.08250e+06);
    checkPhredify(1.47619e+07, 3.39905e+06);
    checkPhredify(4.63523e+07, 1.06730e+07);
    checkPhredify(1.45546e+08, 3.35133e+07);
    checkPhredify(4.57015e+08, 1.05232e+08);
    checkPhredify(1.43503e+09, 3.30427e+08);
    checkPhredify(4.50599e+09, 1.03754e+09);
    checkPhredify(1.41488e+10, 3.25788e+09);

    assertEquals(3.0102999566398, PosteriorUtils.phredIfy(0.0), 0.0001);
    assertEquals(5.7034230418412, PosteriorUtils.phredIfy(1.0), 0.0001);

    assertEquals(0.10607, PosteriorUtils.nonIdentityPhredIfy(3.7), 0.0001);
    assertEquals(3.7, PosteriorUtils.nonIdentityUnPhredIfy(0.10607), 0.0001);
  }

  private void checkPhredify(final double phred, final double score) {
    assertEquals(phred, PosteriorUtils.phredIfy(score), 0.0001 * phred);
    assertEquals(score, PosteriorUtils.unphredIfy(phred), Math.max(0.0001 * score, 0.0001));
  }
}
