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

package com.rtg.vcf.eval;

import junit.framework.TestCase;

/**
 */
public class RocLineTest extends TestCase {


  void check(RocLine roc1, RocLine roc2) {
    assertTrue(roc1.compareTo(roc2) < 0);
    assertTrue(roc2.compareTo(roc1) > 0);
    assertTrue(roc1.compareTo(roc1) == 0);
    assertTrue(roc2.compareTo(roc2) == 0);
  }
  public void testPosterior() {
    final RocLine roc1 = new RocLine("1", 2, 3.5, 4, false);
    final RocLine roc2 = new RocLine("1", 2, 3, 4, false);
    check(roc1, roc2);
  }
  public void testSequence() {
    final RocLine roc1 = new RocLine("1", 2, 3, 4, false);
    final RocLine roc2 = new RocLine("2", 2, 3, 4, false);
    check(roc1, roc2);
  }
  public void testPosition() {
    final RocLine roc1 = new RocLine("1", 2, 3, 4, false);
    final RocLine roc2 = new RocLine("1", 3, 3, 4, false);
    check(roc1, roc2);
  }

  public void testCorrect() {
    final RocLine roc1 = new RocLine("1", 2, 3, 4, false);
    final RocLine roc2 = new RocLine("1", 2, 3, 4, true);
    check(roc1, roc2);
  }

  public void testFieldOrder() {
    RocLine roc1 = new RocLine("3", 6, 2, 4, true);
    final RocLine roc2 = new RocLine("2", 2, 6, 4, false);
    check(roc2, roc1);
    roc1 = new RocLine("3", 6, 7, 4, true);
    check(roc1, roc2);
    roc1 = new RocLine("1", 6, 6, 4, false);
    check(roc1, roc2);
    roc1 = new RocLine("1", 1, 6, 4, false);
    check(roc1, roc2);
  }
  public void testEquals() {
    final RocLine roc1 = new RocLine("3", 6, 2, 4, true);
    final RocLine roc2 = new RocLine("2", 2, 6, 4, false);
    assertFalse(roc1.equals(null));
    assertFalse(roc1.equals(roc2));
    assertTrue(roc1.equals(roc1));
  }

}
