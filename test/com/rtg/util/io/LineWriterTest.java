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

package com.rtg.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 * Test class
 */
public class LineWriterTest extends TestCase {

  public void testWriting() throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final LineWriter lw = new LineWriter(new OutputStreamWriter(bos));
    lw.newLine();
    lw.writeln(65);
    lw.writeln("test");
    lw.writeln(new char[] {'o', 'f'});
    lw.writeln(new char[] {'f', 'o', 'o', 'l', 'i', 'n', 'e', 's'}, 3, 4);
    lw.writeln("barwriter class", 3, 6);
    lw.close();
    final String expect = "" + StringUtils.LS
        + "A" + StringUtils.LS
        + "test" + StringUtils.LS
        + "of" + StringUtils.LS
        + "line" + StringUtils.LS
        + "writer" + StringUtils.LS
        ;
    assertEquals(expect, bos.toString());
  }
}
