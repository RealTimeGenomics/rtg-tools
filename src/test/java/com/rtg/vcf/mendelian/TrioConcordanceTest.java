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

import junit.framework.TestCase;

/**
 */
public class TrioConcordanceTest extends TestCase {

  public void testConcordance() {
    final TrioConcordance tc = new TrioConcordance("child", "father", "mother");

    for (int i = 0; i < 25; ++i) {
      tc.add(new Genotype("0/0"), new Genotype("0/0"), new Genotype("0/0"));
      tc.add(new Genotype("1/0"), new Genotype("0/1"), new Genotype("1/0"));
      tc.add(new Genotype("1/0"), new Genotype("0/1"), new Genotype("0/0"));
      tc.add(new Genotype("1/1"), new Genotype("0/1"), new Genotype("1/1"));
    }

    assertEquals(TrioConcordance.Status.OK, tc.getStatus(100, 99.0));

    tc.add(new Genotype("1/1"), new Genotype("0/0"), new Genotype("1/1"));
    assertEquals(TrioConcordance.Status.MOTHER, tc.getStatus(100, 99.5));
    assertEquals(TrioConcordance.Status.OK, tc.getStatus(200, 99.5));

    tc.add(new Genotype("1/1"), new Genotype("0/0"), new Genotype("0/0"));
    assertEquals(TrioConcordance.Status.BOTH, tc.getStatus(100, 99.5));
    assertEquals(TrioConcordance.Status.OK, tc.getStatus(200, 99.5));

    tc.add(new Genotype("1/1"), new Genotype("0/."), new Genotype("0/0"));
    tc.add(new Genotype("1/1"), new Genotype("./."), new Genotype("0/0"));
    assertEquals(TrioConcordance.Status.OK, tc.getStatus(105, 99.5));
    tc.add(new Genotype("1/1"), new Genotype("0/."), new Genotype("0/0"));
    assertEquals(TrioConcordance.Status.FATHER, tc.getStatus(105, 99.5));
  }

}
