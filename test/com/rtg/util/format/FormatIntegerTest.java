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
package com.rtg.util.format;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for FormatInteger. <p>
 *
 * Run from the command line with:<p>
 *
 * <code>
 * java junit.textui.TestRunner com.rtg.util.format.FormatIntegerTest<br>
 * java junit.swingui.TestRunner com.rtg.util.format.FormatIntegerTest<br>
 * java com.rtg.util.format.FormatIntegerTest<br>
 * </code>
 *
 */
public class FormatIntegerTest extends TestCase {

  /**
   */
  public FormatIntegerTest(final String s) {
    super(s);
  }


  static final int[] TST_DATA =
    new int[]{0, 1, -1, 123, -123, Integer.MAX_VALUE, Integer.MIN_VALUE};


  public static void tst(final FormatInteger fr) {
    // do all the tests in one cycle - instead of calling the method - juri
    for (final int x : TST_DATA) {
      fr.format(x);
      //test that can put into exisiting string
      final StringBuilder sb = new StringBuilder("ABC");
      fr.format(sb, x);
    }
  }


  static final int[] LENGTHS = new int[]{0, 1, 2, 3, 4, 10, 12};


  public void testFormatInteger() {
    for (final int l : LENGTHS) {
      tst(new FormatInteger(l));
      tst(new FormatInteger(l, false));
      tst(new FormatInteger(l, true));
    }
  }


  public static void tstex(final int len, final boolean group, final long val, final String res) {
    final FormatInteger fr = new FormatInteger(len, group);
    final String s = fr.format(val);
    assertEquals(res, s);
    final String bl = fr.blanks();
    assertEquals(len, bl.length());
    assertEquals("", bl.trim());
    final StringBuilder sb = new StringBuilder();
    fr.format(sb, val);
    assertEquals(res, sb.toString());
    final StringBuilder sbl = new StringBuilder();
    fr.blanks(sbl);
    assertEquals("", sbl.toString().trim());
  }


