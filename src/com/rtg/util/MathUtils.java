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

/**
 */
public final class MathUtils {

  /** The natural logarithm of 10 */
  public static final double LOG_10 = Math.log(10.0);
  /** The natural logarithm of 2 */
  public static final double LOG_2 = Math.log(2);
  /** The natural logarithm of e */
  public static final double LOG10_E = Math.log10(Math.E);

  private MathUtils() { //prevent instantiation
  }

  /**
   * Compute -log of a binomial of form p^n*(1-p)^(N-n)binomial(N,n).
   *
   * @param p probability
   * @param nn total population
   * @param n marked subpopulation
   * @return -log of a binomial of form p^n*(1-p)^(N-n)binomial(N,n)
   */
  public static double logBinomial(final double p, final int nn, final int n) {
    assert p >= 0.0 && p <= 1.0;
    assert n >= 0;
    assert nn >= 0;
    final int m = nn - n;
    if (p == 0.0) {
      if (n == 0) {
        return 0.0;
      }
      throw new IllegalArgumentException("if probability is 0.0 then count must be 0. p:" + p
        + " N:" + nn + " n:" + n);
    }
    if (p == 1.0) {
      if (m == 0) {
        return 0.0;
      }
      throw new IllegalArgumentException("if probability is 1.0 then count must be 0. p:" + p
        + " N:" + nn + " n:" + n);
    }
    final double res = n * Math.log(p) + m * Math.log(1.0f - p) + logBinomial(nn, n);
    assert res <= 0;
    return -res;
  }

  /**
   * Compute log binomial(N,n).
   *
   * @param nn total count.
   * @param n subset count.
   * @return log binomial(N,n)
   */
  public static double logBinomial(final int nn, final int n) {
    assert n >= 0;
    assert nn >= n;
    if (nn <= 1 || n == 0 || n == nn) {
      return 0.0;
    }
    if (n == 1 || n == (nn - 1)) {
      return Math.log(nn);
    }
    if (n == 2 || n == (nn - 2)) {
      return Math.log((double) nn * (nn - 1) / 2.0);
    }
    return logFactorial(nn) - logFactorial(n) - logFactorial(nn - n);
  }

  private static final double C0 = Math.PI / 3.0;

  private static final double C1 = Math.PI * 2.0;

  private static final int EXPONENT_OFFSET = 52;
  private static final int EXPONENT_BIAS = 1023;

  private static final int BITS = 16;
  private static final double DIV = 1L << BITS;
  private static final int MASK = (1 << BITS) - 1;
  private static final double[] LOG_TABLE = new double[1 << BITS];

  /** Cache low values to get errors lower. */
  private static final double[] LOG_F = new double[31];
  static {
    double f = 1.0;
    for (int i = 0; i < LOG_F.length; ++i) {
      LOG_F[i] = Math.log(f);
      f = f * (i + 1);
    }

    // The rounding below is heuristic and not strictly necessary
    // to get an approximate result.  By making the rounding 0
    // for entry 0, ensure log(1)=0.
    for (int k = 0; k < 1 << BITS; ++k) {
      final double round = (k & 1) == 0 ? 0 : 0.5;
      LOG_TABLE[k] = Math.log(1 + (k + round) / DIV);
    }
  }

  /**
   * Compute <code>logFactorial(N)</code>. Worst relative error at 31 = -9.1E-8
   *
   * @param n number
   * @return log n!
   */
  public static double logFactorial(final int n) {
    assert n >= 0;
    final double res;
    if (n < LOG_F.length) {
      res = LOG_F[n];
    } else {
      // Use Gospers Formula.
      res = Math.log(C0 + n * C1) * 0.5 + (Math.log(n) - 1.0) * n;
    }
    assert res >= 0.0 && Double.isFinite(res);
    return res;
  }

  /**
   * Get the smallest long which is a power of 2 and &gt; x. Undefined for negative
   * numbers or numbers so large that there is no positive power of 2 available.
   *
   * @param x number being checked.
   * @return integer which is a power of 2 &gt; x.
   */
  public static long ceilPowerOf2(final long x) {
    if (x <= 0 || x >= (1L << 62)) {
      if (x == 0) {
        return 1;
      }
      throw new IllegalArgumentException("Number out of range:" + x);
    }
    return Long.highestOneBit(x) << 1;
  }

  /**
   * Get the smallest power of 2 &gt; x. Undefined for negative numbers or numbers
   * so large that there is no positive power of 2 available.
   *
   * @param x number being checked.
   * @return power of 2 &gt; x.
   */
  public static int ceilPowerOf2Bits(final long x) {
    if (x >= (1L << 62) || x < 0) {
      throw new IllegalArgumentException("Number out of range:" + x);
    }
    long i = 1L;
    int n = 0;
    while (i <= x) {
      assert i > 0;
      assert n >= 0;
      ++n;
      i = i << 1;
    }
    assert 1L << n == i;
    return n;
  }

