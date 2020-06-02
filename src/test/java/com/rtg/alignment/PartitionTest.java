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

package com.rtg.alignment;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class PartitionTest extends TestCase {

  public void test() {
    assertTrue(new Partition().isEmpty());
  }

  public void testBreakMnps() {
    final Partition partition = new Partition();
    partition.add(new Slice(42, "CTA", "GGG", "CCC"));
    final Partition p = Partition.breakMnps(partition);
    assertEquals(3, p.size());
    assertEquals("42 [C, G, C]", p.get(0).toString());
    assertEquals("43 [T, G, C]", p.get(1).toString());
    assertEquals("44 [A, G, C]", p.get(2).toString());
  }

  public void testPeelIndels1() {
    final Partition partition = new Partition();
    partition.add(new Slice(42, "AAT", "CT", "GG"));
    final Partition p = Partition.peelIndels(partition);
    assertEquals(3, p.size());
    assertEquals("42 [A, , ]", p.get(0).toString());
    assertEquals("43 [A, C, G]", p.get(1).toString());
    assertEquals("44 [T, T, G]", p.get(2).toString());
  }

  public void testPeelIndels2() {
    final Partition partition = new Partition();
    partition.add(new Slice(42, "A", "CT", "CT"));
    final Partition p = Partition.peelIndels(partition);
    assertEquals(2, p.size());
    assertEquals("42 [, C, C]", p.get(0).toString());
    assertEquals("42 [A, T, T]", p.get(1).toString());
  }

  public void testPeelIndels3() {
    final Partition partition = new Partition();
    partition.add(new Slice(42, "AA", "CTAA", "CCGA", "ACCA", "AGGG"));
    final Partition p = Partition.peelIndels(partition);
    assertEquals(3, p.size());
    assertEquals("42 [A, C, C, A, A]", p.get(0).toString());
    assertEquals("43 [, TA, CG, CC, GG]", p.get(1).toString());
    assertEquals("43 [A, A, A, A, G]", p.get(2).toString());
  }
}
