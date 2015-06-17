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

import com.rtg.util.Utils;
import com.rtg.util.integrity.Exam.ExamException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for FormatIntegerLeft. <p>
 *
 * Run from the command line with:<p>
 *
 * <code>
 * java junit.textui.TestRunner com.rtg.util.integrity.AssertTest<br>
 * java junit.swingui.TestRunner com.rtg.util.integrity.AssertTest
 * <br>
 * java com.rtg.util.integrity.AssertTest<br>
 * </code>
 *
 */
public class ExamTest extends TestCase {

  /**
   */
  public ExamTest(final String s) {
    super(s);
  }

  public void testTrue() {
    assertTrue(Exam.assertTrue(true));
    try {
      Exam.assertTrue(false);
      fail("AssertionError expected");
    } catch (final ExamException e) {
      assertNull(e.getMessage());
    }
    assertTrue(Exam.assertTrue("ok", true));
    try {
      Exam.assertTrue("oops", false);
      fail("AssertionError expected");
    } catch (final ExamException e) {
      assertEquals("oops", e.getMessage());
    }
  }

  public void testFalse() {
    assertTrue(Exam.assertFalse(false));
    try {
      Exam.assertFalse(true);
      fail("AssertionError expected");
    } catch (final ExamException e) {
      assertNull(e.getMessage());
    }
    assertTrue(Exam.assertFalse("ok", false));
    try {
      Exam.assertFalse("oops", true);
      fail("AssertionError expected");
    } catch (final ExamException e) {
      assertEquals("oops", e.getMessage());
    }
  }

  public void testNotNull() {
    assertTrue(Exam.assertNotNull(new Object()));
    try {
      Exam.assertNotNull(null);
      fail("AssertionError expected");
    } catch (final ExamException e) {
      assertEquals("Is null", e.getMessage());
    }
  }

  public void testMore() {
    assertTrue(Exam.assertEquals(5, 5));

    try {
      Exam.assertEquals(5, 6);
    } catch (final ExamException ex) {
      assertTrue(ex.getMessage().equals("5 != 6"));
    }

    try {
      Exam.assertEquals("a msg", 5, 6);
    } catch (final ExamException ex) {
      assertTrue(ex.getMessage().equals("a msg:5 != 6"));
    }

    assertTrue(Exam.assertEquals(5L, 5L));

    try {
      Exam.assertEquals(5L, 6L);
    } catch (final ExamException ex) {
      assertTrue(ex.getMessage().equals("5 != 6"));
    }
  }

  public void testObject() {
    try {
      Exam.assertEquals(null, "hello");
    } catch (final ExamException ex) {
      assertEquals(null + " != hello", ex.getMessage());
    }

    final String a = "world";
    assertTrue(Exam.assertEquals(a, a));

    try {
      Exam.assertEquals(a, "hello");
    } catch (final ExamException ex) {
      assertEquals("world != hello", ex.getMessage());
    }

    assertTrue(Exam.assertEquals("hello", "hello"));

    assertTrue(Exam.assertEquals(null, null));

    assertTrue(Exam.assertEquals(5, 5));

    assertTrue(Exam.assertEquals(Integer.valueOf(500), Integer.valueOf(500)));
  }

  public void testObjectMsg() {
    try {
      Exam.assertEquals("msg", null, "hello");
    } catch (final ExamException ex) {
      assertEquals("msg:null != hello", ex.getMessage());
    }

    final String a = "world";
    assertTrue(Exam.assertEquals("msg", a, a));

    try {
      Exam.assertEquals("msg", a, "hello");
    } catch (final ExamException ex) {
      assertEquals("msg:world != hello", ex.getMessage());
    }

    assertTrue(Exam.assertEquals("msg", "hello", "hello"));

    assertTrue(Exam.assertEquals("msg", null, null));

    assertTrue(Exam.assertEquals("msg", Integer.valueOf(500), Integer.valueOf(500)));
  }

