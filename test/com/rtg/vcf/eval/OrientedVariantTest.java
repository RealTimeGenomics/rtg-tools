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
 * Test the corresponding class
 */
public class OrientedVariantTest extends TestCase {

  public void test() {
    final Variant v = new MockVariant(0, 0, new byte[] {0, 1, 2}, null);
    final OrientedVariant ov = new OrientedVariant(v, true);
    assertEquals("0:0 NAC+", ov.toString());
    assertTrue(ov.equals(ov));
    assertEquals(-1, ov.getStart());
    assertEquals(-1, ov.getEnd());
    assertEquals(3, ov.nt(true).length);
    assertEquals(3, ov.ntAlleleA().length);
    assertNull(ov.ntAlleleB());
    final OrientedVariant ov2 = new OrientedVariant(new MockVariant(1, 2, new byte[] {1, 1, 1}, new byte[] {2, 2, 2}), false);
    assertEquals("1:2 AAA:CCC-", ov2.toString());
    assertFalse(ov.equals(ov2));
    assertFalse(ov.hashCode() == ov2.hashCode());
    assertTrue(ov.isAlleleA());
    assertFalse(ov2.isAlleleA());
    assertEquals(0, ov.compareTo(ov));
    assertEquals(-1, ov.compareTo(ov2));
    assertEquals(1, ov2.compareTo(ov));
    assertFalse(ov.equals("not an OrientedVariant"));
    assertFalse(ov.equals(null));

    final Variant v2 = new MockVariant(0, 0, new byte[] {0, 1, 2}, new byte[] {1});
    OrientedVariant ov3 = new OrientedVariant(v2, true);
    OrientedVariant ov4 = new OrientedVariant(v2, false);
    assertEquals(1, ov3.compareTo(ov4));
    assertEquals(-1, ov4.compareTo(ov3));
  }
}