  public void testExamples() {
    tstex(0, false, 0, "#0#");
    tstex(1, false, 0, "0");
    tstex(2, false, 0, " 0");
    tstex(0, false, 123, "#123#");
    tstex(1, false, 123, "#123#");
    tstex(3, false, 123, "123");
    tstex(4, false, 123, " 123");
    tstex(0, false, 1234, "#1234#");
    tstex(1, false, 1234, "#1234#");
    tstex(3, false, 1234, "#1234#");
    tstex(4, false, 1234, "1234");
    tstex(5, false, 1234, " 1234");
    tstex(6, false, 1234567, "#1234567#");
    tstex(7, false, 1234567, "1234567");
    tstex(8, false, 1234567, " 1234567");
    tstex(9, false, 1234567890, "#1234567890#");
    tstex(10, false, 1234567890, "1234567890");
    tstex(11, false, 1234567890, " 1234567890");
    tstex(9, false, 8234567890L, "#8234567890#");
    tstex(10, false, 8234567890L, "8234567890");
    tstex(11, false, 8234567890L, " 8234567890");
    tstex(12, false, 8234567890123L, "#8234567890123#");
    tstex(13, false, 8234567890123L, "8234567890123");
    tstex(14, false, 8234567890123L, " 8234567890123");

    tstex(0, true, -123, "#-123#");
    tstex(3, true, -123, "#-123#");
    tstex(4, true, -123, "-123");
    tstex(5, true, -123, " -123");
    tstex(4, true, -123, "-123");
    tstex(0, true, -1234, "#-1,234#");
    tstex(1, true, -1234, "#-1,234#");
    tstex(3, true, -1234, "#-1,234#");
    tstex(5, true, -1234, "#-1,234#");
    tstex(6, true, -1234, "-1,234");
    tstex(7, true, -1234, " -1,234");
    tstex(9, true, -1234567, "#-1,234,567#");
    tstex(10, true, -1234567, "-1,234,567");
    tstex(11, true, -1234567, " -1,234,567");
    tstex(13, true, -1234567890, "#-1,234,567,890#");
    tstex(14, true, -1234567890, "-1,234,567,890");
    tstex(15, true, -1234567890, " -1,234,567,890");
    tstex(13, true, -8234567890L, "#-8,234,567,890#");
    tstex(14, true, -8234567890L, "-8,234,567,890");
    tstex(15, true, -8234567890L, " -8,234,567,890");
    tstex(17, true, -8234567890123L, "#-8,234,567,890,123#");
    tstex(18, true, -8234567890123L, "-8,234,567,890,123");
    tstex(19, true, -8234567890123L, " -8,234,567,890,123");

    tstex(0, false, 123, "#123#");
    tstex(1, false, 123, "#123#");
    tstex(3, false, 123, "123");
    tstex(4, false, 123, " 123");
    tstex(0, false, 1234, "#1234#");
    tstex(1, false, 1234, "#1234#");
    tstex(3, false, 1234, "#1234#");
    tstex(4, false, 1234, "1234");
    tstex(5, false, 1234, " 1234");
    tstex(6, false, 1234567, "#1234567#");
    tstex(7, false, 1234567, "1234567");
    tstex(8, false, 1234567, " 1234567");
    tstex(9, false, 1234567890, "#1234567890#");
    tstex(10, false, 1234567890, "1234567890");
    tstex(11, false, 1234567890, " 1234567890");
    tstex(9, false, 8234567890L, "#8234567890#");
    tstex(10, false, 8234567890L, "8234567890");
    tstex(11, false, 8234567890L, " 8234567890");
    tstex(12, false, 8234567890123L, "#8234567890123#");
    tstex(13, false, 8234567890123L, "8234567890123");
    tstex(14, false, 8234567890123L, " 8234567890123");

    tstex(0, true, 0, "#0#");
    tstex(1, true, 0, "0");
    tstex(2, true, 0, " 0");
    tstex(0, true, 123, "#123#");
    tstex(1, true, 123, "#123#");
    tstex(3, true, 123, "123");
    tstex(4, true, 123, " 123");
    tstex(0, true, 1234, "#1,234#");
    tstex(1, true, 1234, "#1,234#");
    tstex(3, true, 1234, "#1,234#");
    tstex(4, true, 1234, "#1,234#");
    tstex(5, true, 1234, "1,234");
    tstex(6, true, 1234, " 1,234");
    tstex(8, true, 1234567, "#1,234,567#");
    tstex(9, true, 1234567, "1,234,567");
    tstex(10, true, 1234567, " 1,234,567");
    tstex(12, true, 1234567890, "#1,234,567,890#");
    tstex(13, true, 1234567890, "1,234,567,890");
    tstex(14, true, 1234567890, " 1,234,567,890");
    tstex(12, true, 8234567890L, "#8,234,567,890#");
    tstex(13, true, 8234567890L, "8,234,567,890");
    tstex(14, true, 8234567890L, " 8,234,567,890");
    tstex(16, true, 8234567890123L, "#8,234,567,890,123#");
    tstex(17, true, 8234567890123L, "8,234,567,890,123");
    tstex(18, true, 8234567890123L, " 8,234,567,890,123");
  }


  public void testFormatIntegerMIN() {
    try {
      new FormatInteger(Integer.MIN_VALUE);
      fail("should throw new IllegalArgumentException");
    } catch (final IllegalArgumentException e) {
        assertTrue(e.getMessage().equals("leading digits is negative:" + Integer.MIN_VALUE));
    }
  }

  public void testBits() {

    final String expected = "00000000:00000000:00000111:01111101:01000010:01111110:01110100:11001011";
    assertEquals(expected, FormatInteger.toBits(8234567890123L));

    final String expected2 = "11111111:11111111:11111000:10000010:10111101:10000001:10001011:00110101";
    assertEquals(expected2, FormatInteger.toBits(-8234567890123L));

    final FormatInteger fmt = new FormatInteger(12);

    StringBuilder sb = new StringBuilder();
    assertEquals("            ", fmt.blanks(sb).toString());

    //sb = new StringBuilder();
    assertEquals("                    1245", fmt.format(sb, 1245L).toString());
  }


  public void testFormatIntegerNegative() {

    try {
      new FormatInteger(-100);
      fail("should throw IllegalArgumentException");
    } catch (final IllegalArgumentException e) { }

  }


  public void testFormatNull() {

    FormatInteger fi = new FormatInteger(10);
    try {
      fi.format(null, 10);
      fail("should throw nullPointerException");
    } catch (final NullPointerException e) { }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new TestSuite(FormatIntegerTest.class));
    return suite;
  }


  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}


