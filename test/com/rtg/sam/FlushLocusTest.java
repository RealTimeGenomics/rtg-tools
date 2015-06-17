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

import junit.framework.TestCase;

/**
 */
public class FlushLocusTest extends TestCase {

  public void test() {
    final FlushLocus fla = new FlushLocus(0, 10);
    assertEquals("[0,10)", fla.toString());
  }

  public void testEqHash() {
    final FlushLocus fla = new FlushLocus(0, 10);
    final FlushLocus flb = new FlushLocus(5, 15);

    assertEquals(202, fla.hashCode());
    assertEquals(fla, fla);
    assertFalse(fla.equals(flb));
    assertFalse(flb.equals(fla));

    final FlushLocus flx = new FlushLocus(0, 11);
    final FlushLocus fly = new FlushLocus(1, 10);
    assertFalse(fla.equals(flx));
    assertFalse(flx.equals(fla));
    assertFalse(fla.equals(fly));
    assertFalse(fly.equals(fla));

  }

  public void testJoinable() {
    checkJoinable(new FlushLocus(0, 10), new FlushLocus(0, 10), new FlushLocus(0, 10));
    checkJoinable(new FlushLocus(0, 10), new FlushLocus(5, 15), new FlushLocus(0, 15));
    checkJoinable(new FlushLocus(0, 10), new FlushLocus(10, 20), new FlushLocus(0, 20));
    checkJoinable(new FlushLocus(0, 10), new FlushLocus(15, 25), null);

    assertEquals(202, new FlushLocus(0, 10).hashCode());
    assertEquals(new FlushLocus(0, 10), new FlushLocus(0, 10));
    assertFalse(new FlushLocus(0, 10).equals(new FlushLocus(5, 15)));
    assertFalse(new FlushLocus(5, 15).equals(new FlushLocus(0, 10)));

    final FlushLocus flx = new FlushLocus(0, 11);
    final FlushLocus fly = new FlushLocus(1, 10);
    assertFalse(new FlushLocus(0, 10).equals(flx));
    assertFalse(flx.equals(new FlushLocus(0, 10)));
    assertFalse(new FlushLocus(0, 10).equals(fly));
    assertFalse(fly.equals(new FlushLocus(0, 10)));

  }

  private void checkJoinable(final FlushLocus fla, final FlushLocus flb, final FlushLocus flc) {
    final boolean exp = flc != null;
    assertEquals(exp, fla.isJoinable(flb));
    assertEquals(exp, flb.isJoinable(fla));
    if (exp) {
      fla.join(flb);
      assertEquals(fla.mStart, flc.mStart);
      assertEquals(fla.mEnd, flc.mEnd);
    }
  }
}