  public void testIntEquals() {
    assertTrue(Exam.assertEquals(0, 0));
    assertTrue(Exam.assertEquals(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertTrue(Exam.assertEquals(1, 1));
    assertTrue(Exam.assertEquals("ok", 1, 1));
    try {
      Exam.assertEquals(1, 0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("1 != 0", ex.getMessage());
    }
    try {
      Exam.assertEquals("oops", 1, 0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("oops:1 != 0", ex.getMessage());
    }
  }

  public void testDoubleEquals() {
    assertTrue(Exam.assertEquals(0.0, 0.0));
    assertTrue(Exam.assertEquals(Double.NaN, Double.NaN));
    assertTrue(Exam.assertEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    assertTrue(Exam.assertEquals(1.0, 1.0));
    try {
      Exam.assertEquals(1.0, Double.NaN);
      fail();
    } catch (final ExamException ex) {
      assertEquals(1.0 + " != NaN", ex.getMessage());
    }
  }

  public void testDoubleEqualsTolerance() {
    assertTrue(Exam.assertEquals(0.0, 0.0, 0.0));
    assertTrue(Exam.assertEquals(1.0, 1.001, 0.001));
    assertTrue(Exam.assertEquals(1.0, 0.999, 0.001));
    try {
      Exam.assertEquals(1.0, 1.002, 0.001);
      fail();
    } catch (final ExamException ex) {
      assertEquals(1.0 + " != " + 1.002 + ":" + Utils.realFormat(0.001, 3), ex.getMessage());
    }
    try {
      Exam.assertEquals(1.0, 0.998, 0.001);
      fail();
    } catch (final ExamException ex) {
      assertEquals(1.0 + " != " + 0.998 + ":" + Utils.realFormat(0.001, 3), ex.getMessage());
    }
    try {
      Exam.assertEquals(1.0, Double.NaN, 0.0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("NaN not finite number", ex.getMessage());
    }
    try {
      Exam.assertEquals(1.0, Double.POSITIVE_INFINITY, 0.0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("Infinity not finite number", ex.getMessage());
    }
    try {
      Exam.assertEquals(Double.NaN, 1.0, 0.0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("NaN not finite number", ex.getMessage());
    }
    try {
      Exam.assertEquals(Double.POSITIVE_INFINITY, 1.0, 0.0);
      fail();
    } catch (final ExamException ex) {
      assertEquals("Infinity not finite number", ex.getMessage());
    }
  }

  public void testDoubleEqualsToleranceMsg() {
    assertTrue(Exam.assertEquals("", 0.0, 0.0, 0.0));
    try {
      Exam.assertEquals("foobar", 1.0, 1.002, 0.001);
      fail();
    } catch (final ExamException ex) {
      assertEquals("foobar " + 1.0 + " != " + 1.002 + ":" + Utils.realFormat(0.001, 3), ex.getMessage());
    }
  }
  public void testDoubleIsNumber() {
    assertTrue(Exam.assertIsNumber(0.0));
    assertTrue(Exam.assertIsNumber(1.0));
    try {
      Exam.assertIsNumber(Double.NaN);
      fail();
    } catch (final ExamException ex) {
      assertEquals("NaN not finite number", ex.getMessage());
    }
    try {
      Exam.assertIsNumber(Double.POSITIVE_INFINITY);
      fail();
    } catch (final ExamException ex) {
      assertEquals("Infinity not finite number", ex.getMessage());
    }
  }

  public void testAssertSorted() {
    assertTrue(Exam.assertSorted(new double[] {0.0, 1.0, 2.0, Double.POSITIVE_INFINITY}));
    assertTrue(Exam.assertSorted(new double[] {}));
    assertTrue(Exam.assertSorted(new double[] {1.0}));
    try {
      assertTrue(Exam.assertSorted(new double[] {1.0, 0.0, 2.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not sorted:[" + 1.0 + ", " + 0.0 + ", " + 2.0 + "]", ex.getMessage());
    }
    try {
      assertTrue(Exam.assertSorted(new double[] {1.0, 2.0, 0.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not sorted:[" + 1.0 + ", " + 2.0 + ", " + 0.0 + "]", ex.getMessage());
    }
    try {
      assertTrue(Exam.assertSorted(new double[] {1.0, Double.NaN, 0.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not sorted:[" + 1.0 + ", NaN, " + 0.0 + "]", ex.getMessage());
    }
  }

  public void testAssertDescending() {
    assertTrue(Exam.assertDescending(new double[] {Double.POSITIVE_INFINITY, 2.0 , 1.0, 0.0}));
    assertTrue(Exam.assertDescending(new double[] {}));
    assertTrue(Exam.assertDescending(new double[] {1.0}));
    try {
      assertTrue(Exam.assertDescending(new double[] {1.0, 0.0, 2.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not descending:[" + 1.0 + ", " + 0.0 + ", " + 2.0 + "]", ex.getMessage());
    }
    try {
      assertTrue(Exam.assertDescending(new double[] {1.0, 2.0, 0.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not descending:[" + 1.0 + ", " + 2.0 + ", " + 0.0 + "]", ex.getMessage());
    }
    try {
      assertTrue(Exam.assertDescending(new double[] {1.0, Double.NaN, 0.0}));
      fail();
    } catch (final ExamException ex) {
      assertEquals("Not descending:[" + 1.0 + ", NaN, " + 0.0 + "]", ex.getMessage());
    }
  }

  public void testDistribution() {
    assertTrue(Exam.assertDistribution(new double[] {1.0}));
    assertTrue(Exam.assertDistribution(new double[] {0.5, 0.5}));
    assertTrue(Exam.assertDistribution(new double[] {0.0, 0.5, 0.5, 0.0}));
    assertTrue(Exam.assertDistribution(new double[] {1.0}, 1e-6));
    assertTrue(Exam.assertDistribution(new double[] {0.5, 0.5}, 1e-6));
    assertTrue(Exam.assertDistribution(new double[] {0.0, 0.5, 0.5, 0.0}, 1e-6));
    checkDistribution(new double[] {}, "sum=0.0  []");
    checkDistribution(new double[] {0.0}, "sum=0.0  [0.0]");
    checkDistribution(new double[] {-1.0}, "-1.0");
    checkDistribution(new double[] {1.5}, "1.5");
    checkDistribution(new double[] {Double.NaN}, "NaN");
  }

  private void checkDistribution(final double[] a, final String msg) {
    try {
      Exam.assertDistribution(a);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
    try {
      Exam.assertDistribution(a, 1e-6);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
  }

  public void testProbabilities() {
    assertTrue(Exam.assertProbabilities(new double[] {}));
    assertTrue(Exam.assertProbabilities(new double[] {1.0}));
    assertTrue(Exam.assertProbabilities(new double[] {0.5, 0.5}));
    assertTrue(Exam.assertProbabilities(new double[] {0.5, 0.2}));
    assertTrue(Exam.assertProbabilities(new double[] {0.0, 0.5, 0.5, 0.0}));
    assertTrue(Exam.assertProbabilities(new double[] {0.9, 0.0, 0.0, 1.0}));
    checkProbabilities(new double[] {-1.0}, "-1.0");
    checkProbabilities(new double[] {1.5}, "1.5");
    checkProbabilities(new double[] {Double.NaN}, "NaN");
  }

  private void checkProbabilities(final double[] a, final String msg) {
    try {
      Exam.assertProbabilities(a);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
  }

  public void testCummulative() {
    assertTrue(Exam.assertCumulative(new double[] {1.0}));
    assertTrue(Exam.assertCumulative(new double[] {0.5, 1.0}));
    assertTrue(Exam.assertCumulative(new double[] {0.0, 0.5, 1.0, 1.0}));
    checkCummulative(new double[] {}, "[]");
    checkCummulative(new double[] {0.0}, "[0.0]");
    checkCummulative(new double[] {-1.0, 2.0}, "-1.0");
    checkCummulative(new double[] {-0.5, 1.0}, "-0.5");
    checkCummulative(new double[] {1.5, 1.0}, "1.5");
    checkCummulative(new double[] {Double.NaN, 1.0}, "NaN");
  }

  private void checkCummulative(final double[] a, final String msg) {
    try {
      Exam.assertCumulative(a);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
  }

  public void testProbability() {
    assertTrue(Exam.assertProbability(1.0));
    assertTrue(Exam.assertProbability(1.0000001));
    assertTrue(Exam.assertProbability(0.0));
    assertTrue(Exam.assertProbability(0.5));
    checkProbability(-0.0000001, "-0.0000001");
    checkProbability(1.0001, "1.0001");
    checkProbability(Double.NaN, "NaN");
    checkProbability(Double.POSITIVE_INFINITY, "Infinity");
  }

  private void checkProbability(final double p, final String msg) {
    try {
      Exam.assertProbability(p);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
  }

  public void testStrictProbability() {
    assertTrue(Exam.assertProbability(1.0));
    assertTrue(Exam.assertProbability(1.0000001));
    assertTrue(Exam.assertProbability(0.5));
    checkStrictProbability(0.0, "0.0");
    checkStrictProbability(1.0000001, "1.0000001");
    checkStrictProbability(-0.0000001, "-0.0000001");
    checkStrictProbability(1.0001, "1.0001");
    checkStrictProbability(Double.NaN, "NaN");
    checkStrictProbability(Double.POSITIVE_INFINITY, "Infinity");
  }

  private void checkStrictProbability(final double p, final String msg) {
    try {
      Exam.assertStrictProbability(p);
      fail();
    } catch (final ExamException ex) {
      assertEquals(msg, ex.getMessage());
    }
  }

  public static Test suite() {
    return new TestSuite(ExamTest.class);
  }


  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}


