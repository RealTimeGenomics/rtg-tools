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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;

import junit.framework.TestCase;


/**
 * JUnit tests for Utils.
 *
 */
public class UtilsTest extends TestCase {

  /**
   */
  public UtilsTest(final String name) {
    super(name);
  }

  public void testSwapEndian() {
    assertEquals(123456, Utils.swapEndian(Utils.swapEndian(123456)));
    assertEquals(123456L, Utils.swapEndian(Utils.swapEndian(123456L)));
  }

  private void checkBitsSep(final String str, final long v) {
    assertEquals(str, Utils.toBitsSep(v));
    assertEquals(v, Utils.fromBits(str));
  }

  public void testToBitsSep() {
    checkBitsSep("11111111:11111111:11111111:11111111:11111111:11111111:11111111:11111111", -1L);
    checkBitsSep("00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000", 0L);
    checkBitsSep("10000000:00000000:00000000:00000000:00000000:00000000:00000000:00000000", Long.MIN_VALUE);
    checkBitsSep("00000000:00000000:00000000:00000000:00000000:00000000:00000000:00000001", 1L);
    checkBitsSep("00000000:00000000:00000000:00000000:00000000:00000000:00000001:10000000", 3L << 7);

  }

  private void checkBits(final String str, final long v) {
    assertEquals(str, Utils.toBits(v));
    assertEquals(v, Utils.fromBits(str));
  }

  public void testToBits() {
    checkBits("1111111111111111111111111111111111111111111111111111111111111111", -1L);
    checkBits("0", 0L);
    checkBits("1000000000000000000000000000000000000000000000000000000000000000", Long.MIN_VALUE);
    checkBits("1", 1L);
    checkBits("110000000", 3L << 7);

  }

  public void testToBitsA() {
    final String exp = ""
        + "[0]1111111111111111111111111111111111111111111111111111111111111111 "
        + "[1]0000000000000000000000000000000000000000000000000000000000000001 "
        + "[2]0000000000000000000000000000000000000000000000000000000000000000 "
        ;
    assertEquals(exp, Utils.toBits(new long[] {-1, 1, 0}));
  }
  public void testToBitsLen() {
    assertEquals("000", Utils.toBits(Utils.fromBits(""), 3));
    assertEquals("101", Utils.toBits(Utils.fromBits("101"), 3));
    assertEquals("01", Utils.toBits(Utils.fromBits("101"), 2));
    assertEquals("1", Utils.toBits(Utils.fromBits("101"), 1));
    assertEquals("0000000000000000000000000000000000000000000000000000000000000101", Utils.toBits(Utils.fromBits("101"), 64));

    try {
      Utils.toBits(0, 0);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=0", e.getMessage());
    }
    try {
      Utils.toBits(0, 65);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=65", e.getMessage());
    }
  }


  public void testToBits2() {
    assertEquals("000:000", Utils.toBits2(Utils.fromBits(""), 3));
    assertEquals("000:101", Utils.toBits2(Utils.fromBits("101"), 3));
    assertEquals("01:01", Utils.toBits2(Utils.fromBits("101"), 2));
    assertEquals("0:1", Utils.toBits2(Utils.fromBits("101"), 1));
    assertEquals("00000000000000000000000000000000:00000000000000000000000000000101", Utils.toBits2(Utils.fromBits("101"), 32));

    try {
      Utils.toBits2(0, 0);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=0", e.getMessage());
    }
    try {
      Utils.toBits2(0, 33);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=33", e.getMessage());
    }
  }


  public void testFromBitsLong() {
    assertEquals(0, Utils.fromBits(""));
    assertEquals(0, Utils.fromBits("  "));
    assertEquals(0, Utils.fromBits(": "));
    assertEquals(0, Utils.fromBits("0: 0"));
    assertEquals(1, Utils.fromBits("1"));
    assertEquals(2, Utils.fromBits("10"));
    assertEquals(7, Utils.fromBits("111"));
  }

