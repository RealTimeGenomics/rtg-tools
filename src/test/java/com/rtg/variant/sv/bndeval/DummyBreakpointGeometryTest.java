/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

package com.rtg.variant.sv.bndeval;

import static com.rtg.util.StringUtils.LS;

import com.rtg.util.TestUtils;

import junit.framework.TestCase;

/**
 */
public class DummyBreakpointGeometryTest extends TestCase {

  public void testMax() {
    assertEquals(2, AbstractBreakpointGeometry.max(+1, 2, 2));
    assertEquals(2, AbstractBreakpointGeometry.max(-1, 2, 2));
    assertEquals(3, AbstractBreakpointGeometry.max(+1, 2, 3));
    assertEquals(1, AbstractBreakpointGeometry.max(-1, 1, 2));
  }

  public void test() {
    final BreakpointGeometry bg = new BreakpointGeometry(Orientation.UU, "a", "b", 6, 14, 15, 24, 22, 31);
    final String exp = ""
        + "Break-point constraint:UU x=6,14:a y=15,24:b r=22,31";
    assertEquals(exp, bg.toString());
    final String gnu = ""
        + "7.0 15.0" + LS
        + "6.0 16.0" + LS
        + "6.0 24.0" + LS
        + "7.0 24.0" + LS
        + "14.0 17.0" + LS
        + "14.0 15.0" + LS
        + "7.0 15.0" + LS
        ;
    assertEquals(gnu, bg.gnuPlot().replace("\t", ""));
    assertEquals(System.identityHashCode(bg), bg.hashCode());
    assertEquals(50, bg.count());
    assertEquals(Integer.valueOf(6), bg.position("a"));
    assertEquals(Integer.valueOf(15), bg.position("b"));
    assertEquals(null, bg.position("x"));

    final String gnuf = ""
        + "16.0 6.0" + LS
        + "15.0 7.0" + LS
        + "15.0 14.0" + LS
        + "17.0 14.0" + LS
        + "24.0 7.0" + LS
        + "24.0 6.0" + LS
        + "16.0 6.0" + LS
        ;
    assertEquals(gnuf, bg.flip().gnuPlot().replace("\t", ""));
  }

