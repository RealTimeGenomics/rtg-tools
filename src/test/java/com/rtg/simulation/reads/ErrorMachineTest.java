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

import junit.framework.TestCase;

/**
 */
public class ErrorMachineTest extends TestCase {

  public void testDuplication() throws Exception {

    final int[] counts = new int[1];

    final Machine testMachine = new DummyMachineTest.MockMachine() {
      @Override
      public void processFragment(String id, int fragmentStart, byte[] data, int length) {
        assertEquals((counts[0] > 0 ? "dupe-" /*+ (counts[0] + 1))*/ : "") + "blah1", id);
        counts[0]++;
      }

    };

    final ErrorMachine em = new ErrorMachine(0, testMachine, 1.0, 0.0);

    em.processFragment("blah1", 0, new byte[] {0, 1}, 1);

    assertEquals(2, counts[0]);
  }


  public void testChimera() throws Exception {

    final int[] counts = new int[1];

    final Machine testMachine = new DummyMachineTest.MockMachine() {
      @Override
      public void processFragment(String id, int fragmentStart, byte[] data, int length) {
        assertEquals("chimera" + counts[0] + "/", id);
        assertEquals(Integer.MIN_VALUE, fragmentStart);
        assertEquals(3, length);
        assertEquals(3, data.length);
        assertEquals(0, data[0]);
        assertEquals(3, data[1]);
        assertEquals(2, data[2]);
        counts[0]++;
      }
    };

    final ErrorMachine em = new ErrorMachine(0, testMachine, 0.0, 1.0);

    em.processFragment("blah1", 0, new byte[] {0, 1}, 1);
    em.processFragment("blah2", 500, new byte[] {3, 2}, 2);

    em.processFragment("blah1", 0, new byte[] {0, 1}, 1);
    em.processFragment("blah2", 500, new byte[] {3, 2}, 2);

    assertEquals(2, counts[0]);
  }
}
