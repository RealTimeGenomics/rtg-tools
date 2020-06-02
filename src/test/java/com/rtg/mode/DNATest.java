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

import java.util.Arrays;
import java.util.Locale;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class DNATest extends TestCase {

  public final void test() {
    TestUtils.testPseudoEnum(DNA.class, "[N, A, C, G, T]");
    try {
      DNA.valueOf('u');
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals("ch=u", e.getMessage());
    }
  }

  public final void testIsUnknown() {
    assertTrue(DNA.N.ignore());
    assertEquals(0, DNA.N.ordinal());
    assertTrue(DNA.valueOf("N").ignore());
    for (final DNA dna : DNA.values()) {
      if (dna != DNA.N) {
        assertFalse(dna.ignore());
      }
      if (dna.ignore()) {
        assertTrue(dna.ordinal() < SequenceType.DNA.firstValid());
      } else {
        assertTrue(dna.ordinal() >= SequenceType.DNA.firstValid());
      }
    }
  }

  public final void testType() {
    for (final DNA dna : DNA.values()) {
      assertEquals(SequenceType.DNA, dna.type());
    }
  }

  public final void testChars() {
    final char[] chars = DNA.valueChars();
    assertEquals('N', chars[0]);
    assertEquals('A', chars[1]);
    assertEquals('C', chars[2]);
    assertEquals('G', chars[3]);
    assertEquals('T', chars[4]);
    assertEquals(5, chars.length);
  }

  public final void testComplement() {
    for (final DNA dna : DNA.values()) {
      assertEquals(dna, dna.complement().complement());
    }
    assertEquals(DNA.N, DNA.N.complement());
    assertEquals(DNA.T, DNA.A.complement());
    assertEquals(DNA.G, DNA.C.complement());
    assertEquals(DNA.C, DNA.G.complement());
    assertEquals(DNA.A, DNA.T.complement());
    assertEquals(DNA.N.ordinal(), DNA.complement((byte) DNA.N.ordinal()));
    assertEquals(DNA.A.ordinal(), DNA.complement((byte) DNA.T.ordinal()));
  }

  public final void testCodeComplement() {
    for (final DNA dna : DNA.values()) {
      assertEquals(dna.complement().ordinal(), DNA.COMPLEMENT[dna.ordinal()]);
    }
  }

  public final void testToString() {
    for (final DNA dna : DNA.values()) {
      final String str = dna.toString();
      assertEquals(1, str.length());
      assertEquals(str.toUpperCase(Locale.getDefault()), str);
    }
  }

  public void testName() {
    assertEquals("T", DNA.T.name());
    assertEquals("N", DNA.N.name());
  }

  public void testComplementInPlace() {
    final byte[] nt = DNA.stringDNAtoByte("NACGTNACGT");
    assertEquals("[0, 1, 2, 3, 4, 0, 1, 2, 3, 4]", Arrays.toString(nt));
    DNA.reverseComplementInPlace(nt, 2, 5);
    assertEquals("[0, 1, 4, 0, 1, 2, 3, 2, 3, 4]", Arrays.toString(nt));
    DNA.complementInPlace(nt, 1, 2);
    assertEquals("[0, 4, 1, 0, 1, 2, 3, 2, 3, 4]", Arrays.toString(nt));

    final byte[] nt2 = DNA.stringDNAtoByte("ACGTNTCGA");
    assertEquals("[1, 2, 3, 4, 0, 4, 2, 3, 1]", Arrays.toString(nt2));
    DNA.reverseComplementInPlace(nt2);
    assertEquals("[4, 2, 3, 1, 0, 1, 2, 3, 4]", Arrays.toString(nt2));
  }

  public void testGetDNA() {
    assertEquals(0, DNA.getDNA('N'));
    assertEquals(0, DNA.getDNA('n'));
    assertEquals(1, DNA.getDNA('A'));
    assertEquals(1, DNA.getDNA('a'));
    assertEquals(2, DNA.getDNA('C'));
    assertEquals(2, DNA.getDNA('c'));
    assertEquals(3, DNA.getDNA('G'));
    assertEquals(3, DNA.getDNA('g'));
    assertEquals(4, DNA.getDNA('T'));
    assertEquals(4, DNA.getDNA('t'));
  }

  public void testPopulateComplementArray() {
    final byte[] comps = new byte[1];
    final DNA[] dna = {new DNA(0, DNASimple.N) {
      @Override
      public DNA complement() {
        return DNA.A;
      }
    }};
    DNA.populateComplementArray(comps, dna);
    assertEquals(DNA.A.ordinal(), comps[0]);
  }

  public void testStringDNAtoBytes() {
    assertEquals("[0, 1, 2, 3, 4]", Arrays.toString(DNA.stringDNAtoByte("NACGT")));
    assertEquals("[1, 1, 2, 3, 4]", Arrays.toString(DNA.stringDNAtoByte("AACGT")));
    assertEquals("[]", Arrays.toString(DNA.stringDNAtoByte("")));
  }

  public void testByteDNAtoByte() {
    assertEquals("[0, 1, 2, 3, 4]", Arrays.toString(DNA.byteDNAtoByte("NACGT".getBytes())));
    assertEquals("[1, 1, 2, 3, 4]", Arrays.toString(DNA.byteDNAtoByte("AACGT".getBytes())));
    assertEquals("[]", Arrays.toString(DNA.byteDNAtoByte("".getBytes())));
  }
}

