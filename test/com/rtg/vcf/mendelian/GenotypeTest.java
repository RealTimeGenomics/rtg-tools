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
package com.rtg.vcf.mendelian;

import java.io.IOException;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class GenotypeTest extends TestCase {

  public void testDip() throws IOException {
    final Genotype gt = new Genotype(new int[] {1, 0});
    assertEquals(2, gt.length());
    assertEquals(0, gt.get(0));
    assertEquals(1, gt.get(1));
    assertEquals("0/1", gt.toString());

    assertTrue(gt.contains(0));
    assertTrue(gt.contains(1));
    assertFalse(gt.contains(2));
  }

  public void testHap() throws IOException {
    final Genotype gt = new Genotype(new int[]{2});
    assertEquals(1, gt.length());
    assertEquals(2, gt.get(0));
    assertEquals("2", gt.toString());

    assertFalse(gt.contains(0));
    assertFalse(gt.contains(1));
    assertTrue(gt.contains(2));
  }

  public void testComparator() {
    final Genotype[] ordered = {new Genotype("0"), new Genotype("0/0"), new Genotype("0/1"), new Genotype("1"), new Genotype("1/1")};
    TestUtils.testOrder(Genotype.GENOTYPE_COMPARATOR, ordered, false);

  }
}
