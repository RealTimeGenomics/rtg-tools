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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.util.Utils;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.array.longindex.LongIndex;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class ArrayUtilsTest extends TestCase {

  /**
   * Constructor (needed for JUnit)
   * @param name A string which names the object.
   */
  public ArrayUtilsTest(final String name) {
    super(name);
  }

  public void testParse() {
    int[] output = ArrayUtils.parseIntArray("1,2,3,4");
    assertEquals(4, output.length);
    for (int i = 0; i < output.length; ++i) {
      assertEquals(i + 1, output[i]);
    }
    output = ArrayUtils.parseIntArray("1");
    assertEquals(1, output.length);
    output = ArrayUtils.parseIntArray("123,3");
    assertEquals(2, output.length);
    assertEquals(123, output[0]);
    try {
      ArrayUtils.parseIntArray("1,2,3,4a");
      fail("Invalid integer Expected not thrown");
    } catch (final NumberFormatException e) {
      //Expected exception
    }
    try {
      ArrayUtils.parseIntArray("1,2,3,,4");
      fail("Missing value Exception not thrown");
    } catch (final NumberFormatException e) {
      //Expected exception
    }
  }

  public void testSum() {
    assertEquals(0, ArrayUtils.sum(new long[0]));
    assertEquals(0, ArrayUtils.sum(new long[10]));
    assertEquals(5, ArrayUtils.sum(new long[] {1, 2, 2}));
    assertEquals(0, ArrayUtils.sum(new int[0]));
    assertEquals(0, ArrayUtils.sum(new int[10]));
    assertEquals(5, ArrayUtils.sum(new int[] {1, 2, 2}));

    assertEquals(2L * Integer.MAX_VALUE, ArrayUtils.sum(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}));

  }

  public void testAsByteArray() {
    final byte[] testVector = {1, 0, (byte) 255, 1, 16, (byte) 240, Byte.MAX_VALUE, Byte.MIN_VALUE};
    final ArrayList<Byte> al = new ArrayList<>();
    for (final byte b : testVector) {
      al.add(b);
    }
    assertTrue(Arrays.equals(testVector, ArrayUtils.asByteArray(al)));
  }

  public void testAsLongArray() {
    final long[] testVector = {1, 0, (byte) 255, 1, 16, (byte) 240, Long.MAX_VALUE, Long.MIN_VALUE};
    final ArrayList<Long> al = new ArrayList<>();
    for (final long b : testVector) {
      al.add(b);
    }
    assertTrue(Arrays.equals(testVector, ArrayUtils.asLongArray(al)));
  }

  public void testReadLongArray() throws IOException {
    final File longArray = File.createTempFile("long", "array");
    try {
      final long[] numbers = {1, 0, 255, Long.MAX_VALUE, Long.MIN_VALUE};
      try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(longArray)))) {
        for (long number : numbers) {
          output.writeLong(number);
        }
      }
      final long[] saved = ArrayUtils.readLongArray(longArray);
      assertEquals(numbers.length, saved.length);
      for (int i = 0; i < numbers.length; ++i) {
        assertEquals(numbers[i], saved[i]);
      }
    } finally {
      assertTrue(longArray.delete());
    }
  }

  public void testReadInts() throws IOException {
    final File intArray = File.createTempFile("int", "array");
    try {
      final int[] numbers = {1, 0, 255, Integer.MAX_VALUE, Integer.MIN_VALUE};
      try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(intArray)))) {
        for (int number : numbers) {
          output.writeInt(number);
        }
      }
      final LongIndex saved = LongCreate.createIndex(numbers.length);
      final int size = ArrayUtils.readInts(intArray, saved, 0, 1);
      assertEquals(numbers.length, size);
      for (int i = 0; i < numbers.length; ++i) {
        assertEquals((long) numbers[i] + 1L, saved.get(i));
      }
    } finally {
      assertTrue(intArray.delete());
    }
  }
  public void testReadIntsSubset() throws IOException {
    final File intArray = File.createTempFile("int", "array");
    try {
      final int[] numbers = {10, 1, 0, 255, Integer.MAX_VALUE, Integer.MIN_VALUE, 20};
      try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(intArray)))) {
        for (int number : numbers) {
          output.writeInt(number);
        }
      }
      final LongIndex saved = LongCreate.createIndex(numbers.length - 2);
      final int size = ArrayUtils.readInts(intArray, 1, 6, saved, 0, 1);
      assertEquals(numbers.length - 2, size);
      for (int i = 1; i < numbers.length - 1; ++i) {
        assertEquals((long) numbers[i] + 1L, saved.get(i - 1));
      }
    } finally {
      assertTrue(intArray.delete());
    }
  }

  public void testReverseArrayInPlace() {
    final byte[] test = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    Utils.reverseInPlace(test);
    for (int i = 0; i < test.length; ++i) {
      assertEquals(9 - i, test[i]);
    }
  }

  public void testIdentity() {
    final int[] a = ArrayUtils.identity(3);
    assertEquals("[0, 1, 2]", Arrays.toString(a));
  }
}

