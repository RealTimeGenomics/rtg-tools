/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import java.util.Arrays;

import com.rtg.util.integrity.Exam.ExamException;

import junit.framework.TestCase;

/**
 */
public class MutatorResultTest extends TestCase {

  public void test0() {
    final MutatorResult mr = new MutatorResult(new byte[] {}, new byte[] {}, 0);
    mr.integrity();
    assertEquals("0::", mr.toString());
  }

  public void test1() {
    final MutatorResult mr = new MutatorResult(new byte[] {0, 1, 2}, new byte[] {3, 4}, 2);
    mr.integrity();
    assertEquals("2:NAC:GT", mr.toString());
    assertEquals(2, mr.getConsumed());
    assertTrue(Arrays.equals(new byte[] {0, 1, 2}, mr.getFirstHaplotype()));
    assertTrue(Arrays.equals(new byte[] {3, 4}, mr.getSecondHaplotype()));
  }

  public void testCheckHaplotype() {
    failHaplotype(new byte[]{-1});
    failHaplotype(new byte[]{5});
  }

  private void failHaplotype(byte[] haplotypes) {
    try {
      MutatorResult.checkHaplotype(haplotypes);
      fail();
    } catch (final ExamException e) {
      // expected
    }
  }
}