  public void testEquals() {
    TestUtils.equalsTest(new AbstractBreakpointGeometry[] {
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 9, 10, 19, 11, 28),
        new BreakpointGeometry(Orientation.UU, "-", "b", 1, 9, 10, 19, 11, 28),
        new BreakpointGeometry(Orientation.UU, "a", "-", 1, 9, 10, 19, 11, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 0, 9, 10, 19, 11, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 10, 10, 19, 11, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 9, 11, 19, 12, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 9, 10, 20, 11, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 9, 10, 19, 12, 28),
        new BreakpointGeometry(Orientation.UU, "a", "b", 1, 9, 10, 19, 11, 27)
    });
  }

  public void testIntersectAndOverlap() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 10, 11, 11, 12);
    final BreakpointGeometry x = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 10, 11, 11, 12);
    checkIntersectAndOverlap(a, b, x);
  }

  public void testIntersectAndOverlapFailNamesX() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "x", "b", 1, 2, 10, 11, 11, 12);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testIntersectAndOverlapFailNamesY() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "y", 1, 2, 10, 11, 11, 12);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testBug1() {
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 10, 12, 11, 13);
    b.integrity();
    checkIntersectAndOverlap1(b, b, b);
  }

  public void testBug1a() {
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.DD, "a", "b", 2, 1, 12, 11, -14, -13);
    //System.err.println(b);
    //System.err.println("count=" + b.count());
    b.integrity();
    checkIntersectAndOverlap1(b, b, b);
  }

  public void testIntersectAndOverlapOrientation() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.DD, "a", "b", 2, 1, 12, 10, -14, -12);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testIntersectAndOverlapFailX() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 2, 3, 10, 12, 12, 14);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testIntersectAndOverlapFailY() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 12, 13, 13, 14);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testIntersectAndOverlapFailR() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 20, 10, 30, 10, 30);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 15, 20, 35, 31, 35);
    checkIntersectAndOverlap(a, b, null);
  }

  public void testIntersectAndOverlapR() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 110, 129, 110, 129, 220, 239);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 92, 111, 128, 147, 220, 239);
    final BreakpointGeometry c = new BreakpointGeometry(Orientation.UU, "a", "b", 110, 111, 128, 129, 238, 239);
    checkIntersectAndOverlap(a, b, c);
  }

  private void checkIntersectAndOverlap(BreakpointGeometry a, BreakpointGeometry b, BreakpointGeometry exp) {
    a.integrity();
    b.integrity();
    if (exp != null) {
      exp.integrity();
    }
    checkIntersectAndOverlap(a);
    checkIntersectAndOverlap(b);
    checkIntersectAndOverlap1(a, b, exp);
    checkIntersectAndOverlap1(b, a, exp);
    checkIntersectAndOverlap1(a.flip(), b.flip(), exp == null ? null : exp.flip());
    checkIntersectAndOverlap1(b.flip(), a.flip(), exp == null ? null : exp.flip());
  }

  private void checkIntersectAndOverlap(BreakpointGeometry a) {
    checkIntersectAndOverlap1(a, a, a);
    checkIntersectAndOverlap1(a.flip(), a.flip(), a.flip());
  }


  private void checkIntersectAndOverlap1(AbstractBreakpointGeometry a, AbstractBreakpointGeometry b, AbstractBreakpointGeometry exp) {
    a.integrity();
    b.integrity();
    if (exp != null) {
      exp.integrity();
    }
    //System.err.println("a= " + a);
    //System.err.println("b= " + b);
    //System.err.println("x= " + exp);
    assertEquals(exp, a.intersect(b));
    assertEquals(exp != null, a.overlap(b));
  }

  public void testInterAndOver() {
    checkInterAndOver1(new Interval(0, 0), new Interval(-1, 1), null);
    checkInterAndOver1(new Interval(-1, 1), new Interval(0, 0), null);
    checkInterAndOver1(new Interval(0, 2), new Interval(1, 1), null);

    checkInterAndOver(new Interval(0, 2), new Interval(2, 3), null);
    checkInterAndOver(new Interval(0, 2), new Interval(4, 5), null);

    checkInterAndOver(new Interval(0, 1), new Interval(1, 2), null);
    checkInterAndOver(new Interval(0, 2), new Interval(1, 3), new Interval(1, 2));
    checkInterAndOver(new Interval(0, 2), new Interval(2, 3), null);

    checkInterAndOver(new Interval(-3, 2), new Interval(-1, 3), new Interval(-1, 2));

  }

  private void checkInterAndOver(Interval a, Interval b, Interval exp) {
    checkInterAndOver(a);
    checkInterAndOver(b);
    checkInterAndOver1(a, b, exp);
    checkInterAndOver1(b, a, exp);
    checkInterAndOver1(a.negative(), b.negative(), exp == null ? null : exp.negative());
    checkInterAndOver1(b.negative(), a.negative(), exp == null ? null : exp.negative());
    checkInterAndOver1(a.negative(), b, null);
    checkInterAndOver1(b, a.negative(), null);

  }

  private void checkInterAndOver(Interval a) {
    checkInterAndOver1(a, a, a);
    checkInterAndOver1(a.negative(), a.negative(), a.negative());
    checkInterAndOver1(a, a.negative(), null);
    checkInterAndOver1(a.negative(), a, null);
  }

  private void checkInterAndOver1(Interval a, Interval b, Interval exp) {
    assertEquals(exp, AbstractBreakpointGeometry.intersect(a.getA(), a.getB(), b.getA(), b.getB()));
    assertEquals(exp != null, AbstractBreakpointGeometry.over(a.getA(), a.getB(), b.getA(), b.getB()));
  }

  public void testUnionSucceed() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 10, 12, 11, 13);
    final BreakpointGeometry x = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 13);
    checkUnion(a, b, x);
  }

  public void testUnionFailNamesX() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "x", "b", 1, 2, 10, 12, 11, 13);
    checkUnion(a, b, null);
  }

  public void testUnionFailNamesY() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "y", 1, 2, 10, 12, 11, 13);
    checkUnion(a, b, null);
  }

  public void testUnionFailOrientation() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.DD, "a", "b", 2, 1, 12, 10, -14, -12);
    checkUnion(a, b, null);
  }

  public void testUnionX() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 2, 3, 10, 12, 12, 14);
    final BreakpointGeometry x = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 3, 10, 12, 10, 14);
    checkUnion(a, b, x);
  }

  public void testUnionY() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 12, 10, 12);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 1, 2, 12, 13, 13, 14);
    final BreakpointGeometry x = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 2, 10, 13, 10, 14);
    checkUnion(a, b, x);
  }

  public void testUnionR() {
    final BreakpointGeometry a = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 20, 10, 30, 10, 30);
    final BreakpointGeometry b = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 20, 20, 40, 31, 40);
    final BreakpointGeometry x = new BreakpointGeometry(Orientation.UU, "a", "b", 0, 20, 10, 40, 10, 40);
    checkUnion(a, b, x);
  }

  private void checkUnion(BreakpointGeometry a, BreakpointGeometry b, BreakpointGeometry exp) {
    a.integrity();
    b.integrity();
    if (exp != null) {
      exp.integrity();
    }
    checkUnion(a);
    checkUnion(b);
    checkUnion1(a, b, exp);
    checkUnion1(b, a, exp);
    checkUnion1(a.flip(), b.flip(), exp == null ? null : exp.flip());
    checkUnion1(b.flip(), a.flip(), exp == null ? null : exp.flip());
  }

  private void checkUnion(BreakpointGeometry a) {
    checkUnion1(a, a, a);
    checkUnion1(a.flip(), a.flip(), a.flip());
  }


  private void checkUnion1(AbstractBreakpointGeometry a, AbstractBreakpointGeometry b, AbstractBreakpointGeometry exp) {
    assertEquals(exp, a.union(b));
  }

  public void testUnionStatic() {
    try {
      checkUnionStatic1(new Interval(0, 0), new Interval(-1, 1), null);
    } catch (final RuntimeException e) {
      //expected
    }

    checkUnionStatic(new Interval(0, 2), new Interval(2, 3), new Interval(0, 3));
    checkUnionStatic(new Interval(0, 2), new Interval(4, 5), new Interval(0, 5));

    checkUnionStatic(new Interval(0, 1), new Interval(1, 2), new Interval(0, 2));
    checkUnionStatic(new Interval(0, 2), new Interval(1, 3), new Interval(0, 3));
    checkUnionStatic(new Interval(0, 2), new Interval(2, 3), new Interval(0, 3));

    checkUnionStatic(new Interval(-3, 2), new Interval(-1, 3), new Interval(-3, 3));

  }

  private void checkUnionStatic(Interval a, Interval b, Interval exp) {
    checkUnionStatic(a);
    checkUnionStatic(b);
    checkUnionStatic1(a, b, exp);
    checkUnionStatic1(b, a, exp);
    checkUnionStatic1(a.negative(), b.negative(), exp == null ? null : exp.negative());
    checkUnionStatic1(b.negative(), a.negative(), exp == null ? null : exp.negative());
  }

  private void checkUnionStatic(Interval a) {
    checkUnionStatic1(a, a, a);
    checkUnionStatic1(a.negative(), a.negative(), a.negative());
  }

  private void checkUnionStatic1(Interval a, Interval b, Interval exp) {
    assertEquals(exp, AbstractBreakpointGeometry.union(a.getA(), a.getB(), b.getA(), b.getB()));
  }

  public void testIsInRange() {
    assertFalse(AbstractBreakpointGeometry.isInRange(0, 2, -1));
    assertTrue(AbstractBreakpointGeometry.isInRange(0, 2, 0));
    assertTrue(AbstractBreakpointGeometry.isInRange(0, 2, 1));
    assertFalse(AbstractBreakpointGeometry.isInRange(0, 2, 2));

    assertFalse(AbstractBreakpointGeometry.isInRange(1, -1, -1));
    assertTrue(AbstractBreakpointGeometry.isInRange(1, -1, 0));
    assertTrue(AbstractBreakpointGeometry.isInRange(1, -1, 1));
    assertFalse(AbstractBreakpointGeometry.isInRange(1, -1, 2));
  }

}