  /**
   * Renormalize an array to make it a probability distribution.
   *
   * @param a relative frequencies
   * @return corresponding probability distribution
   */
  public static double[] renormalize(final int[] a) {
    double s = 0;
    for (final int v : a) {
      s += v;
    }
    s = 1 / s;
    final double[] p = new double[a.length];
    for (int k = 0; k < a.length; ++k) {
      p[k] = s * a[k];
    }
    return p;
  }

  /**
   * Renormalize an array to make it a probability distribution.
   *
   * @param a relative frequencies
   * @return corresponding probability distribution
   */
  public static double[] renormalize(final double[] a) {
    double s = 0;
    for (final double v : a) {
      s += v;
    }
    s = 1 / s;
    final double[] p = new double[a.length];
    for (int k = 0; k < a.length; ++k) {
      p[k] = s * a[k];
    }
    return p;
  }

  /**
   * Takes a probability distribution and reduces higher-order terms by
   * contributions arising from combinations of lower order terms. It
   * assumes the input distribution is suitable for this operation.
   *
   * @param p original distribution
   * @param rate decay rate
   * @return modified distribution
   */
  public static double[] deconvolve(final double[] p, final double rate) {
    final double[] r = Arrays.copyOf(p, p.length);
    for (int k = 0; k < p.length; ++k) {
      for (int j = 0; j < p.length; ++j) {
        if (k + j < p.length) {
          r[k + j] -= rate * p[k] * p[j];
        }
      }
    }
    // renormalize
    double sum = 0;
    for (final double q : r) {
      sum += q;
    }
    sum = 1 / sum;
    for (int k = 0; k < r.length; ++k) {
      r[k] *= sum;
    }
    return r;
  }

  /**
   * Compute an approximately geometric distribution on n items with decay
   * parameter p.
   *
   * @param n number of items
   * @param p decay rate (0&lt;p&lt;=1)
   * @return distribution
   */
  public static double[] geometric(final int n, final double p) {
    if (p <= 0 || p > 1) {
      throw new IllegalArgumentException();
    }
    final double[] r = new double[n];
    double v = 1.0;
    for (int k = 0; k < n; ++k, v *= p) {
      r[k] = v;
    }
    return renormalize(r);
  }

  /**
   * Compute an approximation to the natural logarithm. Assumes
   * parameter is positive and finite.
   *
   * @param x parameter
   * @return <code>ln(x)</code>
   */
  public static double log(final double x) {
    assert x >= 0 && Double.isFinite(x);
    if (x == 0.0) {
      return Double.NEGATIVE_INFINITY;
    }
    final long t = Double.doubleToRawLongBits(x);
    final long lg = (t >>> EXPONENT_OFFSET) - EXPONENT_BIAS;
    final int mantissa = (int) (t >> (EXPONENT_OFFSET - BITS));
    final double mlg = LOG_TABLE[mantissa & MASK];
    return mlg + lg * LOG_2;
  }

  /**
   * Get a phred-scaled quality value corresponding to the supplied error rate.
   *
   * @param error the error rate, between 0 and 1
   * @return the corresponding phred scaled quality.
   */
  public static double phred(double error) {
    return -10 * Math.log10(error);
  }

  /**
   * Check if two doubles are equal to within specified tolerance.
   * @param a parameter
   * @param b parameter
   * @param tolerance allowance for difference between a and b.
   * @return true iff (a ~= b)
   */
  public static boolean approxEquals(final double a, final double b, final double tolerance) {
    if (Double.isNaN(a) || Double.isNaN(b)) {
      return false;
    }
    if (a == b) {
      return true;
    }
    if (Double.isInfinite(a) || Double.isInfinite(b)) {
      return false;
    }
    if (a >= (b - tolerance) && a <= (b + tolerance)) {
      return true;
    }
    return false;
  }

  /**
   * Calculates <i>ln</i>(<i>e</i><sup>x</sup> + 1).  Approximates that with x for x &gt; 10.
   * @param x a number
   * @return <code>ln(e^x + 1)</code>
   */
  public static double logExpPlus1(double x) {
    return x > 10.0 ? x : Math.log(Math.exp(x) + 1);
  }

  /**
   * Calculates <i>ln</i>(<i>e</i><sup>x</sup> - 1).  Approximates that with x for x &gt; 10 to avoid overflow.
   * @param x a number
   * @return <code>ln(e^x - 1)</code>
   */
  public static double logExpMinus1(double x) {
    return x > 10.0 ? x : Math.log(Math.exp(x) - 1);
  }

  /**
   * Normalize an array of ln values to probabilities.
   * @param logs array of log values to be normalized.
   * @return normalized probabilities.
   */
  public static double[] lnToNormaliedProb(final double[] logs) {
    final double[] prob = new double[logs.length];
    double max = logs[0];
    for (int i = 1; i < logs.length; ++i) {
      if (max < logs[i]) {
        max = logs[i];
      }
    }
    double sum = 0.0;
    for (int i = 0; i < logs.length; ++i) {
      final double v = Math.exp(logs[i] - max);
      prob[i] = v;
      sum += v;
    }
    for (int i = 0; i < logs.length; ++i) {
      prob[i] = prob[i] / sum;
    }
    return prob;
  }

