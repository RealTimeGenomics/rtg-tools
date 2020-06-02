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

import junit.framework.TestCase;

/**
 * Test class
 */
public class ContigFieldTest extends TestCase {

  public void testSomeMethod() {
    final String io = "##contig=<ID=yo,length=20>";
    final ContigField f = new ContigField(io);
    assertEquals(io.replaceAll(",\\s+", ","), f.toString());
    assertEquals("yo", f.getId());
    assertEquals(Integer.valueOf(20), f.getLength());
    assertEquals(f, f.superSet(f));
    final String io2 = "##contig=<ID=%$&*,length=17>";
    final ContigField f2 = new ContigField(io2);
    assertEquals(io2, f2.toString());
    assertEquals("%$&*", f2.getId());
    assertEquals(Integer.valueOf(17), f2.getLength());
    assertEquals(f2, f2.superSet(f2));
    assertNull(f.superSet(f2));
    assertNull(f2.superSet(f));

    final String io3 = "##contig=<ID=yo,assembly=b37>";
    final ContigField f3 = new ContigField(io3);
    assertEquals(io3, f3.toString());
    assertEquals("yo", f3.getId());
    assertEquals(null, f3.getLength());
    assertEquals(f3, f3.superSet(f3));
    assertNull(f2.superSet(f3));
    final ContigField f4 = f.superSet(f3);
    assertEquals("yo", f4.getId());
    assertEquals(Integer.valueOf(20), f4.getLength());
    assertEquals("##contig=<ID=yo,length=20,assembly=b37>", f4.toString());
    final ContigField f5 = f3.superSet(f);
    assertEquals(f4, f5);
  }
}
