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
package com.rtg.sam;

import com.rtg.util.Resources;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class BgzfInputStreamTest extends TestCase {

  public void testInput() throws Exception {

    BgzfInputStream bgzfi = new BgzfInputStream(Resources.getResourceAsStream("com/rtg/sam/resources/bam.bam"));
    try {

      assertEquals(0L, bgzfi.blockStart());

      final byte[] b = new byte[197];
      final int i = bgzfi.read(b);
      assertEquals(197, i);
      assertEquals(0L, bgzfi.blockStart());

    } finally {
      bgzfi.close();
    }
    bgzfi = new BgzfInputStream(Resources.getResourceAsStream("com/rtg/sam/resources/bam.bam"));
    try {
      final byte[] b = new byte[199];

      try {
        final int len = bgzfi.read(b, 10, 199);
        fail();
        assertEquals("wasteful line of code this", 0, len);
      } catch (ArrayIndexOutOfBoundsException aioobe) {
      }

      final int i = bgzfi.read(b);
      assertEquals(198, i);
      assertEquals(134L, bgzfi.blockStart());

    } finally {
      bgzfi.close();
    }
  }
}
