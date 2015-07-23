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

import junit.framework.TestCase;

/**
 */
public class PointerFileLookupTest extends TestCase {
  public void testSpanning() {
    final int[][] pointers = {
            new int[] {0, 35, 60}, //sequences 0 and start of 1
            new int[] {20, 19},    //end of 1, sequence 2
            new int[] {40},        //cont
            new int[] {3}          //fin
    };
    final PointerFileLookup p = PointerFileLookup.generateLookup(pointers);
    assertEquals(0, p.lookup(0));
    assertEquals(0, p.lookup(1));
    assertEquals(1, p.lookup(2));
    assertEquals(3, p.lookup(3));
    assertEquals(0, p.startSeq(0));
    assertEquals(2, p.startSeq(1));
    assertEquals(3, p.startSeq(2));
    assertEquals(3, p.startSeq(3));
  }

  public void testNormal() {
    final int[][] pointers = {
            new int[] {0, 35, 60, 90},
            new int[] {20, 19, 40, 60},
            new int[] {40, 50, 60, 70},
            new int[] {30, 40, 100, 200}
    };
    final PointerFileLookup p = PointerFileLookup.generateLookup(pointers);
    assertEquals(0, p.lookup(0));
    assertEquals(0, p.lookup(1));
    assertEquals(0, p.lookup(2));
    assertEquals(1, p.lookup(3));
    assertEquals(1, p.lookup(4));
    assertEquals(1, p.lookup(5));
    assertEquals(2, p.lookup(6));
    assertEquals(2, p.lookup(7));
    assertEquals(2, p.lookup(8));
    assertEquals(3, p.lookup(9));
    assertEquals(3, p.lookup(10));
    assertEquals(3, p.lookup(11));
    assertEquals(3, p.lookup(12));
    assertEquals(0, p.startSeq(0));
    assertEquals(3, p.startSeq(1));
    assertEquals(6, p.startSeq(2));
    assertEquals(9, p.startSeq(3));
  }

  public void testSingleFile() {
    final int[][] pointers = {
            new int[] {0, 35, 60, 90, 100}, //sequences 0, 1, 2, 3
    };
    final PointerFileLookup p = PointerFileLookup.generateLookup(pointers);
    assertEquals(0, p.lookup(0));
    assertEquals(0, p.lookup(1));
    assertEquals(0, p.lookup(2));
    assertEquals(0, p.lookup(3));
    assertEquals(0, p.lookup(4));
    assertEquals(0, p.startSeq(0));
  }
  public void testDoubleFile() {
    final int[][] pointers = {
            new int[] {0, 35, 60, 90, 100}, //sequences 0, 1, 2, 3
            new int[] {0, 35, 60, 90, 100}, //sequences 4, 5, 6, 7
    };
    final PointerFileLookup p = PointerFileLookup.generateLookup(pointers);
    assertEquals(0, p.lookup(0));
    assertEquals(0, p.lookup(1));
    assertEquals(0, p.lookup(2));
    assertEquals(0, p.lookup(3));
    assertEquals(1, p.lookup(4));
    assertEquals(1, p.lookup(5));
    assertEquals(1, p.lookup(6));
    assertEquals(1, p.lookup(7));
    assertEquals(1, p.lookup(8));
    assertEquals(0, p.startSeq(0));
    assertEquals(4, p.startSeq(1));
  }
}