  public void testFromBitsBad() {
    try {
      Utils.fromBits("1:11111111:11111111:11111111:11111111:11111111:11111111:11111111:11111111");
      fail();
    } catch (final RuntimeException e) {
      assertEquals("bit string too long:65", e.getMessage());
    }
    try {
      Utils.fromBits("x");
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Invalid character in bit string:x", e.getMessage());
    }
  }

  public void testPairHash() {
    assertEquals(1, Utils.pairHash(0, 0));
    assertEquals(2, Utils.pairHash(0, -1));
    assertEquals(3, Utils.pairHash(1, 0));
    assertEquals(4, Utils.pairHash(0, 1));
    assertEquals(5, Utils.pairHash(-1, 0));
    assertEquals(6, Utils.pairHash(0, -2));
    assertEquals(7, Utils.pairHash(1, -1));
    assertEquals(8, Utils.pairHash(2, 0));
    assertEquals(9, Utils.pairHash(1, 1));
    assertEquals(10, Utils.pairHash(0, 2));
    assertEquals(11, Utils.pairHash(-1, 1));
    assertEquals(12, Utils.pairHash(-2, 0));
    assertEquals(13, Utils.pairHash(-1, -1));
  }

  public void testHash() {
    assertEquals(0, Utils.hash(new Object[] {}));
    assertEquals(43, Utils.hash(new Object[] {null}));
    assertEquals(3657, Utils.hash(new Object[] {null, null}));
    assertEquals(26743643, Utils.hash(new Object[] {null, null, null}));
    assertEquals(29349293, Utils.hash(new Object[] {null, 1, null}));
    assertEquals(29333972, Utils.hash(new Object[] {1, null, null}));
    assertTrue(Utils.hash(new Object[] {}) != Utils.hash(new Object[] {null}));
    assertTrue(Utils.hash(new Object[] {1, 2, 3}) != Utils.hash(new Object[] {1, 2, 2}));
    assertTrue(Utils.hash(new Object[] {1, 2, 3}) != Utils.hash(new Object[] {1, 2}));
    assertTrue(Utils.hash(new Object[] {1, 2, 3}) != Utils.hash(new Object[] {1, 2, null}));
    assertTrue(Utils.hash(new Object[] {1, 2, 3}) != Utils.hash(new Object[] {1, 2, 3.0}));
  }

  public void testAppend0() {
    final String[] a = {};
    final String[] b = {};
    final String[] c = Utils.append(a, b);
    assertEquals("[]", Arrays.toString(c));
  }

  public void testAppend1() {
    final String[] a = {"a", "b", "c"};
    final String[] b = {"d", "e", "f"};
    final String[] c = Utils.append(a, b);
    assertEquals("[a, b, c, d, e, f]", Arrays.toString(c));
  }

  public void testReverse0() {
    final byte[] a = new byte[0];
    Utils.reverseInPlace(a); // should not throw an exception
    assertEquals(0, a.length);
  }

  public void testReverse1() {
    final byte[] a = new byte[1];
    a[0] = 9;
    Utils.reverseInPlace(a);
    assertEquals(9, a[0]);
  }

  public void testReverse2() {
    final byte[] a = {9, 11};
    Utils.reverseInPlace(a);
    assertEquals(11, a[0]);
    assertEquals(9, a[1]);
  }

  public void testReverseOdd() {
    final byte[] a = {11, 12, 13, 14, 15};
    Utils.reverseInPlace(a);
    assertEquals(15, a[0]);
    assertEquals(14, a[1]);
    assertEquals(13, a[2]);
    assertEquals(12, a[3]);
    assertEquals(11, a[4]);

    Utils.reverseInPlace(a, 1, 3);
    assertEquals(15, a[0]);
    assertEquals(12, a[1]);
    assertEquals(13, a[2]);
    assertEquals(14, a[3]);
    assertEquals(11, a[4]);
  }

