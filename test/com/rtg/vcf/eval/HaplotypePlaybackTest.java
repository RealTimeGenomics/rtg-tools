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

import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.SlimException;

import junit.framework.TestCase;

/**
 */
public class HaplotypePlaybackTest extends TestCase {

  public void testSimpleBase() {
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //snp C at 1
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2}, null), true));
    //insert G at 4
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(4, 4, new byte[]{3}, null), true));
    //delete length 1 at 6
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(6, 7, new byte[]{}, null), true));

    final byte[] expected = {2, 1, 1, 3, 1, 1, 2, 1, 1};
    check(expected, path);
  }

  public void testOutOfOrder() {
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //snp C at 1
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2}, null), true));
    //delete length 1 at 4
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(4, 5, new byte[]{}, null), true));
    //insert G at 4
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(4, 4, new byte[]{3}, null), true));

    final byte[] expected = {2, 1, 1, 3, 1, 1, 2, 1, 1};
    try {
      check(expected, path);
      fail();
    } catch (SlimException e) {
      assertTrue(e.getMessage().contains("Out of order"));
    }
  }

  public void testMoreComplex() {
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //mnp A -> CTT:GGG at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}), false));
    //insert A -> C:GA at 4
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(4, 5, new byte[]{2}, new byte[]{3, 1}), true));
    //delete A -> i:T at 6
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(6, 7, new byte[]{}, new byte[]{4}), false));

    final byte[] expected = {3, 3, 3, 1, 1, 2, 1, 4, 2, 1, 1};
    check(expected, path);
  }

  public void testInsertPriorToSnp() {
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //insert GGG at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(2, 2, new byte[]{3, 3, 3}, null), true));
    //snp C at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(2, 3, new byte[]{2}, null), true));

    final byte[] expected = {1, 3, 3, 3, 2, 1, 1, 1, 1, 2, 1, 1};
    check(expected, path);
  }


  public void testMoreComplexAlternate() {
                          // 1  2  3  4  5  6  7  8  9
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
                          // 2441  1  31 4     3  1  1
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //mnp A -> CTT:GGG at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}), true));
    //insert A -> C:GA at 4
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(4, 5, new byte[]{2}, new byte[]{3, 1}), false));

    //A -> G:T
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(5, 6, new byte[]{3}, new byte[]{4}), false));
    //delete A -> i:T
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(6, 7, new byte[]{}, new byte[]{4}), true));
    //C -> G:T
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(7, 8, new byte[]{3}, new byte[]{4}), true));

    final byte[] expected = {2, 4 , 4, 1, 1, 3, 1, 4, 3, 1, 1};
    TestUtils.containsAll(path.toString()
      , "HaplotypePlayback: position=-1 inPosition=-1"
      , "current:1-2 (CTT^:GGGv)"
      , "future:");
    check(expected, path);
    TestUtils.containsAll(path.toString()
      , "HaplotypePlayback: position=8 inPosition=-1"
      , "current:" + null
      , "future:");
  }

  public void testBackToBack() {
    final byte[] template = {1, 4, 1, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    // snp T -> T:G at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(2, 3, new byte[]{4}, new byte[]{3}), false));
    // insert CC after 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(3, 3, new byte[]{}, new byte[]{2, 2}), false));
    final byte[] expected = {1, 3, 2, 2, 1, 1, 1};
    TestUtils.containsAll(path.toString()
      , "HaplotypePlayback: position=-1 inPosition=-1"
      , "current:2-3 (Tv:G^)"
      , "future:[3-3 (v:CC^)]");
    check(expected, path);
    //System.out.println(path.toString());
    TestUtils.containsAll(path.toString()
      , "HaplotypePlayback: position=4 inPosition=-1"
      , "current:" + null
      , "future:");
  }

  private void check(byte[] expected, HaplotypePlayback path) {
    int i = 0;
    while (path.hasNext()) {
      path.next();
      //System.err.println(path.nt() + " " + path.toString());
      assertEquals(expected[i], path.nt());
      i++;
    }
    assertEquals(expected.length, i);
  }

  public void testCopy() {
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
                               // 2441  1  31 1     3  1  1
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //mnp A -> CTT:GGG at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}), true));
    assertEquals(path.toString(), path.copy().toString());

  }

  public void testPartialUpdated() {
                               // 1  2  3  4  5  6  7  8  9
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
                               // 2441  1  31 1     3  1  1
    final HaplotypePlayback path = new HaplotypePlayback(template);
    assertEquals(null, path.currentVariant());
    //mnp A -> CTT:GGG at 2
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}), true));
    //insert A -> C:GA at 4
    final OrientedVariant first = OrientedVariantTest.createOrientedVariant(new MockVariant(4, 5, new byte[]{2}, new byte[]{3, 1}), false);
    path.addVariant(first);

    final byte[] expected = {2, 4 , 4, 1, 1, 3, 1, 4, 3, 1, 1};
    int i = 0;
    while (i < 7) {
      path.next();
      //System.err.println(path.nt());
      assertEquals(expected[i], path.nt());
      i++;
    }
    assertEquals(first, path.currentVariant());
    //A -> G:T
    final OrientedVariant next = OrientedVariantTest.createOrientedVariant(new MockVariant(5, 6, new byte[]{3}, new byte[]{4}), false);
    path.addVariant(next);
    //delete A -> i:T
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(6, 7, new byte[]{}, new byte[]{4}), true));
    //C -> G:T
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(7, 8, new byte[]{3}, new byte[]{4}), true));

    while (path.templatePosition() < 4) {
      path.next();
      //System.err.println(path.nt());
      assertEquals(expected[i], path.nt());
      i++;
    }
    assertEquals(next, path.currentVariant());

    while (path.hasNext()) {
      path.next();
      //System.err.println(path.nt());
      assertEquals(expected[i], path.nt());
      i++;
    }
    assertEquals(expected.length, i);
  }

  public void testComparable() {
                               // 1  2  3  4  5  6  7  8  9
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
                               // 2441  1  31 1     3  1  1
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //mnp A -> CTT:GGG at 2
    final OrientedVariant ov1 = OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}, 1), true);
    path.addVariant(ov1);
    //insert A -> C:GA at 4
    final OrientedVariant ov3 = OrientedVariantTest.createOrientedVariant(new MockVariant(4, 5, new byte[]{2}, new byte[]{3, 1}, 3), false);
    path.addVariant(ov3);

    assertEquals(0, path.compareTo(path));
    final HaplotypePlayback copy = path.copy();
    assertTrue(path.equals(copy));
    assertTrue(path.hashCode() == copy.hashCode());
    final MockVariant mv4 = new MockVariant(5, 6, new byte[]{3}, new byte[]{4}, 4);
    copy.addVariant(OrientedVariantTest.createOrientedVariant(mv4, false));
    assertEquals(-1, path.compareTo(copy));
    assertEquals(1, copy.compareTo(path));
    assertFalse(path.equals(copy));
    assertFalse(path.hashCode() == copy.hashCode());
    assertFalse(path.equals(null));
    path.addVariant(OrientedVariantTest.createOrientedVariant(mv4, true));
    assertEquals(1, path.compareTo(copy));
    assertEquals(-1, copy.compareTo(path));

    final HaplotypePlayback pathAdvanced = path.copy();
    pathAdvanced.next();
    assertEquals(-1, path.compareTo(pathAdvanced));
    assertEquals(1, pathAdvanced.compareTo(path));

    final HaplotypePlayback path2 = new HaplotypePlayback(template);
    final HaplotypePlayback copy2 = path2.copy();
    assertEquals(0, path2.compareTo(copy2));

    path2.addVariant(ov1);
    assertEquals(1, path2.compareTo(copy2));
    assertEquals(-1, copy2.compareTo(path2));

    final OrientedVariant ov2 = OrientedVariantTest.createOrientedVariant(new MockVariant(2, 3, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}, 2), true);
    copy2.addVariant(ov2);
    assertEquals(-1, path2.compareTo(copy2));
    assertEquals(1, copy2.compareTo(path2));

  }
  public void testComparable2() {
                               // 1  2  3  4  5  6  7  8  9
    final byte[] template = {1, 1, 1, 1, 1, 1, 2, 1, 1};
                               // 2441  1  31 1     3  1  1
    final HaplotypePlayback path = new HaplotypePlayback(template);
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2, 4, 4}, new byte[]{3, 3, 3}), true));
    final HaplotypePlayback copy = path.copy();
    path.next();
    path.next();
    copy.next();
    assertEquals(1, path.compareTo(copy));
    assertEquals(-1, copy.compareTo(path));
  }

  public void testMoveForward() {
    final byte[] template = {1, 1, 1, 1, 2, 3, 4, 1, 2, 3, 4, 1, 1, 2, 1, 1};
    final HaplotypePlayback path = new HaplotypePlayback(template);
    //snp C at 0
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(1, 2, new byte[]{2}, null), true));
    //snp A at
    path.addVariant(OrientedVariantTest.createOrientedVariant(new MockVariant(14, 15, new byte[]{1}, null), true));

    final byte[] expected = {2, 1, 1, 1, 2, 3, 4, 1, 2, 3, 4, 1, 1, 1, 1, 1};
    int i = -1;
    while (path.templatePosition() < 4) {
      path.next();
      i++;
      //System.err.println(path.nt());
      assertEquals("position: " + i, expected[i], path.nt());
      assertEquals(i, path.templatePosition());
    }
    i = 9;
    path.moveForward(i);
    while (path.hasNext()) {
      path.next();
      i++;
      assertEquals("position: " + i, expected[i], path.nt());
      assertEquals(i, path.templatePosition());
    }
    assertEquals(expected.length - 1, i);
  }
}
