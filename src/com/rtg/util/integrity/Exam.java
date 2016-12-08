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
package com.rtg.util.integrity;

import java.util.Arrays;

import com.rtg.util.Utils;

/**
 * For utilities to do with assertion checking.
 *
 */
public final class Exam {

  private static final double TOLERANCE = 0.0000001;

  // Private to prevent instantiation or extension
  private Exam() { }

  /**
   * Used for assertions that are always run not under control of
   * the assert mechanism.
   *
   */
  public static class ExamException extends RuntimeException {
    ExamException() {
      super();
    }

    /**
     * Creates a new <code>AssertException</code> with the given message.
     *
     * @param msg exception message
     */
    public ExamException(final String msg) {
      super(msg);
    }
  }

  /**
   * A compact way of doing an assertion check that is to be run
   * even when assertion checking not turned on.
   * This makes unit testing etc. more consistent so that they
   * dont depend on assertions being enabled.
   * @param check if false then throw an error.
   * @return true iff the check passed.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertTrue(final boolean check) {
    if (!check) {
      throw new ExamException();
    }
    return check;
  }

  /**
   * A compact way of doing an assertion check that is to be run
   * even when assertion checking not turned on.
   * This makes unit testing etc. more consistent so that they
   * dont depend on assertions being enabled.
   * @param check if true then throw an error.
   * @return true iff the check passed.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertFalse(final boolean check) {
    if (check) {
      throw new ExamException();
    }
    return true;
  }

  /**
   * A compact way of doing an assertion check that is to be run
   * even when assertion checking not turned on.
   * This makes unit testing etc. more consistent so that they
   * dont depend on assertions being enabled.
   * @param check if false then throw an error.
   * @param msg message to be included with the exception.
   * @return true iff the check passed.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertTrue(final String msg, final boolean check) {
    if (!check) {
      throw new ExamException(msg);
    }
    return true;
  }

  /**
   * A compact way of doing an assertion check that is to be run
   * even when assertion checking not turned on.
   * This makes unit testing etc. more consistent so that they
   * dont depend on assertions being enabled.
   * @param check if false then throw an error.
   * @param msg message to be included with the exception.
   * @return true iff the check passed.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertFalse(final String msg, final boolean check) {
    if (check) {
      throw new ExamException(msg);
    }
    return true;
  }

  /**
   * Assert that object isnt null.
   * @param obj object to be checked.
   * @return true iff obj is not null.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertNotNull(final Object obj) {
    if (obj == null) {
      throw new ExamException("Is null");
    }
    return true;
  }

  /**
   * Assert that object isnt null.
   * @param msg a message.
   * @param obj object to be checked.
   * @return true iff obj is not null.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertNotNull(final String msg, final Object obj) {
    if (obj == null) {
      throw new ExamException(msg + ": Is null");
    }
    return true;
  }

  /**
   * Check if two objects are equal.
   * @param msg a message.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b or a.equals(b))
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final String msg, final Object a, final Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || !a.equals(b)) {
      throw new ExamException(msg + ":" + a + " != " + b);
    }
    return true;
  }

  /**
   * Check if two <code>Object</code>s are equal.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final Object a, final Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || !a.equals(b)) {
      throw new ExamException(a + " != " + b);
    }
    return true;
  }

  /**
   * Check if two integers are equal.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final int a, final int b) {
    if (a == b) {
      return true;
    }
    throw new ExamException(a + " != " + b);
  }

  /**
   * Check if two integers are equal.
   * @param msg a message.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final String msg, final long a, final long b) {
    if (a == b) {
      return true;
    }
    throw new ExamException(msg + ":" + a + " != " + b);
  }

  /**
   * Check if two longs are equal.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final long a, final long b) {
    if (a == b) {
      return true;
    }
    throw new ExamException(a + " != " + b);
  }

  /**
   * Check if two doubles are equal.
   * @param a parameter
   * @param b parameter
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final double a, final double b) {
    if (Double.isNaN(a) && Double.isNaN(b)) {
      return true;
    }
    if (a == b) {
      return true;
    }
    throw new ExamException(a + " != " + b);
  }

  /**
   * Check if a double is a finite number (not NaN or infinite).
   * @param a parameter
   * @return true iff a finite.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertIsNumber(final double a) {
    if (!Double.isInfinite(a) && !Double.isNaN(a)) {
      return true;
    }
    throw new ExamException(a + " not finite number");
  }

  /**
   * Check if two doubles are equal to within specified tolerance.
   * @param a parameter
   * @param b parameter
   * @param tolerance allowance for difference between a and b.
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final double a, final double b, final double tolerance) {
    assertIsNumber(a);
    assertIsNumber(b);
    if (a >= (b - tolerance) && a <= (b + tolerance)) {
      return true;
    }
    throw new ExamException(a + " != " + b + ":" + Utils.realFormat(tolerance, 3));
  }

  /**
   * Check if two doubles are equal to within specified tolerance.
   * @param msg a message.
   * @param a parameter
   * @param b parameter
   * @param tolerance allowance for difference between a and b.
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertEquals(final String msg, final double a, final double b, final double tolerance) {
    assertIsNumber(a);
    assertIsNumber(b);
    if (a >= (b - tolerance) && a <= (b + tolerance)) {
      return true;
    }
    throw new ExamException(msg + " " + a + " != " + b + ":" + Utils.realFormat(tolerance, 3));
  }

  /**
   * Check if two doubles are equal to within specified tolerance.
   * @param a parameter
   * @param b parameter
   * @param tolerance allowance for difference between a and b.
   * @return true iff (a == b)
   * @throws AssertionError if the assertion failed
   */
  public static boolean checkEquals(final double a, final double b, final double tolerance) {
    assertIsNumber(a);
    assertIsNumber(b);
    return a >= (b - tolerance) && a <= (b + tolerance);
  }

