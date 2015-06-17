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


import junit.framework.TestCase;

/**
 * Tests FormatReal. Run from the command line with:<p>
 *
 * <code>
 * java junit.textui.TestRunner com.rtg.util.format.FormatRealTest<br>
 * java junit.swingui.TestRunner com.rtg.util.format.FormatRealTest<br>
 * java com.rtg.util.format.FormatRealTest<br>
 * </code>
 *
 */
public class FormatRealTest extends TestCase {

  public FormatRealTest(final String name) {
    super(name);
  }

  @Override
  public void setUp() {
    clearStrBuffer();
  }

  @Override
  public void tearDown() {
    mStrBuff = null;
    mFReal = null;
  }


  /** Clear StringBuilder object for test methods  */
  private void clearStrBuffer() {
    mStrBuff = new StringBuilder();
  }


  /**
   * Tests constructor and format methods FormatReal constructor is
   * sent # of places to left of decimal and # of spaces to right of
   * decimal as integer parameters
   */
  public void testFormatDoubles() {
    mFReal = new FormatReal(2, 3);
    mStr = mFReal.format(mStrBuff, 14.7).toString();
    assertEquals("Should be equal", "14.700", mStr);

    mFReal = new FormatReal(2, 1);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, -4.75).toString();
    // Should round decimal up to 1st place
    assertEquals("Should be equal", "-4.8", mStr);

    mFReal = new FormatReal(1, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, -23.27).toString();
    assertEquals("Should be equal", "#-23.27#", mStr);

    mFReal = new FormatReal(2, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.MIN_VALUE).toString();
    assertEquals("Should be equal", "0.00", mStr.trim()); // it will round MIN to zero

    mFReal = new FormatReal(2, 5);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.MAX_VALUE).toString();
    //System.err.println("MAX >>> " + mStr + mStr.substring(mStr.indexOf(".")+1 ).length());
    assertEquals("Should be the size of 6 ", 6, mStr.substring(mStr.indexOf(".") + 1).length());


  }


  /**
   * Tests constructor and format methods FormatReal constructor is
   * sent # of places to left of decimal and # of spaces to right of
   * decimal as integer parameters
   */
  public void testFormatFloats() {
    mFReal = new FormatReal(2, 3);
    mStr = mFReal.format(mStrBuff, 14.7f).toString();
    assertEquals("Should be equal", "14.700", mStr);

    mFReal = new FormatReal(1, 1);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, 4.75f).toString();
    // Should round decimal up to 1st place
    assertEquals("Should be equal", "4.8", mStr);

    mFReal = new FormatReal(2, 10);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, -3.2e10f).toString();
    assertEquals("Should be equal", "#-32000000000.0000000000#", mStr);

    mFReal = new FormatReal(2, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.MIN_VALUE).toString();
    assertEquals("Should be equal", " 0.00", mStr); // it will round MIN to zero


    mFReal = new FormatReal(5, 5);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.MAX_VALUE).toString();

    //System.err.println("MAX <<< " + mStr + str.substring(mStr.indexOf(".")+1 ).length());
    assertEquals("Should be the size of 6 ", 6, mStr.substring(mStr.indexOf(".") + 1).length());

  }


  /** Sends negative values to constructor  */
  public void testInvalidConstructor() {
    // Negative # of places to left of decimal specified
    try {
      mFReal = new FormatReal(-1, 1);
      fail("should throw IllegalArgumentException");
    } catch (final IllegalArgumentException e) { }
  }


  /** Test with 0 decimal places  */
  public void testConstructorZeroDecimalPlaces() {
    mFReal = new FormatReal(2, 0);
    mStr = mFReal.format(mStrBuff, 14.7).toString();
    assertEquals("Should be equal", "15.", mStr);
  }


  /**
   * Test with the negative input expected : uncaught
   * NegativeArraySizeException
   */
  public void testConstructorNegativeInput() {
    try {
      mFReal = new FormatReal(-100, 0);
      fail("should throw illegal argument exception");
    } catch (final IllegalArgumentException e) { }
  }



  /**
   * Tests format method with null input expected uncaught
   * NullPointerException
   */
  public void testFormatNull() {
    mFReal = new FormatReal(2, 2);
    try {
      mStrBuff = mFReal.format(null, 0.0d);
      fail("should throw exception");
    } catch (final RuntimeException e) { }
  }


  /** Tests format method with NaN  */
  public void testFormatNaN() {
    mFReal = new FormatReal(2, 2);
    mStr = mFReal.format(mStrBuff, Float.NaN).toString();
    assertEquals("Should be NaN", mStr, "  NaN");
    mStrBuff = new StringBuilder();
    mStr = mFReal.format(mStrBuff, Float.NaN).toString();
    assertEquals("Should be NaN", mStr, "  NaN");
  }



  /**
   * Tests format method with Pos/Neg Infinities Is Infinity argument
   * legal?
   */
  public void testFormatInfinityDP() {
    mFReal = new FormatReal(2, 2);
    // Test using doubles
    mStr = mFReal.format(mStrBuff, Float.POSITIVE_INFINITY).toString();
    assertEquals("Should be Infinity", "#Infinity#", mStr);
  }


  /**
   * Tests format method with Pos/Neg Infinities Is Infinity argument
   * legal?
   */
  public void testFormatInfinityDN() {
    mFReal = new FormatReal(2, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.NEGATIVE_INFINITY).toString();
    assertEquals("Should be Infinity", "#-Infinity#", mStr);
  }


  /**
   * Tests format method with Pos/Neg Infinities Is Infinity argument
   * legal?
   */
  public void testFormatInfinitySP() {
    mFReal = new FormatReal(2, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.POSITIVE_INFINITY).toString();

    assertEquals("Should be Infinity", "#Infinity#", mStr);
  }



  /**
   * Tests format method with Pos/Neg Infinities Is Infinity argument
   * legal?
   */
  public void testFormatInfinitySN() {
    mFReal = new FormatReal(2, 2);
    clearStrBuffer();
    mStr = mFReal.format(mStrBuff, Float.NEGATIVE_INFINITY).toString();

    assertEquals("Should be negative Infinity", "#-Infinity#", mStr);
  }

  private StringBuilder mStrBuff;
  private String mStr;
  private FormatReal mFReal;
}
