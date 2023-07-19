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

package com.rtg.vcf;

import junit.framework.TestCase;

/**
 */
public class BreakpointAltTest extends TestCase {
  void check(String orig, String remoteChr, int remotePos, boolean localUp, boolean remoteUp) {
    final BreakpointAlt b = new BreakpointAlt(orig);
    assertEquals(remoteChr, b.getRemoteChr());
    assertEquals(remotePos, b.getRemotePos());
    assertEquals(localUp, b.isLocalUp());
    assertEquals(remoteUp, b.isRemoteUp());
    assertEquals("A", b.getRefSubstitution());
    assertEquals(orig, b.toString());
  }

  public void testBreakpointAlt() {
    check("A[foo:221[", "foo", 220, true, false);
    check("A]foo:221]", "foo", 220, true, true);
    check("[foo:221[A", "foo", 220, false, false);
    check("]foo:221]A", "foo", 220, false, true);
    check("]foo:261]A", "foo", 260, false, true);
    check("]bar:261]A", "bar", 260, false, true);
    check("A]foo:bar:221]", "foo:bar", 220, true, true);
  }
}
