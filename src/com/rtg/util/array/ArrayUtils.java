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
package com.rtg.util.array;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.rtg.util.array.longindex.LongIndex;
import com.rtg.util.io.ByteArrayIOUtils;
import com.rtg.util.io.FileUtils;

/**
 * Array utility functions.
 *
 */
public final class ArrayUtils {

  private ArrayUtils() { }

  /**
   * Sum an array of longs.
   *
   * @param a array
   * @return sum
   */
  public static long sum(final long[] a) {
    long sum = 0;
    for (final long x : a) {
      sum += x;
    }
    return sum;
  }

  /**
   * Sum an array of ints.  Returns a long in order to minimise changes of overflow.
   *
   * @param a array
   * @return sum
   */
  public static long sum(final int[] a) {
    long sum = 0;
    for (final int x : a) {
      sum += x;
    }
    return sum;
  }

  /**
   * Read an array of longs from a file.
   *
   * @param f file
   * @return array
   * @exception IOException if an I/O error occurs
   */
  public static long[] readLongArray(final File f) throws IOException {
    final byte[] b = new byte[(int) f.length()];
    try (InputStream stream = new FileInputStream(f)) {
      final int length = stream.read(b);
      if (length != b.length) {
        throw new IOException();
      }
    }
    return ByteArrayIOUtils.convertToLongArray(b);
  }

  /**
   * Read an array of longs from a file.
   *
   * @param f file
   * @param a index to read into
   * @param offset in the array.
   * @param addend amount to add to each entry
   * @return length
   * @exception IOException if an I/O error occurs
   */
  public static int readInts(final File f, final LongIndex a, final long offset, final long addend) throws IOException {
    return readInts(f, 0, (int) f.length() / 4, a, offset, addend);
  }

  /**
   * Read an array of longs from a file.
   *
   * @param f file
   * @param fileOffset offset into the file to start reading at
   * @param fileEnd position in the file to finish
   * @param a index to read into
   * @param offset in the array.
   * @param addend amount to add to each entry
   * @return length
   * @exception IOException if an I/O error occurs
   */
  public static int readInts(final File f, final int fileOffset, final int fileEnd, final CommonIndex a, final long offset, final long addend) throws IOException {
    final int length = (fileEnd - fileOffset) * 4;
    final byte[] b = new byte[length];
    try (InputStream stream = new FileInputStream(f)) {

      // Skip has failed lets attempt to use read to fix things up.
      int remaining = fileOffset * 4;
      long skipped;
      while (remaining > 0 && (skipped = FileUtils.streamSkip(stream, remaining)) > 0) {
        remaining -= (int) skipped;
      }
      if (remaining > 0) {
        throw new IOException();
      }
      int soFar = 0;
      int read;
      while (soFar < length && (read = stream.read(b, soFar, length - soFar)) > 0) {
        soFar += read;
      }
      if (soFar < length) {
        throw new IOException();
      }
    }
    for (int i = 0; i < b.length; i += 4) {
      a.set(offset + i / 4, ByteArrayIOUtils.bytesToIntBigEndian(b, i) + addend);
    }
    return b.length / 4;
  }

  /**
   * Return list of boxed bytes as a primitive array.
   *
   * @param l list
   * @return byte array
   */
  public static byte[] asByteArray(final List<Byte> l) {
    final byte[] a = new byte[l.size()];
    for (int i = 0; i < a.length; i++) {
      a[i] = l.get(i);
    }
    return a;
  }

  /**
   * Return list of boxed longs as a primitive array.
   *
   * @param l list
   * @return long array
   */
  public static long[] asLongArray(final List<Long> l) {
    final long[] a = new long[l.size()];
    for (int i = 0; i < a.length; i++) {
      a[i] = l.get(i);
    }
    return a;
  }

  /**
   * @param input list of integers represented as decimals separated by comma.
   * @return the values separated into an array
   * @throws NumberFormatException if the string format is invalid
   */
  public static int[] parseIntArray(final String input) {
    final String[] strings = input.split(", *");
    final int[] values = new int[strings.length];
    for (int i = 0; i < strings.length; i++) {
      values[i] = Integer.parseInt(strings[i]);
    }
    return values;
  }

  /**
   * Checks that a segment of an array is sorted.
   * @param arr1 primary array
   * @param start the first position to check (inclusive)
   * @param end one past the last position to check (exclusive)
   * @return <code>true</code> if the array is sorted, <code>false</code>
   *         otherwise
   */
  public static boolean isSorted(final CommonIndex arr1, final long start, final long end) {
    assert 0 <= start;
    assert start <= end;
    assert end <= arr1.length();
    for (long ii = start; ii < end - 1; ii++) {
      if (arr1.get(ii) > arr1.get(ii + 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks that an array is sorted.
   * @param arr1 primary array
   * @param len the length to check
   * @return <code>true</code> if the array is sorted, <code>false</code>
   *         otherwise
   */
  public static boolean isSorted(final CommonIndex arr1, final long len) {
    assert len <= arr1.length();
    for (long ii = 0; ii < len - 1; ii++) {
      if (arr1.get(ii) > arr1.get(ii + 1)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks that an array is sorted (strictly increasing).
   * @param arr1 primary array
   * @param len the length to check
   * @return <code>true</code> if the array is sorted, <code>false</code> otherwise
   */
  public static boolean isSortedStrict(final CommonIndex arr1, final long len) {
    assert len <= arr1.length();
    for (long ii = 0; ii < len - 1; ii++) {
      if (arr1.get(ii) >= arr1.get(ii + 1)) {
        return false;
      }
    }
    return true;
  }

}
