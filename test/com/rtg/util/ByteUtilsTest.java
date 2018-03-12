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

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class ByteUtilsTest extends TestCase {

  public void testConstants() throws IOException {
    final OutputStream out = new ByteArrayOutputStream();
    ByteUtils.writeLn(out);
    out.write(ByteUtils.FS_BYTE);
    out.write(ByteUtils.TAB_BYTE);
    assertEquals(StringUtils.LS + StringUtils.FS + "\t", out.toString());
  }

  private byte[][] convert(String... strings) {
    final byte[][] result = new byte[strings.length][];
    int i = 0;
    for (String s : strings) {
      result[i++] = s.getBytes();
    }
    return result;
  }

  public void testLeftClip() {
    assertEquals(0, ByteUtils.longestPrefix(0, new byte[0]));
    assertEquals(2, ByteUtils.longestPrefix(0, convert("hi")));
    assertEquals(2, ByteUtils.longestPrefix(0, convert("hi", "hi")));
    assertEquals(2, ByteUtils.longestPrefix(0, convert("hi", "his")));
    assertEquals(2, ByteUtils.longestPrefix(0, convert("hit", "his")));
    assertEquals(3, ByteUtils.longestPrefix(0, convert("hit", "hittter", "hits")));
    assertEquals(2, ByteUtils.longestPrefix(1, convert("hit", "hittter", "hits")));
    assertEquals(1, ByteUtils.longestPrefix(2, convert("hit", "hittter", "hits")));
    assertEquals(0, ByteUtils.longestPrefix(3, convert("hit", "hittter", "hits")));
    assertEquals(0, ByteUtils.longestPrefix(4, convert("hit", "hittter", "hits")));
  }

  public void testRightClip() {
    assertEquals(0, ByteUtils.longestSuffix(0, new byte[0]));
    assertEquals(2, ByteUtils.longestSuffix(0, convert("hi")));
    assertEquals(2, ByteUtils.longestSuffix(0, convert("hi", "hi")));
    assertEquals(2, ByteUtils.longestSuffix(0, convert("hi", "shi")));
    assertEquals(2, ByteUtils.longestSuffix(0, convert("thi", "shi")));
    assertEquals(3, ByteUtils.longestSuffix(0, convert("rethit", "hit", "rhit")));
    assertEquals(2, ByteUtils.longestSuffix(1, convert("rethit", "hit", "rhit")));
    assertEquals(1, ByteUtils.longestSuffix(2, convert("rethit", "hit", "rhit")));
    assertEquals(0, ByteUtils.longestSuffix(3, convert("rethit", "hit", "rhit")));
    assertEquals(0, ByteUtils.longestSuffix(4, convert("rethit", "hit", "rhit")));

    assertEquals(6, ByteUtils.longestSuffix(0, convert("acacacac", "acacac")));
    assertEquals(0, ByteUtils.longestPrefix(6, convert("acacacac", "acacac")));
  }

}
