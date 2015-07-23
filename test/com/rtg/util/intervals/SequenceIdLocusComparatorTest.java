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
package com.rtg.util.intervals;

import junit.framework.TestCase;

/**
 */
public class SequenceIdLocusComparatorTest extends TestCase {


  public void testCompare() {
    final SequenceIdLocus pos1 = new SequenceIdLocusSimple(10, 100, 102);
    final SequenceIdLocusComparator comp = new SequenceIdLocusComparator();

    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(11, 99, 101)) < 0);
    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(9, 99, 101)) > 0);

    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(10, 100, 102)) == 0);
    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(10, 101, 103)) < 0);
    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(10, 99, 101)) > 0);

    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(10, 100, 110)) < 0);
    assertTrue(comp.compare(pos1, new SequenceIdLocusSimple(10, 100, 101)) > 0);

  }

}
