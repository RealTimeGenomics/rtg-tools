/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import com.rtg.mode.SequenceType;
import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class FastaWriterTest extends TestCase {

  public void test() throws Exception {
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaWriter f = new FastaWriter(out.lineWriter(), 0);
    f.write("0 foo", new byte[] {0, 1, 2, 3, 4}, new byte[] {20, 20, 20, 20, 20}, 5);
    f.write("1 foo/Left", new byte[] {0, 1, 2, 3, 4}, new byte[] {20, 20, 20, 20, 20}, 5);
    f.write("1 foo/Right", new byte[] {0, 1, 2, 3, 4}, new byte[] {20, 20, 20, 20, 20}, 5);
    assertEquals(">0 foo\nNACGT\n>1 foo/Left\nNACGT\n>1 foo/Right\nNACGT\n", out.toString());
  }

  public void testProtein() throws Exception {
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaWriter f = new FastaWriter(out.lineWriter(), 0, SdfSubseq.getByteMapping(SequenceType.PROTEIN, false));
    f.write("0 foo", new byte[] {0, 1, 2, 3, 4}, new byte[] {20, 20, 20, 20, 20}, 5);
    assertEquals(">0 foo\nX*ARN\n", out.toString());
  }
}
