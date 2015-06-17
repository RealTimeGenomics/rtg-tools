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

package com.rtg.util.arithcode;

import com.rtg.util.array.byteindex.ByteChunks;

import junit.framework.TestCase;

/**
 * Test for both <code>OutputBytes</code> and <code>InputBytes</code>
 */
public class BytesTest extends TestCase {

  private void write(OutputBytes ob, String str) {
    for (int i = 0; i < str.length(); i++) {
      ob.writeBit(str.charAt(i) == '1');
    }
  }

  private String read(InputBytes ib, int bits) {
    ib.integrity();
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bits; i++) {
      sb.append(ib.readBit() ? "1" : "0");
    }
    assertFalse(ib.readBit());
    return sb.toString();
  }

  public void test() {
    final ByteChunks bc = new ByteChunks(0);
    final OutputBytes ob = new OutputBytes(bc);

    final String s1 = "00101";
    write(ob, s1);
    assertEquals(1, ob.endBlock());
    final String s2 = "00101111" + "1100011";
    write(ob, s2);
    assertEquals(3, ob.endBlock());
    ob.close();

    assertEquals(3, bc.length());
    assertEquals(0x28, bc.get(0));
    assertEquals(0x2F, bc.get(1));
    assertEquals(0xC6, bc.get(2));

    assertEquals(s1 + "000", read(new InputBytes(bc, 0, 1), 8));
    assertEquals(s2 + "0", read(new InputBytes(bc, 1, 3), 16));

  }

  //test empty and finish on byte boundary
  public void test1() {
    final ByteChunks bc = new ByteChunks(0);
    final OutputBytes ob = new OutputBytes(bc);

    write(ob, "");
    assertEquals(0, ob.endBlock());
    final String s2 = "00101111";
    write(ob, s2);
    assertEquals(1, ob.endBlock());
    ob.close();

    assertEquals(1, bc.length());
    assertEquals(0x2F, bc.get(0));

    assertEquals("0", read(new InputBytes(bc, 0, 0), 1));
    assertEquals(s2, read(new InputBytes(bc, 0, 1), 8));
  }

}
