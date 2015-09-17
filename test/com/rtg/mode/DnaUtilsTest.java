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
package com.rtg.mode;

import java.util.Arrays;

import com.rtg.util.Utils;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class DnaUtilsTest extends TestCase {

  public void testToCodes01() {
    assertEquals("AAA", DnaUtils.toCodes(Utils.fromBits(""), Utils.fromBits(""), 3));
    assertEquals("TGC", DnaUtils.toCodes(Utils.fromBits("110"), Utils.fromBits("101"), 3));
    assertEquals("AT", DnaUtils.toCodes(Utils.fromBits("101"), Utils.fromBits("101"), 2));
    assertEquals("T", DnaUtils.toCodes(Utils.fromBits("101"), Utils.fromBits("101"), 1));
    assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATAT", DnaUtils.toCodes(Utils.fromBits("101"), Utils.fromBits("101"), 64));
    try {
      DnaUtils.toCodes(0, 0);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=0", e.getMessage());
    }
    try {
      DnaUtils.toCodes(0, 65);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=65", e.getMessage());
    }
  }

  public void testToCodes() {
    assertEquals("AAA", DnaUtils.toCodes(Utils.fromBits(""), 3));
    assertEquals("CAC", DnaUtils.toCodes(Utils.fromBits("101"), 3));
    assertEquals("AT", DnaUtils.toCodes(Utils.fromBits("101"), 2));
    assertEquals("G", DnaUtils.toCodes(Utils.fromBits("110"), 1));
    assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAACAC", DnaUtils.toCodes(Utils.fromBits("101"), 32));
    try {
      DnaUtils.toCodes(0, 0);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=0", e.getMessage());
    }
    try {
      DnaUtils.toCodes(0, 33);
    } catch (final RuntimeException e) {
      assertEquals("length out of range=33", e.getMessage());
    }
  }

  public void testReverseComplement() {
    assertEquals("", DnaUtils.reverseComplement(""));
    assertEquals("ncagt", DnaUtils.reverseComplement("actgn"));
    assertEquals("NCAGT", DnaUtils.reverseComplement("ACTGN"));
    assertEquals("tgcatgca", DnaUtils.reverseComplement("tgcatgca"));
  }

  public void testEncodeString() {
    final String exp = "NACGT";
    final byte[] b = DnaUtils.encodeString(exp);
    final String s = DnaUtils.bytesToSequenceIncCG(b);
    assertEquals(exp, s);
  }

  public void testByteRevCo() {
    final byte[] src = {'A', 'C', 'C', 'T', 'G', 'G', 'A'};
    final byte[] dest = new byte[src.length];

    DnaUtils.reverseComplement(src, dest, src.length);
    assertTrue(Arrays.toString(dest), Arrays.equals(new byte[] {'T', 'C', 'C', 'A', 'G', 'G', 'T'}, dest));

    final byte[] dest2 = new byte[src.length];

    DnaUtils.reverseComplement(src, dest2, 5);
    assertTrue(Arrays.toString(dest2), Arrays.equals(new byte[] {'C', 'A', 'G', 'G', 'T', 0, 0}, dest2)); //cheating by using N as 0, oh well.
  }

}