  /**
   * Check that array in ascending order.
   * @param a array to be checked.
   * @return true if array in correct order.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertSorted(final double[] a) {
    for (int i = 1; i < a.length; ++i) {
      Exam.assertTrue("Not sorted:" + Arrays.toString(a), a[i - 1] < a[i]);
    }
    return true;
  }

  /**
   * Check that array in descending order.
   * @param a array to be checked.
   * @return true if array in correct order.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertDescending(final double[] a) {
    for (int i = 1; i < a.length; ++i) {
      Exam.assertTrue("Not descending:" + Arrays.toString(a), a[i - 1] >= a[i]);
    }
    return true;
  }

  /**
   * Assert that the array contains a valid probability distribution that sums to 1.0.
   * @param da array to be checked.
   * @return true if all correct.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertDistribution(final double[] da) {
    double sum = 0.0;

    for (final double d : da) {
      Exam.assertTrue(Utils.realFormat(d), d >= 0.0 && d <= (1.0 + TOLERANCE) && !Double.isNaN(d));
      sum += d;
    }
    if (Exam.checkEquals(1.0, sum, 0.0001)) {
      return true;
    }
    throw new Exam.ExamException("sum=" + Utils.realFormat(sum) + "  " + Utils.realFormatArray(da));
  }

  /**
   * Assert that the array contains a valid probability distribution that sums to 1.0 within prescribed tolerance.
   * @param da array to be checked.
   * @param tolerance when checking individual values and total.
   * @return true if all correct.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertDistribution(final double[] da, final double tolerance) {
    double sum = 0.0;

    for (final double d : da) {
      Exam.assertTrue(Utils.realFormat(d), d >= 0.0 && d <= (1.0 + tolerance) && !Double.isNaN(d));
      sum += d;
    }
    if (Exam.checkEquals(1.0, sum, tolerance)) {
      return true;
    }
    throw new Exam.ExamException("sum=" + Utils.realFormat(sum) + "  " + Utils.realFormatArray(da));
  }

  /**
   * Assert that the array contains valid probabilities.
   * @param da array to be checked.
   * @return true if all correct.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertProbabilities(final double[] da) {
    for (final double d : da) {
      Exam.assertProbability(d);
    }
    return true;
  }

  /**
   * Assert that the array contains a cumulative monotone increasing distribution where each entry is a valid
   * probability and the last element is 1.0.
   * @param da array to be checked.
   * @return true if all correct.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertCumulative(final double[] da) {
    final double tolerance = 0.000001;
    Exam.assertTrue(Utils.realFormatArray(da), da.length > 0);
    double prev = Double.NEGATIVE_INFINITY;
    for (final double d : da) {
      Exam.assertTrue(Utils.realFormat(d), d >= 0.0 && d <= (1.0 + tolerance) && !Double.isNaN(d));
      Exam.assertTrue(prev <= d);
      prev = d;
    }
    if (checkEquals(1.0, prev, tolerance)) {
      return true;
    }
    throw new Exam.ExamException(Utils.realFormatArray(da));
  }

  /**
   * Assert that p is a probability (can be 0.0 and 1.0 with a tolerance).
   * @param p value to be checked.
   * @return true if ok.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertProbability(final double p) {
    if (p >= 0.0 && p <= 1.000001 && !Double.isNaN(p)) {
      return true;
    }
    throw new Exam.ExamException(Utils.realFormat(p));
  }

  /**
   * Assert that p is a probability (strictly greater than 0.0).
   * @param p value to be checked.
   * @return true if ok.
   * @throws AssertionError if the assertion failed
   */
  public static boolean assertStrictProbability(final double p) {
    if (p > 0.0 && p <= 1.0 && !Double.isNaN(p)) {
      return true;
    }
    throw new Exam.ExamException(Utils.realFormat(p));
  }

  /**
   * Invokes an integrity check on an object if it is an instance of
   * Integrity.
   *
   * @param o an <code>Object</code> that may or may not implement the Integrity interface.
   * @return true, as with <code>Integrity.integrity</code>.
   */
  public static boolean integrity(Object o) {
    if ((o != null) && (o instanceof Integrity)) {
      ((Integrity) o).integrity();
    }
    return true;
  }

  /**
   * Invokes a global integrity check on an object if it is an instance of
   * Integrity.
   *
   * @param o an <code>Object</code> that may or may not implement the Integrity interface.
   * @return true, as with <code>Integrity.integrity</code>.
   */
  public static boolean globalIntegrity(Object o) {
    if ((o != null) && (o instanceof Integrity)) {
      ((Integrity) o).globalIntegrity();
    }
    return true;
  }
}