  public void testDecimalPlaces() {
    assertEquals(1, Utils.decimalPlaces(0.0));
    assertEquals(1, Utils.decimalPlaces(0.1));
    assertEquals(2, Utils.decimalPlaces(0.01));
    assertEquals(3, Utils.decimalPlaces(0.001));
    assertEquals(4, Utils.decimalPlaces(0.0001));
    assertEquals(5, Utils.decimalPlaces(0.00001));
    assertEquals(1, Utils.decimalPlaces(0.000000001));
    assertEquals(1, Utils.decimalPlaces(1.0));
    assertEquals(1, Utils.decimalPlaces(1.1));
    assertEquals(2, Utils.decimalPlaces(1.01));
    assertEquals(3, Utils.decimalPlaces(1.001));
    assertEquals(4, Utils.decimalPlaces(1.0001));
    assertEquals(5, Utils.decimalPlaces(1.00001));
    assertEquals(1, Utils.decimalPlaces(1.000000001));
    assertEquals(10, Utils.decimalPlaces(1.0 / 3.0));
  }

  private void checkIntWrite(final int i, final String exp) throws IOException {
    final OutputStream out = new ByteArrayOutputStream();
    Utils.intWrite(out, i);
    assertEquals(exp, out.toString());
  }

  public void testIntWrite() throws IOException {
    checkIntWrite(0, "0");
    checkIntWrite(1, "1");
    checkIntWrite(-1, "-1");
    checkIntWrite(9, "9");
    checkIntWrite(10, "10");
    checkIntWrite(12, "12");
    checkIntWrite(99, "99");
    checkIntWrite(100, "100");
    checkIntWrite(101, "101");
    checkIntWrite(Integer.MAX_VALUE, "2147483647");
    checkIntWrite(Integer.MIN_VALUE, "-2147483648");
  }

  public void testRealFormat() {
    assertEquals("null", Utils.realFormat(null));
    assertEquals("0.0", Utils.realFormat(0.0));
    assertEquals("0.5", Utils.realFormat(0.5));
    assertEquals("-1.5", Utils.realFormat(-1.5));
    assertEquals("1.5", Utils.realFormat(1.5));
    assertEquals("1234.5", Utils.realFormat(1234.5));
    assertEquals("-1234.5", Utils.realFormat(-1234.5));
    assertEquals("Infinity", Utils.realFormat(Double.POSITIVE_INFINITY));
    assertEquals("-Infinity", Utils.realFormat(Double.NEGATIVE_INFINITY));
    assertEquals("NaN", Utils.realFormat(Double.NaN));

    assertEquals("0.01", Utils.realFormat(0.01));
    assertEquals("0.05", Utils.realFormat(0.05));

    assertEquals("0.001", Utils.realFormat(0.001));
    assertEquals("0.005", Utils.realFormat(0.005));

    assertEquals("1.001", Utils.realFormat(1.001));
    assertEquals("1.005", Utils.realFormat(1.005));
  }

  public void testNegZero() {
    assertTrue(Utils.negZero("-0", 0));
    assertTrue(Utils.negZero("-0.0", 1));
    assertTrue(Utils.negZero("-0.00", 2));

    assertFalse(Utils.negZero("0", 0));
    assertFalse(Utils.negZero("0.0", 1));
    assertFalse(Utils.negZero("0.00", 2));
    assertFalse(Utils.negZero("-9", 0));
    assertFalse(Utils.negZero("-0.9", 1));
    assertFalse(Utils.negZero("-0.09", 2));
  }

  private void checkWriteReal(final double x, final int dp) throws IOException {
    final String a = Utils.realFormat(x, dp);
    //System.err.println(a);
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    Utils.realWrite(out, x, dp);
    assertTrue(a + ":" + out.toString(), Arrays.equals(a.getBytes(), out.toByteArray()));
  }

  private void checkWriteReal(final double x) throws IOException {
    double y = x;
    for (int i = 0; i < 12; ++i) {
      for (int dp = 0; dp < 11; ++dp) {
        checkWriteReal(y, dp);
      }
      y = 10.0 * y;
    }
  }

