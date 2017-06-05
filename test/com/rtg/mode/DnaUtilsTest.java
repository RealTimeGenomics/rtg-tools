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

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 *
 */
public class DnaUtilsTest extends TestCase {

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
    try {
      DnaUtils.encodeString("X");
    } catch (IllegalBaseException e) {
      TestUtils.containsAll(e.getMessage(), "Illegal DNA", "X");
    }
  }

  public void testEncodeArrayCopy() {
    final String exp = "AGTGCCGCGATCGTAGACAGTNNTCAGT";
    assertEquals(exp, DnaUtils.bytesToSequenceIncCG(DnaUtils.encodeArrayCopy(exp.getBytes())));
    assertEquals(exp.substring(4, 10), DnaUtils.bytesToSequenceIncCG(DnaUtils.encodeArrayCopy(exp.getBytes(), 4, 6)));
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

