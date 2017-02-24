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

package com.rtg.simulation.variants;

import com.rtg.util.PortableRandom;
import com.rtg.util.test.NotRandomRandom;

import junit.framework.TestCase;

/**
 */
public class MutatorSingleTest extends TestCase {

  public void testBad() {
    try {
      new MutatorSingle("X@");
      fail();
    } catch (final RuntimeException e) {
      assertEquals("X@", e.getMessage());
    }
  }
  private void expandCheck(String subject, String expected) {
    final String actual = MutatorSingle.expandSpec(subject);
    assertEquals(expected, actual);
  }
  public void testExpand() {
    expandCheck("5I3E4D", "IIIIIEEEDDDD");
    expandCheck("5IEIDEE4D", "IIIIIEIDEEDDDD");
    final String unchanged = "IDIEIXIXIXEIEIDIDDIDI";
    expandCheck(unchanged, unchanged);
    try {
      MutatorSingle.expandSpec("5I8");
      fail();
    } catch (RuntimeException e) {
    }
  }

  public void test1() {
    final MutatorSingle ms = new MutatorSingle("XX");
    ms.integrity();
    assertEquals("XX", ms.toString());
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, left.getFirstHaplotype());
    assertEquals("2:CG:CG", left.toString());
    assertEquals("2:TA:TA", right.toString());
  }

  public void test2() {
    final MutatorSingle ms = new MutatorSingle("YY");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, left.getFirstHaplotype());
    assertEquals("2:CG:CG", left.toString());
    assertEquals("2:GT:GT", right.toString());
  }

  public void test3() {
    final MutatorSingle ms = new MutatorSingle("I");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, left.getFirstHaplotype());
    assertEquals("0:A:A", left.toString());
    assertEquals("0:C:C", right.toString());
  }

  public void test4() {
    final MutatorSingle ms = new MutatorSingle("D");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, left.getFirstHaplotype());
    assertEquals("1::", left.toString());
    assertEquals("1::", right.toString());
  }

  public void test5() {
    final MutatorSingle ms = new MutatorSingle("J");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra, left.getFirstHaplotype());
    assertEquals("0:A:A", left.toString());
    assertEquals("0:G:G", right.toString());
  }

  public void test6() {
    final MutatorSingle ms = new MutatorSingle("==");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1, 2, 3, 4}, 2, ra, null);
    final MutatorResult right = ms.generateMutation(new byte[] {1, 2, 3, 4}, 2, ra, left.getFirstHaplotype());
    assertEquals("2:GT:GT", left.toString());
    assertEquals("2:GT:GT", right.toString());
  }

  public void test1end() {
    final MutatorSingle ms = new MutatorSingle("==");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1}, 0, ra, null);
    assertNull(left);
  }

  public void test1endE() {
    final MutatorSingle ms = new MutatorSingle("EE");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1}, 0, ra, null);
    assertNull(left);
  }

  public void test2end() {
    final MutatorSingle ms = new MutatorSingle("XX");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1}, 0, ra, null);
    assertNull(left);
  }

  public void test3end() {
    final MutatorSingle ms = new MutatorSingle("YY");
    ms.integrity();
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult left = ms.generateMutation(new byte[] {1}, 0, ra, null);
    assertNull(left);
  }


  public void testRandom() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.random(random));
    assertEquals(2, MutatorSingle.random(random));
    assertEquals(3, MutatorSingle.random(random));
    assertEquals(4, MutatorSingle.random(random));
    assertEquals(1, MutatorSingle.random(random));
  }

  public void testMinusa() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(2, MutatorSingle.minus(random, (byte) 1));
    assertEquals(3, MutatorSingle.minus(random, (byte) 1));
    assertEquals(4, MutatorSingle.minus(random, (byte) 1));
    assertEquals(2, MutatorSingle.minus(random, (byte) 1));
    assertEquals(3, MutatorSingle.minus(random, (byte) 1));
  }

  public void testMinusb() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.minus(random, (byte) 2));
    assertEquals(3, MutatorSingle.minus(random, (byte) 2));
    assertEquals(4, MutatorSingle.minus(random, (byte) 2));
    assertEquals(1, MutatorSingle.minus(random, (byte) 2));
    assertEquals(3, MutatorSingle.minus(random, (byte) 2));
  }

  public void testMinusc() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.minus(random, (byte) 4));
    assertEquals(2, MutatorSingle.minus(random, (byte) 4));
    assertEquals(3, MutatorSingle.minus(random, (byte) 4));
    assertEquals(1, MutatorSingle.minus(random, (byte) 4));
    assertEquals(2, MutatorSingle.minus(random, (byte) 4));
  }

  public void testMinus2a() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(2, MutatorSingle.minus(random, (byte) 1, (byte) 3));
    assertEquals(4, MutatorSingle.minus(random, (byte) 1, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 1, (byte) 3));
    assertEquals(4, MutatorSingle.minus(random, (byte) 1, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 1, (byte) 3));
  }

  public void testMinus2b() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(3, MutatorSingle.minus(random, (byte) 1, (byte) 2));
    assertEquals(4, MutatorSingle.minus(random, (byte) 1, (byte) 2));
    assertEquals(3, MutatorSingle.minus(random, (byte) 1, (byte) 2));
    assertEquals(4, MutatorSingle.minus(random, (byte) 1, (byte) 2));
    assertEquals(3, MutatorSingle.minus(random, (byte) 1, (byte) 2));
  }

  public void testMinus2c() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.minus(random, (byte) 3, (byte) 4));
    assertEquals(2, MutatorSingle.minus(random, (byte) 3, (byte) 4));
    assertEquals(1, MutatorSingle.minus(random, (byte) 3, (byte) 4));
    assertEquals(2, MutatorSingle.minus(random, (byte) 3, (byte) 4));
    assertEquals(1, MutatorSingle.minus(random, (byte) 3, (byte) 4));
  }

  public void testMinus2d() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.minus(random, (byte) 3, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 3, (byte) 3));
    assertEquals(4, MutatorSingle.minus(random, (byte) 3, (byte) 3));
    assertEquals(1, MutatorSingle.minus(random, (byte) 3, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 3, (byte) 3));
  }

  public void testMinus2e() {
    final PortableRandom random = new NotRandomRandom();
    assertEquals(1, MutatorSingle.minus(random, (byte) 4, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 4, (byte) 3));
    assertEquals(1, MutatorSingle.minus(random, (byte) 4, (byte) 3));
    assertEquals(2, MutatorSingle.minus(random, (byte) 4, (byte) 3));
    assertEquals(1, MutatorSingle.minus(random, (byte) 4, (byte) 3));
  }


}
