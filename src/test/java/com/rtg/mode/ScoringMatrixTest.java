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

import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 */
public class ScoringMatrixTest extends TestCase {

  private ScoringMatrix mMatrix = null;

  @Override
  public void setUp() throws IOException, InvalidParamsException {
    mMatrix = new ProteinScoringMatrix("BLOSUM62");
  }

  @Override
  public void tearDown() {
    mMatrix = null;
  }


  public final void testScore() {
    check(4, Protein.A, Protein.A);
    check(-1, Protein.A, Protein.R);
    check(1, Protein.STOP, Protein.STOP);
    check(-1, Protein.K, Protein.D);
    check(2, Protein.M, Protein.L);
    check(0, Protein.X, Protein.A);
    check(-1, Protein.X, Protein.R);
    check(-2, Protein.X, Protein.C);
    check(-4, Protein.X, Protein.STOP);
  }

  private void check(final int sc, final Protein a, final Protein b) {
    assertEquals(sc, mMatrix.score(a.ordinal(), b.ordinal()));
    assertEquals(sc, mMatrix.score(b.ordinal(), a.ordinal()));
  }

  public final void testToStringStringBuilder() {
    final String str = mMatrix.toString();
    //System.err.println(str);
    assertTrue(str.startsWith("[-1, -4, 0, -1, -1, -1, -2,"));
    assertTrue(str.endsWith("-1, -2, -2, 0, -3, -1, 4]" + StringUtils.LS));
  }

}
