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

import com.rtg.util.arithcode.Order0ModelBuilder;

/**
 */
public class ArithByteCompressionTest extends AbstractByteCompressionTest {

  @Override
  protected ByteCompression getCompressor() {
    return new ArithByteCompression(10, 20, new Order0ModelBuilder(10));
  }

  public void testBytes() {
    final ArithByteCompression cmp = new ArithByteCompression(10, 12, new Order0ModelBuilder(10));
    cmp.integrity();
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
      cmp.integrity();
    }
    assertEquals(74, cmp.bytes());
    cmp.freeze();
    cmp.integrity();
    assertEquals(74, cmp.bytes());
  }

  //set initial count high enough that counts never frozen
  public void testBytesUncompressed() {
    final ArithByteCompression cmp = new ArithByteCompression(10, 1000, new Order0ModelBuilder(10));
    cmp.integrity();
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
      cmp.integrity();
    }
    assertEquals(80, cmp.bytes());
    cmp.freeze();
    cmp.integrity();
    assertEquals(74, cmp.bytes());
  }


}
