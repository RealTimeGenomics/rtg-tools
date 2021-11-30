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

package com.rtg.simulation.reads;

import java.io.IOException;
import java.util.Arrays;

import com.rtg.util.io.MemoryPrintStream;

/**
 * Test class
 */
public class IlluminaSingleEndMachineTest extends DummyIlluminaMachineTest {

  public void testPaired() throws Exception {
    final IlluminaSingleEndMachine m = new IlluminaSingleEndMachine(42);
    assertFalse(m.isPaired());
  }

  @Override
  public void test() throws Exception {
    final IlluminaSingleEndMachine m = (IlluminaSingleEndMachine) getMachine(42);
    try (final MemoryPrintStream out = new MemoryPrintStream()) {
      final FastaReadWriter w = new FastaReadWriter(out.lineWriter()) {
        @Override
        public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
          super.writeRead(name, data, qual, length);
          assertEquals("[20, 20, 20, 20]", Arrays.toString(qual));
        }
      };
      m.setReadWriter(w);
      final byte[] frag = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
      m.setReadLength(4);

      m.processFragment("name/", 30, frag, frag.length);
      assertEquals(">0 name/31/F/4.\nAAAA\n", out.toString());

      assertEquals(4, m.mResidueCount);
      assertEquals(4, m.mWorkspace.length);
    }
  }
}
