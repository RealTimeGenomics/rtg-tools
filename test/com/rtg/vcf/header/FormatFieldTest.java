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
package com.rtg.vcf.header;

import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 * Test class
 */
public class FormatFieldTest extends TestCase {

  public void testSomeMethod() {
    final String io = "##FORMAT=<ID=yo, Number=5, Type=Float, Description=\"fun for the whole family\">";
    final FormatField f = new FormatField(io);
    assertEquals(io.replaceAll(",\\s+", ","), f.toString());
    assertEquals("yo", f.getId());
    assertEquals(5, f.getNumber().getNumber());
    assertEquals(MetaType.FLOAT, f.getType());
    assertEquals("fun for the whole family", f.getDescription());
    assertEquals(f, f.superSet(f));
    final FormatField ioI = new FormatField(io.replaceAll("Float", "Integer"));
    assertEquals("yo", ioI.getId());
    assertEquals(5, ioI.getNumber().getNumber());
    assertEquals(MetaType.INTEGER, ioI.getType());
    assertEquals("fun for the whole family", ioI.getDescription());
    assertEquals(ioI, ioI.superSet(ioI));
    assertEquals(f, ioI.superSet(f));
    assertEquals(f, f.superSet(ioI));

    final String io2 = "##FORMAT=<ID=%$&*,Number=A,Type=Flag,Description=\"oh rearry\">";
    final FormatField f2 = new FormatField(io2);
    assertEquals(io2, f2.toString());
    assertEquals("%$&*", f2.getId());
    assertEquals(-1, f2.getNumber().getNumber());
    assertEquals(VcfNumberType.ALTS, f2.getNumber().getNumberType());
    assertEquals(MetaType.FLAG, f2.getType());
    assertEquals("oh rearry", f2.getDescription());
    assertEquals(f2, f2.superSet(f2));
    assertNull(f2.superSet(f));
    assertNull(f.superSet(f2));


    final String io3 = "##FORMAT=<ID=%$&*,Type=Flag,Description=\"oh rearry\">";
    try {
      new FormatField(io3);
      fail("Should detect missing Number sub field");
    } catch (NoTalkbackSlimException e) {
      // expected
    }
  }
}
