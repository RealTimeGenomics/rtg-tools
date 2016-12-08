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

import static com.rtg.mode.TranslatedFrame.FORWARD1;
import static com.rtg.mode.TranslatedFrame.FORWARD2;
import static com.rtg.mode.TranslatedFrame.FORWARD3;
import static com.rtg.mode.TranslatedFrame.REVERSE1;
import static com.rtg.mode.TranslatedFrame.REVERSE2;
import static com.rtg.mode.TranslatedFrame.REVERSE3;
import static com.rtg.util.StringUtils.LS;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class TranslatedFrameTest extends TestCase {

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame}.
   */
  public final void test() {
    TestUtils.testPseudoEnum(TranslatedFrame.class, "[FORWARD1, FORWARD2, FORWARD3, REVERSE1, REVERSE2, REVERSE3]");
    assertEquals(FORWARD1, REVERSE1.getReverse());
    assertEquals(FORWARD2, REVERSE2.getReverse());
    assertEquals(FORWARD3, REVERSE3.getReverse());
    assertEquals(REVERSE1, FORWARD1.getReverse());
    assertEquals(REVERSE2, FORWARD2.getReverse());
    assertEquals(REVERSE3, FORWARD3.getReverse());
    final byte[] codon = TranslatedFrame.populateCodonAminoArray(new DNA[0]);
    assertEquals(512, codon.length);
    for (byte aCodon : codon) {
      assertEquals(-1, aCodon);
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame}.
   */
  public final void test1() {
    final int l = DNA.values().length;
    final int v = 1 << TranslatedFrame.DNA_UNKNOWN_BITS;
    assertTrue(v >= l);
    final int vm = 1 << TranslatedFrame.DNA_UNKNOWN_BITS - 1;
    assertTrue(vm < l);
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#display()}.
   */
  public final void testCode() {
    for (final TranslatedFrame bi : TranslatedFrame.values()) {
      assertEquals(bi, TranslatedFrame.frameFromCode(bi.ordinal()));
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#phase()}.
   */
  public final void testPhase() {
    for (final TranslatedFrame bi : TranslatedFrame.values()) {
      assertTrue(bi.phase() >= 0 && bi.phase() < 3);
    }
    assertEquals(0, FORWARD1.phase());
    assertEquals(1, FORWARD2.phase());
    assertEquals(2, FORWARD3.phase());
    assertEquals(0, REVERSE1.phase());
    assertEquals(1, REVERSE2.phase());
    assertEquals(2, REVERSE3.phase());
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#display()}.
   */
  public final void testDisplay() {
    assertEquals("+1", FORWARD1.display());
    assertEquals("+2", FORWARD2.display());
    assertEquals("+3", FORWARD3.display());
    assertEquals("-1", REVERSE1.display());
    assertEquals("-2", REVERSE2.display());
    assertEquals("-3", REVERSE3.display());
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#isForward()}.
   */
  public final void testIsForward() {
    assertTrue(FORWARD1.isForward());
    assertTrue(FORWARD2.isForward());
    assertTrue(FORWARD3.isForward());
    assertFalse(REVERSE1.isForward());
    assertFalse(REVERSE2.isForward());
    assertFalse(REVERSE3.isForward());
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#frameFromCode(int)}.
   */
  public final void testFrameFromCodeBad() {
    try {
      TranslatedFrame.frameFromCode(-1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      TranslatedFrame.frameFromCode(6);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeF1() {
    checkCode(FORWARD1, 0, new Protein[] {Protein.X, Protein.T, Protein.R});
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeF2() {
    checkCode(FORWARD2, 1, new Protein[] {Protein.T, Protein.R, Protein.V});
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeF3() {
    checkCode(FORWARD3, 2, new Protein[] {Protein.R, Protein.V, Protein.Y});
  }

  private void checkCode(final Frame f, final int offset, final Protein[] exp) {
    final int length = 5 + offset;
    final byte[] codes = {0, 1, 2, 3, 4, 1, 2};
    assertEquals(exp[0].ordinal(), f.code(codes, length, 0));
    assertEquals(exp[1].ordinal(), f.code(codes, length, 1));
    assertEquals(exp[2].ordinal(), f.code(codes, length, 2));
    try {
      f.code(codes, length, length);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      f.code(codes, length, -1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeR1() {
    checkCodeR(REVERSE1, new Protein[] {Protein.V, Protein.Y, Protein.T});
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeR2() {
    checkCodeR(REVERSE2, new Protein[] {Protein.Y, Protein.T, Protein.R});
  }

  /**
   * Test method for {@link com.rtg.mode.TranslatedFrame#code(byte[], int, int)}.
   */
  public final void testCodeR3() {
    checkCodeR(REVERSE3, new Protein[] {Protein.T, Protein.R, Protein.V});
  }

  private void checkCodeR(final Frame f, final Protein[] exp) {
    final int length = 7;
    final byte[] codes = {0, 1, 2, 3, 4, 1, 2};
    assertEquals(exp[0].ordinal(), f.code(codes, length, 0));
    assertEquals(exp[1].ordinal(), f.code(codes, length, 1));
    assertEquals(exp[2].ordinal(), f.code(codes, length, 2));
    try {
      f.code(codes, length, length);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
    try {
      f.code(codes, length, -1);
      fail("RuntimeException expected");
    } catch (final RuntimeException e) {
      //expected
    }
  }

  /**
   * This table was taken from the "inverse table" in "http://en.wikipedia.org/wiki/Codon" with a bit of editting
   * to change the stop/start codons to unknown, to change U to T (so it is DNA rather than RNA)
   * and to change the layout a bit (single column and put in alphabetical order).
   */
  private static final String CODON_TABLE = ""
    + "Xaa/X AAN, AGN, ATN, CAN, GAN, TAN, TGN, TTN" + LS
    + "***/* TAA, TAG, TGA" + LS
    + "Ala/A GCA, GCC, GCG, GCN, GCT" + LS
    + "Arg/R AGA, AGG, CGA, CGC, CGG, CGN, CGT" + LS
    + "Asn/N AAC, AAT" + LS
    + "Asp/D GAC, GAT" + LS
    + "Cys/C TGC, TGT" + LS
    + "Gln/Q CAA, CAG" + LS
    + "Glu/E GAA, GAG" + LS
    + "Gly/G GGA, GGC, GGG, GGN, GGT" + LS
    + "His/H CAC, CAT" + LS
    + "Ile/I ATA, ATC, ATT" + LS
    + "Leu/L CTA, CTC, CTG, CTN, CTT, TTA, TTG" + LS
    + "Lys/K AAA, AAG" + LS
    + "Met/M ATG" + LS
    + "Phe/F TTC, TTT" + LS
    + "Pro/P CCA, CCC, CCG, CCN, CCT" + LS
    + "Ser/S AGC, AGT, TCA, TCC, TCG, TCN, TCT" + LS
    + "Thr/T ACA, ACC, ACG, ACN, ACT" + LS
    + "Trp/W TGG" + LS
    + "Tyr/Y TAC, TAT" + LS
    + "Val/V GTA, GTC, GTG, GTN, GTT" + LS;


  /**
   * Compare the DNA and byte versions
   */
  public final void testCodonToAmino1() {
    assertEquals(512, TranslatedFrame.CODON_TO_AMINO.length);
    for (final DNA c1 : DNA.values()) {
      for (final DNA c2 : DNA.values()) {
        for (final DNA c3 : DNA.values()) {
          final Protein pr = TranslatedFrame.codonToAmino(c1, c2, c3);
          final byte prb = TranslatedFrame.codonToAmino((byte) c1.ordinal(), (byte) c2.ordinal(), (byte) c3.ordinal());
          assertEquals(pr.ordinal(), prb);
        }
      }
    }
  }

  public final void testCodonToAmino() {
    final Map<Protein, SortedSet<String>> map = new TreeMap<>();
    for (final DNA c1 : DNA.values()) {
      for (final DNA c2 : DNA.values()) {
        for (final DNA c3 : DNA.values()) {
          final String s = "" + c1 + c2 + c3;
          final Protein pr = TranslatedFrame.codonToAmino(c1, c2, c3);
          if (c1.ignore() || c2.ignore()) {
            assertTrue(s + ":" + pr, pr.ignore());
          } else {
            final SortedSet<String> set = map.get(pr);
            final SortedSet<String> ns;
            if (set == null) {
              ns = new TreeSet<>();
              map.put(pr, ns);
            } else {
              ns = set;
            }
            ns.add(s);
          }
        }
      }
    }

    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<Protein, SortedSet<String>> e : map.entrySet()) {
      final Protein pr = e.getKey();
      final SortedSet<String> set = e.getValue();
      sb.append(pr.threeLetter()).append("/").append(pr.toString()).append(" ");
      int i = 0;
      for (final String s : set) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(s);
        ++i;
      }
      sb.append(LS);
    }
    assertEquals(CODON_TABLE, sb.toString());
  }


  public void testFirstValidForward() {
    final Frame f = FORWARD1;
    assertEquals(0, f.calculateFirstValid(0, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(2, f.calculateFirstValid(1, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(1, f.calculateFirstValid(2, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(0, f.calculateFirstValid(3, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(2, f.calculateFirstValid(4, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(1, f.calculateFirstValid(5, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(0, f.calculateFirstValid(6, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(2, f.calculateFirstValid(7, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(1, f.calculateFirstValid(8, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(0, f.calculateFirstValid(9, Integer.MAX_VALUE, Integer.MIN_VALUE));
    assertEquals(2, f.calculateFirstValid(10, Integer.MAX_VALUE, Integer.MIN_VALUE));
  }

//  public void testLastValidForward() {
//    final Frame f = TranslatedFrame.FORWARD1;
//    assertEquals(5, f.calculateLastValid(1, 7, 10));
//    assertEquals(5, f.calculateLastValid(4, 6, 10));
//    assertEquals(6, f.calculateLastValid(0, 6, 10));
//    assertEquals(9, f.calculateLastValid(0, 9, 10));
//    assertEquals(6, f.calculateLastValid(0, 8, 10));
//    assertEquals(6, f.calculateLastValid(0, 7, 10));
//    assertEquals(2, f.calculateLastValid(4, 3, 10));
//    assertEquals(2, f.calculateLastValid(4, 4, 10));
//    assertEquals(5, f.calculateLastValid(4, 5, 10));
//    assertEquals(6, f.calculateLastValid(0, 7, 11));
//    assertEquals(3, f.calculateLastValid(0, 5, 11));
//    assertEquals(3, f.calculateLastValid(0, 4, 11));
//    assertEquals(3, f.calculateLastValid(3, 5, 11));
//  }
//
//  public void testFirstValidReverse() {
//    final Frame f = TranslatedFrame.REVERSE1;
//    assertEquals(0, f.calculateFirstValid(1, 7, 10));
//    assertEquals(0, f.calculateFirstValid(4, 6, 10));
//    assertEquals(1, f.calculateFirstValid(0, 6, 10));
//    assertEquals(1, f.calculateFirstValid(0, 9, 10));
//    assertEquals(1, f.calculateFirstValid(0, 8, 10));
//    assertEquals(1, f.calculateFirstValid(0, 7, 10));
//    assertEquals(0, f.calculateFirstValid(4, 3, 10));
//    assertEquals(0, f.calculateFirstValid(4, 4, 10));
//    assertEquals(0, f.calculateFirstValid(4, 5, 10));
//    assertEquals(2, f.calculateFirstValid(0, 7, 11));
//    assertEquals(2, f.calculateFirstValid(0, 5, 11));
//    assertEquals(2, f.calculateFirstValid(0, 4, 11));
//    assertEquals(2, f.calculateFirstValid(3, 5, 11));
//  }

  public void testLastValidReverse() {
    final Frame f = REVERSE1;
    assertEquals(6, f.calculateLastValid(1, 7, 10));
    assertEquals(6, f.calculateLastValid(4, 6, 10));
    assertEquals(4, f.calculateLastValid(0, 6, 10));
    assertEquals(7, f.calculateLastValid(0, 9, 10));
    assertEquals(7, f.calculateLastValid(0, 8, 10));
    assertEquals(7, f.calculateLastValid(0, 7, 10));
    assertEquals(3, f.calculateLastValid(4, 3, 10));
    assertEquals(3, f.calculateLastValid(4, 4, 10));
    assertEquals(3, f.calculateLastValid(4, 5, 10));
    assertEquals(5, f.calculateLastValid(0, 7, 11));
    assertEquals(5, f.calculateLastValid(0, 5, 11));
    assertEquals(2, f.calculateLastValid(0, 4, 11));
    assertEquals(5, f.calculateLastValid(3, 5, 11));
  }
}

