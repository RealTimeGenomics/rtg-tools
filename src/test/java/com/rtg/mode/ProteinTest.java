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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import com.rtg.util.StringUtils;

import junit.framework.TestCase;


/**
 */
public class ProteinTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.Protein()}.
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  public final void test() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
    // Check toString of values
    final String methodName = "values";
    Method m = Protein.class.getMethod(methodName);
    final Protein[] r = (Protein[]) m.invoke(null);
    assertEquals("[X, *, A, R, N, D, C, Q, E, G, H, I, L, K, M, F, P, S, T, W, Y, V]", Arrays.toString(r));
    final String valueOfMethod = "valueOf";
    m = Protein.class.getMethod(valueOfMethod, String.class);
    // Check ordinal and valueOf
    for (int i = 0; i < r.length; ++i) {
      assertEquals(i, r[i].ordinal());
      if (r[i] != Protein.STOP) {
        assertEquals(r[i], m.invoke(null, r[i].toString()));
        assertEquals(Protein.valueOf(r[i].toString()), r[i]);
      }
    }
  }

  //Copied  from the IUPAC page
  private static final String STR = ""
    + "X\tXaa\tAny amino acid" + StringUtils.LS
    + "*\t***\tTranslated stop codon" + StringUtils.LS
    + "A\tAla\tAlanine" + StringUtils.LS
    + "R\tArg\tArginine" + StringUtils.LS
    + "N\tAsn\tAsparagine" + StringUtils.LS
    + "D\tAsp\tAspartic acid" + StringUtils.LS
    + "C\tCys\tCysteine" + StringUtils.LS
    + "Q\tGln\tGlutamine" + StringUtils.LS
    + "E\tGlu\tGlutamic acid" + StringUtils.LS
    + "G\tGly\tGlycine" + StringUtils.LS
    + "H\tHis\tHistidine" + StringUtils.LS
    + "I\tIle\tIsoleucine" + StringUtils.LS
    + "L\tLeu\tLeucine" + StringUtils.LS
    + "K\tLys\tLysine" + StringUtils.LS
    + "M\tMet\tMethionine" + StringUtils.LS
    + "F\tPhe\tPhenylalanine" + StringUtils.LS
    + "P\tPro\tProline" + StringUtils.LS
    + "S\tSer\tSerine" + StringUtils.LS
    + "T\tThr\tThreonine" + StringUtils.LS
    + "W\tTrp\tTryptophan" + StringUtils.LS
    + "Y\tTyr\tTyrosine" + StringUtils.LS
    + "V\tVal\tValine" + StringUtils.LS;

  /**
   * Test method for {@link com.rtg.mode.Protein()}.
   */
  public final void testNames() {
    final StringBuilder sb = new StringBuilder();
    for (final Protein pr : Protein.values()) {
      sb.append(pr.name()).append("\t").append(pr.threeLetter()).append("\t").append(pr.fullName()).append(StringUtils.LS);
      assertEquals(0, pr.compareTo(pr));
      assertFalse(pr.equals(null));
      assertTrue(pr.equals(pr));
      assertEquals(pr.ordinal(), pr.hashCode());
    }
    assertFalse(Protein.A.equals(Protein.C));
    assertEquals(STR, sb.toString());
  }

  /**
   * Test method for {@link com.rtg.mode.Protein#ignore()}.
   */
  public final void testIgnore() {
    final Protein unknown = Protein.X;
    final Protein stop = Protein.STOP;
    assertTrue(unknown.ignore());
    assertEquals(0, unknown.ordinal());
    assertTrue(Protein.valueOf("X").ignore());
    for (final Protein protein : Protein.values()) {
      if (protein != unknown && protein != stop) {
        assertFalse(protein.ignore());
      }
      if (protein.ignore()) {
        assertTrue(protein.ordinal() < SequenceType.PROTEIN.firstValid());
      } else {
        assertTrue(protein.ordinal() >= SequenceType.PROTEIN.firstValid());
      }
    }
  }

  /**
   * Test method for {@link com.rtg.mode.Protein#type()}.
   */
  public final void testType() {
    for (final Protein protein : Protein.values()) {
      assertEquals(SequenceType.PROTEIN, protein.type());
    }
  }

  /**
   * Test method for {@link com.rtg.mode.Protein#toString()}.
   */
  public final void testToString() {
    for (final Protein protein : Protein.values()) {
      final String str = protein.toString();
      assertEquals(1, str.length());
      assertEquals(str.toUpperCase(Locale.getDefault()), str);
    }
  }

}

