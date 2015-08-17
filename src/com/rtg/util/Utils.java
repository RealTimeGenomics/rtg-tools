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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Locale;

import com.rtg.util.io.FileUtils;

/**
 * Utility functions with no defined home.
 */
public final class Utils {

  private Utils() {  }

  /**
   * Convert a number into a bit string with separators between each group of 8.
   * @param x number to be displayed
   * @return string giving bit decomposition of x.
   */
  public static String toBitsSep(final long x) {
    final StringBuilder sb = new StringBuilder();
    long t = x;
    for (int i = 0; i < Long.SIZE; i++) {
      if ((i & 7) == 0 && i > 0) {
        sb.append(":");
      }
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  /**
   * Convert a number into a bit string with separators between each group of 8.
   * @param x number to be displayed
   * @return string giving bit decomposition of x.
   */
  public static String toBitsSep(final int x) {
    final StringBuilder sb = new StringBuilder();
    int t = x;
    for (int i = 0; i < Integer.SIZE; i++) {
      if ((i & 7) == 0 && i > 0) {
        sb.append(":");
      }
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  /**
   * Convert an array into a bit string.
   * @param x array of numbers to be displayed
   * @return string giving bit decomposition of x.
   */
  public static String toBits(final long[] x) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < x.length; i++) {
      sb.append("[").append(i).append("]").append(toBits(x[i], 64)).append(" ");
    }
    return sb.toString();
  }

  /**
   * Convert a number into a bit string.
   * @param x number to be displayed
   * @return string giving bit decomposition of x.
   */
  public static String toBits(final long x) {
    final StringBuilder sb = new StringBuilder();
    long t = x;
    boolean first = false;
    for (int i = 0; i < 64; i++) {
      if (t < 0) {
        sb.append("1");
        first = true;
      } else if (first) {
        sb.append("0");
      }
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  /**
   * Convert a number into a bit string.
   * The low order groups of bits of length <code>len</code> are displayed.
   * This is handy for displaying hash function bits.
   * @param x number to be displayed
   * @param len number of bits and codes to display.
   * @return string giving bit decomposition of x.
   */
  public static String toBits(final long x, final int len) {
    if (len <= 0 || len > 64) {
      throw new IllegalArgumentException("length out of range=" + len);
    }
    final StringBuilder sb = new StringBuilder();
    final int left = 64 - len;
    long t = x << left;
    for (int i = 0; i < len; i++) {
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  /**
   * Convert a number into a bit string.
   * Two low order groups of bits of length <code>len</code> are displayed.
   * This is handy for displaying hash function bits.
   * @param x number to be displayed
   * @param len number of bits and codes to display.
   * @return string giving bit decomposition of x.
   */
  public static String toBits2(final long x, final int len) {
    final StringBuilder sb = new StringBuilder();
    if (len <= 0 || len > 32) {
      throw new IllegalArgumentException("length out of range=" + len);
    }
    final int left = 64 - 2 * len;
    long t = x << left;
    for (int i = 0; i < len; i++) {
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    sb.append(":");
    for (int i = 0; i < len; i++) {
      sb.append(t < 0 ? "1" : "0");
      t = t << 1;
    }
    assert t == 0;
    return sb.toString();
  }

  /**
   * Convert a string of 0's and 1's into a long.
   * Ignores spaces and ':'s so that <code>toBits</code> can be read in again.
   * @param str string to be converted.
   * @return the result of parsing the bit string.
   */
  public static long fromBits(final String str) {
    long res = 0;
    int len = 0;
    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);
      switch(c) {
        case '0':
          res = res << 1;
          len++;
          break;
        case '1':
          res = (res << 1) + 1L;
          len++;
          break;
        case ':': //allows toBits to be read in again
        case ' ':
          break;
        default:
          throw new IllegalArgumentException("Invalid character in bit string:" + c);
      }
    }
    if (len > 64) {
      throw new IllegalArgumentException("bit string too long:" + len);
    }
    return res;
  }

  /**
   * Produce a one-one onto mapping from a pair of integers to an integer.
   * Only fails because of finite precision of integers. Taken to be a good way of
   * combining hashes.
   * @param i first integer.
   * @param j second integer.
   * @return combined integer.
   */
  public static int pairHash(final int i, final int j) {
    if (i == 0 && j == 0) {
      return 1;
    }
    final long li = (long) i;
    final long lj = (long) j;
    final long k = (li < 0 ? -li : li) + (lj < 0 ? -lj : lj);
    assert k >= 0 : k;
    final long l = i >= 0 ? k + j : 3 * k - j;
    final long x = ((k * (k - 1L)) << 1) + l + 2L;
    return (int) x;
  }

  /**
   * Produce a one-one onto mapping from three integers to an integer.
   * Only fails because of finite precision of integers. Taken to be a good way of
   * combining hashes.
   * @param i first integer.
   * @param j second integer.
   * @param k third integer.
   * @return combined integer.
   */
  public static int pairHash(int i, int j, int k) {
    final int t = pairHash(i, j);
    return pairHash(t, k);
  }

  /**
   * Produce a one-one onto mapping from four integers to an integer.
   * Only fails because of finite precision of integers. Taken to be a good way of
   * combining hashes.
   * @param i first integer.
   * @param j second integer.
   * @param k third integer.
   * @param l fourth integer.
   * @return combined integer.
   */
  public static int pairHash(int i, int j, int k, int l) {
    int t = pairHash(i, j);
    t = pairHash(t, k);
    return pairHash(t, l);
  }

  /**
   * Produce a one-one onto mapping from five integers to an integer.
   * Only fails because of finite precision of integers. Taken to be a good way of
   * combining hashes.
   * @param i first integer.
   * @param j second integer.
   * @param k third integer.
   * @param l fourth integer.
   * @param m fifth integer.
   * @return combined integer.
   */
  public static int pairHash(int i, int j, int k, int l, int m) {
    int t = pairHash(i, j);
    t = pairHash(t, k);
    t = pairHash(t, l);
    return pairHash(t, m);
  }

  /**
   * Repeatedly applies {@link Utils#pairHash(int, int)} until all values have
   * been combined. Note that unrolled cases of {@code pairHash} are provided for up to five integers.
   * @param vals the values to turn into one hash
   * @return the hash
   */
  public static int pairHashContinuous(int... vals) {
    assert vals.length > 1;
    int ret = pairHash(vals[0], vals[1]);
    for (int i = 2; i < vals.length; i++) {
      ret = pairHash(ret, vals[i]);
    }
    return ret;
  }

  /**
   * Compute a hash code for a collection of objects from their individual hash codes.
   * @param objects from which hash code to be computed.
   * @return hash code.
   */
  public static int hash(final Object[] objects) {
    if (objects.length == 0) {
      return 0;
    }
    final Object obj = objects[0];
    int hash = (obj == null ? 0 : objects[0].hashCode()) + 43;
    for (int i = 1; i < objects.length; i++) {
      final Object ob = objects[i];
      final int h = ob == null ? 0 : ob.hashCode();
      hash = pairHash(hash, h);
    }
    return hash;
  }

  /**
   * Tests whether two objects are equal handling cases where one or both may be null
   * @param o1 object 1
   * @param o2 object 2
   * @return true if both objects are {@link Object#equals(Object)} or both null
   */
  public static boolean equals(Object o1, Object o2) {
    if (o1 == null) {
      return o2 == null;
    }
    return o1.equals(o2);
  }

  /**
   * Check two lists of objects for equality.
   * @param a array of objects to be checked.
   * @param b array of objects to be checked.
   * @return true iff the lists are the same length and each corresponding object is equal.
   */
  public static boolean equals(final Object[] a, final Object[] b) {
    if (a == b) {
      return true;
    } else if (a.length != b.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      final Object x = a[i];
      final Object y = b[i];
      if (x == null) {
        if (y != null) {
          return false;
        }
      } else {
        if (!x.equals(y)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Converts an int from little endian to big endian, or vice versa.
   * @param i an <code>int</code> value
   * @return an <code>int</code> value, with the endianness swapped.
   */
  public static int swapEndian(final int i) {
    return ((i & 0xff) << 24) | ((i & 0xff00) << 8) | ((i >>> 8) & 0xff00) | ((i >>> 24) & 0xff);
  }

  /**
   * Converts an long from little endian to big endian, or vice versa.
   * @param l a <code>long</code> value
   * @return a <code>long</code> value, with the endianness swapped.
   */
  public static long swapEndian(final long l) {
    return ((l & 0xff) << 56)
        | ((l & 0xff00) << 40)
        | ((l & 0xff0000) << 24)
        | ((l & 0xff000000L) << 8)
        | ((l >>> 8) & 0xff000000L)
        | ((l >>> 24) & 0xff0000)
        | ((l >>> 40) & 0xff00)
        | ((l >>> 56) & 0xff);
  }

  /**
   * Append two arrays creating a new array for the result.
   * @param <A> the type of the arrays.
   * @param a first array
   * @param b second array
   * @return the concatenation of a then b.
   */
  public static <A> A[] append(final A[] a, final A[] b) {
    final int alen = a.length;
    final int blen = b.length;
    final A[] res = Arrays.copyOf(a, alen + blen);
    System.arraycopy(b, 0, res, alen, blen);
    return res;
  }

  /**
   * Append Strings to an array creating a new array for the result.
   * @param a first array
   * @param b second set of Strings
   * @return the concatenation of a then b.
   */
  public static String[] append(final String[] a, final String... b) {
    final int alen = a.length;
    final int blen = b.length;
    final String[] res = Arrays.copyOf(a, alen + blen);
    System.arraycopy(b, 0, res, alen, blen);
    return res;
  }

  /**
   * Reverse all of a byte array in-place.
   * @param a a non-null array of bytes.
   */
  public static void reverseInPlace(final byte[] a) {
    for (int start = 0, end = a.length - 1; start < end; start++, end--) {
      final byte tmp = a[start];
      a[start] = a[end];
      a[end] = tmp;
    }
  }

  /**
   * Check if a string is of the form "-0.00".
   *
   * @param str the string being checked.
   * @param dp the number of zeros after the decimal point.
   * @return true iff of the the form being checked.
   */
  static boolean negZero(final String str, final int dp) {
    if (str.charAt(0) != '-' || str.charAt(1) != '0') {
      return false;
    } else if (dp == 0) {
      return true;
    } else if (str.charAt(2) != '.') {
      return false;
    }
    for (int i = 0; i < dp; i++) {
      if (str.charAt(3 + i) != '0') {
        return false;
      }
    }
    return true;
  }

  /**
   * Output a real in a way that works the same in C# and Java (always has at least one
   * decimal place of precision).
   * @param x number to be formatted.
   * @return formatted number.
   */
  public static String realFormat(final Double x) {
    return x == null ? "null" : realFormat(x, decimalPlaces(x));
  }

  /**
   * Format a real with specified number of digits after the decimal point.
   * @param x number to be formatted.
   * @param dp number of digits of precision.
   * @return the formatted string.
   */
  public static String realFormat(final double x, final int dp) {
    assert dp >= 0;
    if (Double.isNaN(x) || Double.isInfinite(x)) {
      return Double.toString(x);
    }
    final String fmt = "%1$01." + dp + "f";
    final String res = String.format(Locale.ROOT, fmt, x);
    if (x <= 0.0) {
      if (negZero(res, dp)) {
        return res.substring(1);
      } else {
        return res;
      }
    } else {
      return res;
    }
  }

  private static final int[] POWERS_10 = {
    1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
  };

  /**
   * Write an integer into an <code>OutputStream</code> without constructing a <code>String</code>.
   * @param out where number to be written.
   * @param i number to be formatted.
   * @throws IOException from out.
   */
  public static void intWrite(final OutputStream out, final int i) throws IOException {
    if (i == 0) {
      out.write('0');
      return;
    }
    int t;
    if (i < 0) {
      if (i == Integer.MIN_VALUE) {
        out.write("-2147483648".getBytes());
        return;
      }
      out.write('-');
      t = -i;
    } else {
      t = i;
    }
    int x = POWERS_10.length - 1;
    //Find first non-zero digit
    while (POWERS_10[x] > t) {
      x--;
    }

    //generate all digits - avoid division and %
    while (x >= 0) {
      int d = 0;
      final int pwr = POWERS_10[x];
      while (pwr <= t) {
        t -= pwr;
        d++;
      }
      out.write('0' + d);
      x--;
    }
    assert t == 0;
  }

  private static final byte[] NAN_BYTES = "NaN".getBytes();
  private static final double MAX = (double) Integer.MAX_VALUE;
  private static final double MIN = (double) Integer.MIN_VALUE;

  /**
   * Write a real with specified number of digits after the decimal point.
   * @param out where number to be written.
   * @param d number to be formatted.
   * @param dp number of digits of precision.
   * @throws IOException from out.
   */
  public static void realWrite(final OutputStream out, final double d, final int dp) throws IOException {
    //The order of the following tests is important
    if (Double.isNaN(d)) {
      out.write(NAN_BYTES);
      return;
    }
    final boolean tooBig;
    final double e;
    if (dp >= POWERS_10.length) {
      tooBig = true;
      e = Double.NaN;
    } else {
      e = d * POWERS_10[dp];
      tooBig = e <= MIN || e > MAX;
    }
    if (tooBig) {
      out.write(realFormat(d, dp).getBytes());
      return;
    }
    final int s;
    if (d < 0.0) {
      s = (int) -MathUtils.round(-e);
    } else {
      s = (int) MathUtils.round(e);
    }
    if (dp == 0) {
      intWrite(out, s);
      return;
    }

    int t;
    if (s < 0) {
      t = -s;
      out.write('-');
    } else {
      t = s;
    }
    if (t == 0) {
      out.write('0');
      out.write('.');
      for (int k = 0; k < dp; k++) {
        out.write('0');
      }
      return;
    }

    //Find first non-zero digit
    int x = POWERS_10.length - 1;
    while (POWERS_10[x] > t) {
      x--;
    }

    if (x < dp) {
      out.write((byte) '0');
    } else {
      //generate leading digits - avoid division and %
      while (x >= dp) {
        int dig = 0;
        final int pwr = POWERS_10[x];
        while (pwr <= t) {
          t -= pwr;
          dig++;
        }
        out.write('0' + dig);
        x--;
      }
    }
    out.write((byte) '.');

    //generate digits after the point - avoid division and %
    int y = Math.max(dp - 1, x);
    while (y >= 0) {
      int dig = 0;
      final int pwr = POWERS_10[y];
      while (pwr <= t) {
        t -= pwr;
        dig++;
      }
      out.write('0' + dig);
      y--;
    }
    assert t == 0;

  }

  /**
   * Format a real array in a way that can be easily ported to C#.
   * @param x array to be formatted.
   * @param dp number of digits of precision.
   * @return the formatted string.
   */
  public static String realFormat(final double[] x, final int dp) {
    final StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < x.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(realFormat(x[i], dp));
    }
    sb.append("]");
    return sb.toString();
  }

  private static final byte[] COMMA_BYTES = ", ".getBytes();

  /**
   * Format a real array in a way that can be easily ported to C#.
   * @param out output stream to write to
   * @param x array to be formatted.
   * @param dp number of digits of precision.
   * @throws IOException from out.
   */
  public static void realWrite(final OutputStream out, final double[] x, final int dp) throws IOException {
    out.write((byte) '[');
    for (int i = 0; i < x.length; i++) {
      if (i > 0) {
        out.write(COMMA_BYTES);
      }
      realWrite(out, x[i], dp);
    }
    out.write((byte) ']');
  }


  /**
   * Format a real array in a way that can be easily ported to C#.
   * @param x array to be formatted.
   * @return the formatted string.
   */
  public static String realFormatArray(final double[] x) {
    final StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < x.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(realFormat(x[i]));
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Compute the number of decimal places needed to reasonably display a double.
   * Done explicitly so that we can sharpen.
   * @param x number to be displayed.
   * @return the number of decimal places needed.
   */
  static int decimalPlaces(final double x) {
    double y = x * 10.0;
    int dp = 1;
    while (true) {
      if (dp == 10) {
        return 10;
      }
      final double z = MathUtils.round(y);
      final double del = Math.abs(z - y);
      if (del < 0.0000001) {
        return dp;
      }
      dp++;
      y = y * 10.0;
    }
  }

  /**
   * Format returning "null" explicitly. Enables dealing with C# Java discrepancies.
   * @param obj object to be formatted.
   * @return formatted object.
   */
  public static String nullFormat(final Object obj) {
    return obj == null ? "null" : obj.toString();
  }

  /**
   * Get a stack trace as a string.
   * @param t the throwable (including exceptions) with the stack trace.
   * @return the stack trace.
   */
  public static String getStackTrace(final Throwable t) {
    final StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * Get a string that says where this call was made from.
   * @param index depth down the stack to report (0 is the line where the <code>whereAmI</code>
   * call was made).
   * @return a string that says where this call was made from.
   */
  public static String whereAmI(final int index) {
    final String res = whereAmI();
    final String[] split = res.split(StringUtils.LS);
    return split[3 + index];
  }

  /**
   * Get a string that says where this call was made from.
   * @return a string that says where this call was made from.
   */
  public static String whereAmI() {
    final RuntimeException re = new RuntimeException();
    return getStackTrace(re);
  }

  /**
   * @param zippedSv zipped file to check
   * @return true if this looks like a SNP file.
   * @throws IOException if an IO Error occurs
   */
  public static boolean isSvOutput(File zippedSv) throws IOException {
    final boolean ret;
    try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.createGzipInputStream(zippedSv, false)))) {
      final String line = r.readLine();
      ret = line != null && line.startsWith("#Version") && (line.contains("SV simple output") || line.contains("SV bayesian output"));
    }
    return ret;
  }

  /**
   * @param zippedCoverage zipped file to check
   * @return true if this looks like a BED coverage file.
   * @throws IOException if an IO Error occurs
   */
  public static boolean isCoverageOutput(File zippedCoverage) throws IOException {
    final boolean ret;
    try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.createGzipInputStream(zippedCoverage, false)))) {
      final String line = r.readLine();
      ret = line != null && line.startsWith("#Version") && line.contains("Coverage output");
    }
    return ret;
  }

  /**
   * @param zippedVcf zipped file to check
   * @return true if this looks like a <code>VCF</code> file.
   * @throws IOException if an IO Error occurs
   */
  public static boolean isVcfFormat(File zippedVcf) throws IOException {
    try (BufferedReader r = new BufferedReader(new InputStreamReader(FileUtils.createGzipInputStream(zippedVcf, false)))) {
      String line;
      while ((line = r.readLine()) != null) {
        if (line.startsWith("##fileformat=VCF")) {
          return true;
        }
        if (!line.startsWith("##")) {
          return false;
        }
      }
      return false;
    }
  }
}
