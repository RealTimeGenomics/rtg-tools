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

package com.rtg.reader;

import java.util.UUID;

import junit.framework.TestCase;

/**
 * Test class
 */
public class SdfIdTest extends TestCase {

  public void testSomeMethod() {
    SdfId id = new SdfId();
    assertTrue(id.available());
    assertFalse(id.check(new SdfId()));
    assertTrue(id.check(new SdfId(0L)));
    assertFalse(id.check(new SdfId(1L)));
    assertFalse(new SdfId(0L).equals(id));
    assertEquals(new SdfId(new UUID(id.getHighBits(), id.getLowBits())), id);
    assertTrue(id.toString().matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    assertTrue(0L != id.getLowBits());
    assertTrue(0L != id.getHighBits());
    id = new SdfId(555L);
    assertTrue(id.available());
    assertFalse(id.check(new SdfId()));
    assertTrue(id.check(new SdfId(0L)));
    assertFalse(id.check(new SdfId(1L)));
    assertFalse(new SdfId(0L).equals(id));
    assertFalse(new SdfId().equals(id));
    assertEquals(new SdfId(555L), id);
    assertTrue(id.toString().matches("^[1-9a-fA-F]([1-9a-fA-F]?){15}$"));
    assertEquals(555L, id.getLowBits());
    assertEquals(0L, id.getHighBits());
    id = new SdfId(0L);
    assertFalse(id.available());
    assertTrue(id.check(new SdfId()));
    assertTrue(id.check(new SdfId(0L)));
    assertTrue(id.check(new SdfId(1L)));
    assertEquals(new SdfId(0L), id);
    assertTrue(id.toString().matches("^0$"));
    assertEquals(0L, id.getLowBits());
    assertEquals(0L, id.getHighBits());
    id = new SdfId("ffffffff-ffff-ffff-ffff-ffffffffffff");
    assertFalse(id.check(new SdfId()));
    assertTrue(id.check(new SdfId(0L)));
    assertFalse(id.check(new SdfId(1L)));
    assertEquals("ffffffff-ffff-ffff-ffff-ffffffffffff", id.toString());
    assertEquals(-1L, id.getLowBits());
    assertEquals(-1L, id.getHighBits());
    id = new SdfId("ffffffff");
    assertFalse(id.check(new SdfId()));
    assertTrue(id.check(new SdfId(0L)));
    assertFalse(id.check(new SdfId(1L)));
    assertEquals("ffffffff", id.toString());
    assertEquals(0xffffffffL, id.getLowBits());
    assertEquals(0L, id.getHighBits());
    assertFalse(id.equals("1"));
  }

}
