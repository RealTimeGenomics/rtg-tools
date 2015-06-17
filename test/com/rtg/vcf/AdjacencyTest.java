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

package com.rtg.vcf;

import java.io.IOException;

import junit.framework.TestCase;

/**
 */
public class AdjacencyTest extends TestCase {

  public void test() {
    final Adjacency adj = new Adjacency("chr1", 10, true, "ACGT", "chr2", 22, false);
    assertEquals("chr1", adj.thisChromosome());
    assertEquals(10,     adj.thisEndPos());
    assertEquals("ACGT", adj.thisBases());
    assertEquals(true,   adj.thisIsForward());

    assertEquals("chr2", adj.mateChromosome());
    assertEquals(22,     adj.mateStartPos());
    assertEquals(false,  adj.mateIsForward());
  }

  public void testParseFwdFwd() throws IOException {
    final Adjacency adj = Adjacency.parseAdjacency("chr1", 12, "A[chr2:9[");
    assertEquals("chr1", adj.thisChromosome());
    assertEquals(12,     adj.thisEndPos());
    assertEquals("A",    adj.thisBases());
    assertEquals(true,   adj.thisIsForward());

    assertEquals("chr2", adj.mateChromosome());
    assertEquals(9,     adj.mateStartPos());
    assertEquals(true,  adj.mateIsForward());
  }

  public void testParseFwdRev() throws IOException {
    final Adjacency adj = Adjacency.parseAdjacency("chr1", 12, "A]chr2:9]");
    assertEquals("chr1", adj.thisChromosome());
    assertEquals(12,     adj.thisEndPos());
    assertEquals("A",    adj.thisBases());
    assertEquals(true,   adj.thisIsForward());

    assertEquals("chr2", adj.mateChromosome());
    assertEquals(9,     adj.mateStartPos());
    assertEquals(false,  adj.mateIsForward());
  }

  public void testParseRevFwd() throws IOException {
    final Adjacency adj = Adjacency.parseAdjacency("chr1", 12, "[chr2:9[CG");
    assertEquals("chr1", adj.thisChromosome());
    assertEquals(12,     adj.thisEndPos());
    assertEquals("CG",   adj.thisBases());
    assertEquals(false,   adj.thisIsForward());

    assertEquals("chr2", adj.mateChromosome());
    assertEquals(9,     adj.mateStartPos());
    assertEquals(true,  adj.mateIsForward());
  }

  public void testParseRevRev() throws IOException {
    final Adjacency adj = Adjacency.parseAdjacency("chr1", 12, "]chr2:9]CG");
    assertEquals("chr1", adj.thisChromosome());
    assertEquals(12,     adj.thisEndPos());
    assertEquals("CG",   adj.thisBases());
    assertEquals(false,   adj.thisIsForward());

    assertEquals("chr2", adj.mateChromosome());
    assertEquals(9,     adj.mateStartPos());
    assertEquals(false,  adj.mateIsForward());
  }

  public void testBadParse() throws IOException {
    assertNull(Adjacency.parseAdjacency("chr1", 12, "C"));
    assertNull(Adjacency.parseAdjacency("chr1", 12, "<DEL>"));
    assertNull(Adjacency.parseAdjacency("chr1", 12, "<DUP:TANDEM>"));
  }

  public void testExceptionParse() {
    checkIoException("C[]");
    checkIoException("C[chr:32");
    checkIoException("C(chr:32[");
    checkIoException("C[chr:32[C");
    checkIoException("(chr:32[C");
    checkIoException("[chr:32(C");
  }

  protected void checkIoException(final String call) {
    try {
      Adjacency.parseAdjacency("chr1", 12, call);
      fail("Excepted exception when parsing: ");
    } catch (IOException ex) {
      assertEquals("Invalid .vcf adjacency call: " + call, ex.getMessage());
    }
  }

  public void testCompareTo() {
    // in strictly sorted order
    final Adjacency[] a = {
        new Adjacency("chr1", 10, true),
    new Adjacency("chr1", 10, false),
    new Adjacency("chr1", 11, true),
    new Adjacency("chr2", 10, true),
    };
    // check all pairs
    for (int i = 0; i < a.length; i++) {
      for (int j = 0; j < a.length; j++) {
        final int expect = Integer.signum(i - j);
        assertEquals(expect, a[i].compareTo(a[j]));
        if (expect == 0) {
          assertEquals(a[i].hashCode(), a[j].hashCode());
        }
      }
    }
  }
}
