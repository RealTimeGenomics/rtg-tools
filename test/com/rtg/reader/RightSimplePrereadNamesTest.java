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

import java.io.IOException;

import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 */
public class RightSimplePrereadNamesTest extends TestCase {
  public void testSomeMethod() throws IOException {
    final SimpleNames sprn = new SimpleNames();
    final RightSimpleNames rprn = new RightSimpleNames(sprn);
    sprn.setName(0, "first");
    sprn.setName(1, "second/1");
    sprn.setName(2, "third");
    sprn.setName(3, "fourth/1");
    rprn.setName(0, "f");
    rprn.setName(1, "sec");
    rprn.setName(2, "thi");
    rprn.setName(3, "fou");
    assertEquals(4L, sprn.length());
    assertEquals(4L, rprn.length());
    assertEquals("first", sprn.name(0));
    assertEquals("second/1", sprn.name(1));
    assertEquals("third", sprn.name(2));
    assertEquals("fourth/1", sprn.name(3));
    assertEquals("first", rprn.name(0));
    assertEquals("second/1", rprn.name(1));
    assertEquals("third", rprn.name(2));
    assertEquals("fourth/1", rprn.name(3));
    assertEquals(66, sprn.bytes());
    assertEquals(0, rprn.bytes());
    StringBuilder sb = new StringBuilder();
    sprn.writeName(sb, 2);
    assertEquals("third", sb.toString());
    MemoryPrintStream mps = new MemoryPrintStream();
    sprn.writeName(mps.outputStream(), 1);
    assertEquals("second/1", mps.toString());

    sb = new StringBuilder();
    rprn.writeName(sb, 2);
    assertEquals("third", sb.toString());
    mps = new MemoryPrintStream();
    rprn.writeName(mps.outputStream(), 1);
    assertEquals("second/1", mps.toString());
}

}
