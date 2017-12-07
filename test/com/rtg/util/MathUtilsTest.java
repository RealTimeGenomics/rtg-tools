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
public class MathUtilsTest extends TestCase {

  public void testCeilPower2() {
    assertEquals(1, MathUtils.ceilPowerOf2(0));
    assertEquals(2, MathUtils.ceilPowerOf2(1));
    assertEquals(4, MathUtils.ceilPowerOf2(2));
    assertEquals(4, MathUtils.ceilPowerOf2(3));
    assertEquals(8, MathUtils.ceilPowerOf2(4));
    long i = 1L;
    while (i > 0) {
      assertEquals(i, MathUtils.ceilPowerOf2(i - 1));
      if (i << 1 > 0) {
        assertEquals(i << 1, MathUtils.ceilPowerOf2(i));
      }
      final long j = i << 1;
      if (j > 2) {
        assertEquals((i + 1) + ":" + j, j, MathUtils.ceilPowerOf2(i + 1));
      }
      i = i << 1;
    }
    assertEquals(Long.MIN_VALUE, i);
  }
  public void testCeilPower2Error() {
    try {
      MathUtils.ceilPowerOf2(-1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      MathUtils.ceilPowerOf2(Long.MIN_VALUE);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      MathUtils.ceilPowerOf2(Long.MIN_VALUE >> 1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }
  public void testCeilPower2Bits() {
    assertEquals(0, MathUtils.ceilPowerOf2Bits(0));
    assertEquals(1, MathUtils.ceilPowerOf2Bits(1));
    assertEquals(2, MathUtils.ceilPowerOf2Bits(2));
    assertEquals(2, MathUtils.ceilPowerOf2Bits(3));
    assertEquals(3, MathUtils.ceilPowerOf2Bits(4));
    long i = 1L;
    int n = 0;
    while (i > 0) {
      assertEquals(n, MathUtils.ceilPowerOf2Bits(i - 1));
      if (i << 1 > 0) {
        assertEquals(n + 1, MathUtils.ceilPowerOf2Bits(i));
      }
      final long j = i << 1;
      if (j > 2) {
        assertEquals((i + 1) + ":" + n, n + 1, MathUtils.ceilPowerOf2Bits(i + 1));
      }
      i = i << 1;
      ++n;
    }
    assertEquals(Long.MIN_VALUE, i);
  }
  public void testCeilPower2BitsError() {
    try {
      MathUtils.ceilPowerOf2Bits(-1);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      assertEquals("Number out of range:-1", e.getMessage());
    }
    try {
      MathUtils.ceilPowerOf2Bits(Long.MIN_VALUE);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      assertEquals("Number out of range:-9223372036854775808", e.getMessage());
    }
    assertEquals(62, MathUtils.ceilPowerOf2Bits(Long.MAX_VALUE >> 1));
    try {
      MathUtils.ceilPowerOf2Bits((Long.MAX_VALUE >> 1) + 1L);
      fail("Exception expected");
    } catch (final RuntimeException e) {
      assertEquals("Number out of range:4611686018427387904", e.getMessage());
    }
  }

  public void testLogBinomialP() {
    final double[]  pascal = new double[10];
    pascal[0] = 1.0;
    for (int i = 0; i < pascal.length - 1; ++i) {
      for (int j = 0; j <= i; ++j) {
        assertEquals(i + ":" + j, -Math.log(pascal[j]) + i * Math.log(2.0) , MathUtils.logBinomial(0.5, i, j), 1.0e-7);
      }
      for (int j = i; j >= 0; --j) {
        pascal[j + 1] += pascal[j];
      }
    }
    assertEquals(0.0 , MathUtils.logBinomial(0.0, 5, 0), 0.0);
    assertEquals(0.0 , MathUtils.logBinomial(1.0, 5, 5), 0.0);
    try {
      MathUtils.logBinomial(0.0, 5, 2);
    } catch (final IllegalArgumentException e) {
      //expected
    }
    try {
      MathUtils.logBinomial(1.0, 5, 2);
    } catch (final IllegalArgumentException e) {
      //expected
    }
  }

  public void testLogBinomialPBad() {
    assertEquals(0.0, MathUtils.logBinomial(0.0, 2, 0));
    try {
      assertEquals(0.0, MathUtils.logBinomial(0.0, 2, 1));
      fail();
    } catch (final IllegalArgumentException e) {
      final String teststring = "if probability is 0.0 then count must be 0. p:0.0 N:2 n:1";
      assertEquals(teststring, e.getMessage());
    }
    assertEquals(0.0, MathUtils.logBinomial(1.0, 2, 2));
    try {
      assertEquals(0.0, MathUtils.logBinomial(1.0, 2, 1));
      fail();
    } catch (final IllegalArgumentException e) {
      final String teststring = "if probability is 1.0 then count must be 0. p:1.0 N:2 n:1";
      assertEquals(teststring, e.getMessage());
    }
  }

  public void testLogBinomial() {
    final double[]  pascal = new double[10];
    pascal[0] = 1.0;
    for (int i = 0; i < pascal.length - 1; ++i) {
      for (int j = 0; j <= i; ++j) {
        assertEquals(i + ":" + j, Math.log(pascal[j]), MathUtils.logBinomial(i, j), 1.0e-7);
      }
      for (int j = i; j >= 0; --j) {
        pascal[j + 1] += pascal[j];
      }
    }
  }


  public void testLogFactorial() {
    double f = 2.0;
    for (int i = 2; i < 40; ++i) {
      final double lf = Math.log(f);
      final double error = (MathUtils.logFactorial(i) - lf) / lf;
      //System.err.println("i:" + i + " error:" + error);
      assertEquals(0.0, error, 1.0e-7);
      f = f * (i + 1);
    }
    assertEquals(0.0, MathUtils.logFactorial(0), 0.0);
    assertEquals(0.0, MathUtils.logFactorial(1), 0.0);
    assertEquals(Math.log(2.0), MathUtils.logFactorial(2), 1.0e-9);
    assertEquals(Math.log(6.0), MathUtils.logFactorial(3), 1.0e-9);
  }

  public void testRenormalize() {
    assertEquals(1 / 3.0, MathUtils.renormalize(new int[] {1, 2})[0]);
    assertEquals(2 / 3.0, MathUtils.renormalize(new int[] {1, 2})[1]);
  }

  public void testRenormalizeDouble() {
    assertEquals(1 / 3.0, MathUtils.renormalize(new double[] {1, 2})[0]);
    assertEquals(2 / 3.0, MathUtils.renormalize(new double[] {1, 2})[1]);
  }

  public void testlnToNormalizedProb() {
    checkLnToNormalizedProb(new double[] {-42.0}, new double[] {1.0});
    checkLnToNormalizedProb(new double[] {Math.log(Double.MIN_VALUE)}, new double[] {1.0});
    checkLnToNormalizedProb(new double[] {Math.log(Double.MIN_VALUE) - 100.0}, new double[] {1.0});
    checkLnToNormalizedProb(new double[] {Math.log(0.5), Math.log(0.1), Math.log(0.3), Math.log(0.1)}, new double[] {0.5, 0.1, 0.3, 0.1});
    checkLnToNormalizedProb(new double[] {Math.log(0.5), Math.log(0.1), Math.log(0.3), Math.log(0.1), Double.NEGATIVE_INFINITY}, new double[] {0.5, 0.1, 0.3, 0.1, 0.0});
    checkLnToNormalizedProb(new double[] {Double.NEGATIVE_INFINITY}, new double[] {Double.NaN});
  }

  private void checkLnToNormalizedProb(final double[] logs, final double[] expProb) {
    assertEquals(expProb.length, logs.length);
    final double[] actual = MathUtils.lnToNormaliedProb(logs);
    for (int i = 0; i < logs.length; ++i) {
      if (Double.isNaN(expProb[i])) {
        assertTrue(Double.isNaN(actual[i]));
      } else {
        assertEquals(expProb[i], actual[i], 1e-8);
      }
    }
  }

  public void testDeconvole() {
    final double[] d = MathUtils.deconvolve(new double[] {0.5, 0.3, 0.3}, 0.5);
    assertEquals(0.5952380952380953, d[0], 1e-8);
    assertEquals(0.23809523809523808, d[1], 1e-8);
    assertEquals(0.16666666666666669, d[2], 1e-8);
  }

  public void testGeometric() {
    final double[] g = MathUtils.geometric(2, 1);
    assertEquals(2, g.length);
    assertEquals(0.5, g[0], 1e-8);
    assertEquals(0.5, g[1], 1e-8);
    assertEquals(1, MathUtils.geometric(1, 0.1)[0], 1e-8);
    final double[] h = MathUtils.geometric(3, 0.5);
    final double d = 1 + 0.5 + 0.25;
    assertEquals(1 / d, h[0]);
    assertEquals(0.5 / d, h[1]);
    assertEquals(0.25 / d, h[2]);
  }

  public void testLog() {
    assertEquals(Double.NEGATIVE_INFINITY, MathUtils.log(0.0));
    double x = 1e-100;
    for (int j = 0; j < 10000000; ++j) {
      assertTrue(Math.abs(Math.log(x) - MathUtils.log(x)) < 0.0001);
      x *= 1.00005;
    }
  }

  private void check(final double a, final double b, final double tol, final boolean exp) {
    assertEquals(exp, MathUtils.approxEquals(a, b, tol));
    assertEquals(exp, MathUtils.approxEquals(b, a, tol));
    assertEquals(exp, MathUtils.approxEquals(-a, -b, tol));
    assertEquals(exp, MathUtils.approxEquals(-b, -a, tol));
  }
  public void testDoubleEqualsTolerance() {
    check(0.0, 0.0, 0.0, true);
    check(1.0, 1.001, 0.001, true);
    check(1.0, 0.999, 0.001, true);
    check(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.001, true);

    check(1.0, 1.002, 0.001, false);
    check(1.0, 0.998, 0.001, false);
    check(1.0, Double.NaN, 0.0, false);
    check(1.0, Double.POSITIVE_INFINITY, 0.0, false);
    check(Double.NaN, 1.0, 0.0, false);
    check(Double.NaN, Double.NaN, 0.0, false);
    check(Double.POSITIVE_INFINITY, Double.MAX_VALUE, 0.0, false);
    check(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, false);
  }

  public void testLogExpPlus1() {
    for (double x = -1234.0; x <= 1234.0; ++x) {
      final double y = MathUtils.logExpPlus1(x);
      if (x > 10.0) {
        assertEquals(x, y);
      } else {
        assertEquals(Math.log(Math.exp(x) + 1), y);
      }
    }
    assertEquals(1234567890.1, MathUtils.logExpPlus1(1234567890.1));
    assertEquals(0.0, MathUtils.logExpPlus1(-1234567890.1));
  }

  public void testLogExpMinus1() {
    for (double x = 0.0; x <= 1234.0; ++x) {
      final double y = MathUtils.logExpMinus1(x);
      if (x > 10.0) {
        assertEquals(x, y);
      } else {
        assertEquals("" + x, Math.log(Math.exp(x) - 1), y);
      }
    }
    assertTrue(Double.isInfinite(MathUtils.logExpMinus1(0.0)));
    assertEquals(1234567890.1, MathUtils.logExpMinus1(1234567890.1));
    assertTrue(Double.isNaN(MathUtils.logExpMinus1(-1234567890.1)));
  }

  public void testRound() {
    assertEquals(0, MathUtils.round(0));
    assertEquals(0, MathUtils.round(0.25));
    assertEquals(1, MathUtils.round(0.49999999999999994));
    assertEquals(1, MathUtils.round(0.5));
    assertEquals(1, MathUtils.round(0.999999));
    assertEquals(1, MathUtils.round(1));
  }

  public void testCappedInt() {
    assertEquals("0", MathUtils.cappedInt(0));
    assertEquals("1", MathUtils.cappedInt(1));
    assertEquals("-1", MathUtils.cappedInt(-1));
    assertEquals(String.valueOf(Integer.MAX_VALUE - 1), MathUtils.cappedInt(Integer.MAX_VALUE - 1));
    assertEquals(String.valueOf(Integer.MAX_VALUE), MathUtils.cappedInt(Integer.MAX_VALUE));
    assertEquals(String.valueOf(-Integer.MAX_VALUE), MathUtils.cappedInt(-Integer.MAX_VALUE));
    assertEquals(String.valueOf(-Integer.MAX_VALUE + 1), MathUtils.cappedInt(-Integer.MAX_VALUE + 1));
    assertEquals(String.valueOf(-Integer.MAX_VALUE), MathUtils.cappedInt(-Integer.MAX_VALUE - 1));
    assertEquals(String.valueOf(-Integer.MAX_VALUE), MathUtils.cappedInt(Integer.MIN_VALUE));
    assertEquals(String.valueOf(Integer.MAX_VALUE), MathUtils.cappedInt(Long.MAX_VALUE));
    assertEquals(String.valueOf(-Integer.MAX_VALUE), MathUtils.cappedInt(-Long.MAX_VALUE));
    assertEquals(String.valueOf(-Integer.MAX_VALUE), MathUtils.cappedInt(Long.MIN_VALUE));
  }

  public void testCappedFloat() {
    assertEquals("0.0", MathUtils.cappedFloat(0));
    assertEquals("1.0", MathUtils.cappedFloat(1));
    assertEquals("-1.0", MathUtils.cappedFloat(-1));
    assertEquals("0.0", MathUtils.cappedFloat(Double.MIN_VALUE));
    assertEquals("NaN", MathUtils.cappedFloat(Double.NaN));
    assertEquals(String.valueOf(Integer.MAX_VALUE) + ".0", MathUtils.cappedFloat(Double.MAX_VALUE));
    assertEquals(String.valueOf(Integer.MAX_VALUE) + ".0", MathUtils.cappedFloat(Double.POSITIVE_INFINITY));
    assertEquals(String.valueOf(-Integer.MAX_VALUE) + ".0", MathUtils.cappedFloat(-Double.MAX_VALUE));
    assertEquals(String.valueOf(-Integer.MAX_VALUE) + ".0", MathUtils.cappedFloat(Double.NEGATIVE_INFINITY));
  }

  public void testUnboxNatural() {
    assertEquals(-1, MathUtils.unboxNatural(null));
    assertEquals(0, MathUtils.unboxNatural(0));
    assertEquals(Integer.MAX_VALUE, MathUtils.unboxNatural(Integer.MAX_VALUE));
  }

  public void testMedian() {
    assertTrue(Double.isNaN(MathUtils.median(new double[0])));
    assertEquals(42.0, MathUtils.median(new double[] {42}));
    assertEquals(42.0, MathUtils.median(new double[] {42, 42}));
    assertEquals(42.0, MathUtils.median(new double[] {42, 42}));
    assertEquals(42.0, MathUtils.median(new double[] {42, 42, 42}));
    assertEquals(42.0, MathUtils.median(new double[] {Double.POSITIVE_INFINITY, 8, 42, 78, 12}));
  }

  public void testIsZero() {
    assertTrue(MathUtils.isZero(new int[0]));
    assertTrue(MathUtils.isZero(new int[42]));
    assertFalse(MathUtils.isZero(new int[] {42}));
    assertFalse(MathUtils.isZero(new int[] {0, 42}));
  }
}
