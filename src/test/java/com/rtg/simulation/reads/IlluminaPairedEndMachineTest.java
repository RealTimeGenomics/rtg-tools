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

import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class IlluminaPairedEndMachineTest extends TestCase {

  public void testProcessFragment() throws Exception {
    final IlluminaPairedEndMachine m = new IlluminaPairedEndMachine(42);
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
    m.setReadWriter(w);
    assertTrue(m.isPaired());
    m.setLeftReadLength(5);
    m.setRightReadLength(5);
    final byte[] frag = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    m.processFragment("name/", 30, frag, frag.length);
    assertEquals(">0 name/31/F/5./Left\nAAAAA\n>0 name/52/R/5./Right\nTTTTT\n", out.toString());
  }

  public void testProcessFragmentReadThrough() throws Exception {
    final IlluminaPairedEndMachine m = new IlluminaPairedEndMachine(42);
    final MemoryPrintStream out = new MemoryPrintStream();
    final FastaReadWriter w = new FastaReadWriter(out.lineWriter());
    m.setReadWriter(w);
    m.setLeftReadLength(55);
    m.setRightReadLength(55);
    final byte[] frag = {1, 1, 1, 1, 1};
    m.processFragment("name/", 30, frag, frag.length);
    assertEquals(">0 name/31/F/55./Left\nAAAAACGGTCTCGGCATTCCTGCTGAACCGCTCTTCCGATCTNNNNNTCTAGCCT\n>0 name/-19/R/55./Right\nTTTTTTGTGAGAAAGGGATGTGCTGCGAGAAGGCTAGANNNNNAGATCGGAAGAG\n", out.toString());
  }

  public void testBaseQuality() throws Exception {
    final IlluminaPairedEndMachine m = new IlluminaPairedEndMachine(42);
    m.setQualRange((byte) 15, (byte) 35);
    int qsummatch = 0;
    int qsummismatch = 0;
    final int n = 1000;
    for (int i = 0; i < n; ++i) {
      final byte correctCallQuality = m.getCorrectCallQuality((byte) 1);
      final byte missCallQuality = m.getMissCallQuality();
      //System.err.println(String.format(" = %2d  X %2d  %b", correctCallQuality, missCallQuality, correctCallQuality > missCallQuality));
      qsummatch += correctCallQuality;
      qsummismatch += missCallQuality;
    }
//    System.err.println("Avg match quality = " + qsummatch / n);
//    System.err.println("Avg mismatch quality = " + qsummismatch / n);
    assertTrue(qsummatch > qsummismatch);
  }
}