  /**
   * Our own implementation of Math.round due to difference between java 1.6 and 1.7 implementations
   * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6430675">Java bug 6430675</a>
   *
   * Using java 1.6 implementation of Math.round
   * defined as floor of 0.5d plus value.
   * @param val value to round
   * @return rounded value
   */
  public static long round(double val) {
    return (long) Math.floor(val + 0.5d);
  }

  /**
   * Get Hoeffding probability number, in phred space, with protection for no trials.
   * @param trials the number of trials measured
   * @param observed the observed number of trials matching the condition
   * @param prob the expected probability of a trial matching the condition
   * @return the phred-scaled Hoeffding probability, or null if there are zero trials
   */
  public static Double hoeffdingPhred(double trials, double observed, double prob) {
    if (trials == 0) {
      return null;
    }
    final double hoeffding = hoeffdingLn(trials, observed, prob);
    return lnToPhred(hoeffding);
  }

  /**
   * Get Hoeffding probability number, in ln space
   * @param trials the number of trials measured
   * @param observed the observed number of trials matching the condition
   * @param prob the expected probability of a trial matching the condition
   * @return the ln-scaled Hoeffding probability, or null if there are zero trials
   */
  public static double hoeffdingLn(double trials, double observed, double prob) {
    return -2 * Math.pow(trials * prob - observed, 2) / trials;
  }

  /**
   * turn a log probability into <code>Phred</code> score
   * @param val value to convert
   * @return the result
   */
  public static double lnToPhred(double val) {
    return -10 * LOG10_E * val;
  }

  /**
   * Turn a phred score into a probability
   *
   * @param phred the phred score to convert
   * @return the probability value
   */
  public static double phredToProb(double phred) {
    return Math.pow(10.0, -phred / 10.0);
  }

  private static final String OVER = String.valueOf(Integer.MAX_VALUE);
  private static final String UNDER = String.valueOf(-Integer.MAX_VALUE); // Deliberately not Integer.MIN_VALUE

  /**
   * Return a long as a string but capped as a 32-bit signed value.
   * (e.g. for compatibility with BCF).  Note 0x80000000 is reserved
   * in BCF for missing value.
   * @param val value
   * @return string representation
   */
  public static String cappedInt(final long val) {
    if (val >= Integer.MAX_VALUE) {
      return OVER;
    } else if (val <= Integer.MIN_VALUE) {
      return UNDER;
    }
    return Long.toString(val);
  }


  /**
   * Return a double as a string but capped as within the range of a 32-bit signed integer.
   * @param val value
   * @return string representation
   */
  public static String cappedFloat(double val) {
    if (val >= Integer.MAX_VALUE) {
      return OVER + ".0";
    } else if (val <= Integer.MIN_VALUE) {
      return UNDER + ".0";
    }
    return Utils.realFormat(val, 1);
  }

  /**
   * Given non-negative or null return primitive int or -1.
   * @param x value to get primitive for
   * @return -1 for null otherwise <code>x</code>.
   */
  public static int unboxNatural(final Integer x) {
    return x == null ? -1 : x;
  }

  private static double median(final double[] values, int start, int end) {
    final int length = end - start;
    if (length == 0) {
      return Double.NaN;
    }
    final double median;
    if (length % 2 == 0) {
      median = (values[start + length / 2] + values[start + length / 2 - 1]) / 2;
    } else {
      median = values[start + length / 2];
    }
    return median;
  }

  /**
   * Compute the median of a set of values. Warning: this procedure will sort the array.
   * @param values array of values
   * @return median
   */
  public static double median(final double[] values) {
    Arrays.sort(values);
    return median(values, 0, values.length);
  }

  /**
   * Compute the quartiles of a set of values. Warning: this procedure will sort the array.
   * @param values array of values
   * @return array of three elements: first, second, and third quartiles
   */
  public static double[] quartiles(final double[] values) {
    Arrays.sort(values);
    final int qlength = values.length / 2;
    final double[] result = new double[3];
    result[0] = median(values, 0, qlength);
    result[1] = median(values, 0, values.length);
    result[2] = median(values, values.length - qlength, values.length);
    return result;
  }

  /**
   * Test if an array is all zero.
   * @param counts array to test
   * @return true iff the array is all zero
   */
  public static boolean isZero(final int[] counts) {
    for (final int count : counts) {
      if (count != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return the largest integer in an array.
   * @param s array
   * @return maximum
   */
  public static int max(final int... s) {
    int max = s[0];
    for (final int v : s) {
      if (v > max) {
        max = v;
      }
    }
    return max;
  }
}
