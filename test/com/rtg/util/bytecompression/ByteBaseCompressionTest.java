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

package com.rtg.util.bytecompression;

import com.rtg.util.array.longindex.LongChunks;

/**
 */
public class ByteBaseCompressionTest extends AbstractByteCompressionTest {

  @Override
  protected ByteCompression getCompressor() {
    return new ByteBaseCompression(10);
  }

  public void testNoCompression() {
    check(new ByteBaseCompression(256), "1234", "094123", "86754", "", "", "12", "1");
  }

  public void testBytes() {
    final ByteBaseCompression cmp = new ByteBaseCompression(256);
    final String[] strings = {"1234", "094123", "86754", "", "", "12", "1"};
    final int[] pointers = new int[strings.length + 1];
    pointers[0] = 0;
    int numBytes = 0;
    for (int i = 0; i < strings.length; i++) {
      final int length = strings[i].length();
      numBytes += length;
      pointers[i + 1] = numBytes;
    }
    final byte[] bytes = new byte[numBytes];
    for (int i = 0; i < strings.length; i++) {
      final int start = pointers[i];
      for (int j = 0; j < strings[i].length(); j++) {
        bytes[start + j] = (byte) (strings[i].charAt(j) - '0');
      }
    }
    for (int i = 0; i < pointers.length - 1; i++) {
      final int length = pointers[i + 1] - pointers[i];
      cmp.add(bytes, pointers[i], length);
      assertEquals(length, cmp.length(i));
    }
    assertEquals(82, cmp.bytes());
    cmp.freeze();
    assertEquals(82, cmp.bytes());
    for (int i = 0; i < pointers.length - 1; i++) {
      final int length = pointers[i + 1] - pointers[i];
      assertEquals(length, cmp.length(i));
    }
  }

  public void testSdfConstructor() {
    final String[] strings = {"1234", "094123", "86754", "", "", "12", "1"};
    final LongChunks pointers = new LongChunks(strings.length + 1);
    pointers.set(0, 0);
    int numBytes = 0;
    for (int i = 0; i < strings.length; i++) {
      final int length = strings[i].length();
      numBytes += length;
      pointers.set(i + 1, numBytes);
    }
    final ByteArray data = new BitwiseByteArray(numBytes, 6);
    for (int i = 0; i < strings.length; i++) {
      final long start = pointers.get(i);
      for (int j = 0; j < strings[i].length(); j++) {
        data.set(start + j, new byte[] {(byte) (strings[i].charAt(j) - '0')}, 1);
      }
    }
    final ByteCompression cmp = new ByteBaseCompression(data, pointers);
    try {
      cmp.add(new byte[0], 0, 0);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Adding to a frozen ByteCompression", e.getMessage());
    }
    final long totalBytes = cmp.bytes();
    cmp.freeze();
    assertTrue(totalBytes >= cmp.bytes());
    for (int i = 0; i < pointers.length() - 1; i++) {
      final long start = pointers.get(i);
      final int length = (int) (pointers.get(i + 1) - start);
      final byte[] outBytes = new byte[length];
      cmp.get(outBytes, i, 0, length);
      for (int j = 0; j < length; j++) {
        assertEquals(data.get(j + start), outBytes[j]);
      }
    }
    try {
      cmp.add(new byte[0], 0, 0);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Adding to a frozen ByteCompression", e.getMessage());
    }
  }
}
