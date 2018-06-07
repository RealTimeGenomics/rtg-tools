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

import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class EmptyStringNamesTest extends TestCase {

  public void testSomeMethod() {
    final EmptyStringNames thing = new EmptyStringNames(5);
    assertEquals(5, thing.length());
    assertEquals("", thing.name(3));
    final StringBuilder sb = new StringBuilder();
    thing.writeName(sb, 2);
    assertEquals("", sb.toString());
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    thing.writeName(baos, 0);
    assertEquals("", baos.toString());
    final SimpleNames prni = new SimpleNames();
    prni.setName(0, "");
    prni.setName(1, "");
    prni.setName(2, "");
    prni.setName(3, "");
    prni.setName(4, "");
    assertEquals(prni.calcChecksum(), thing.calcChecksum());
  }
}