  public void testWriteReal() throws IOException {
    checkWriteReal(0.000000123456789);
    checkWriteReal(0.123456789);
    checkWriteReal(12.3456789);
  }

  private void checkReal1(final String a, final double x, final int dp) throws IOException {
    assertEquals(a, Utils.realFormat(x, dp));
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    Utils.realWrite(out, x, dp);
    assertTrue(a + ":" + out.toString(), Arrays.equals(a.getBytes(), out.toByteArray()));
  }

  public void testReal1() throws IOException {

    checkReal1("0.060", 0.060, 3);
    checkReal1("0.060", 0.0599, 3);
    checkReal1("0.060", 0.0601, 3);
    checkReal1("1.060", 1.060, 3);
    checkReal1("10.060", 10.060, 3);
    checkReal1("0.006", 0.006, 3);
    checkReal1("-2", -1.5, 0);
    checkReal1("2", 1.5, 0);
    checkReal1("-1235", -1234.5, 0);
    checkReal1("0", -0.001, 0);
    checkReal1("0.00", -0.001, 2);
    checkReal1("0", 0.0, 0);
    checkReal1("1", 0.5, 0);
    checkReal1("2", 1.5, 0);
    checkReal1("1235", 1234.5, 0);
    checkReal1("Infinity", Double.POSITIVE_INFINITY, 0);
    checkReal1("-Infinity", Double.NEGATIVE_INFINITY, 0);
    checkReal1("NaN", Double.NaN, 0);

    checkReal1("0.00", 0.0, 2);
    checkReal1("0.50", 0.5, 2);
    checkReal1("-1.50", -1.5, 2);
    checkReal1("1.50", 1.5, 2);
    checkReal1("1234.50", 1234.5, 2);
    checkReal1("-1234.50", -1234.5, 2);
    checkReal1("Infinity", Double.POSITIVE_INFINITY, 2);
    checkReal1("-Infinity", Double.NEGATIVE_INFINITY, 2);
    checkReal1("NaN", Double.NaN, 2);

    checkReal1("0.0000000000", 0.0, 10);
    checkReal1("0.5000000000", 0.5, 10);
    checkReal1("-1.5000000000", -1.5, 10);
    checkReal1("1.5000000000", 1.5, 10);
    checkReal1("1234.5000000000", 1234.5, 10);
    checkReal1("-1234.5000000000", -1234.5, 10);
    checkReal1("Infinity", Double.POSITIVE_INFINITY, 10);
    checkReal1("-Infinity", Double.NEGATIVE_INFINITY, 10);
    checkReal1("NaN", Double.NaN, 10);
  }

  private void checkReal2(final String a, final int dp, final double... x) throws IOException {
    assertEquals(a, Utils.realFormat(x, dp));
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    Utils.realWrite(out, x, dp);
    assertTrue(a + ":" + out.toString(), Arrays.equals(a.getBytes(), out.toByteArray()));
  }

  public void testReal2() throws IOException {
    checkReal2("[]", 1);
    checkReal2("[-0.1]", 1, -0.1);
    checkReal2("[-0.1, 0.2]", 1, -0.1, 0.2);
  }

  public void testNullFormat() {
    assertEquals("null", Utils.nullFormat(null));
    assertEquals("1", Utils.nullFormat(1));
  }


  public void testGetStackTrace() {
    try {
      throw new RuntimeException("a test exception");
    } catch (final Throwable t) {
      final String st = Utils.getStackTrace(t);
      TestUtils.containsAll(st, "a test exception", "estGetStackTrace", "UtilsTest");
    }
  }

  public void testWhereAmI() {
    final String str = Utils.whereAmI(0);
    //System.err.println(str);
    final String strl = str.toLowerCase(Locale.getDefault());
    assertTrue(strl.contains("testwhereami"));
    assertTrue(strl.contains("utilstest"));
  }
}
