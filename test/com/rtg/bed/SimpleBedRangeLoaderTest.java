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

package com.rtg.bed;

import com.rtg.util.intervals.RangeList;

import junit.framework.TestCase;

/**
 */
public class SimpleBedRangeLoaderTest extends TestCase {

  public void testBedRecord() {
    BedRecord rec = new BedRecord("chr1", 2, 80, "anno1", "anno2");
    BedRangeLoader<String> l = new SimpleBedRangeLoader();

    assertEquals("anno1", l.getMeta(rec));

    rec = new BedRecord("chr1", 2, 80);
    assertEquals("chr1:3-80", l.getMeta(rec));

    rec = new BedRecord("chr1", 2, 2); // End is exclusive, so this is an odd record, being "before" the start
    assertEquals("chr1:3-2", l.getMeta(rec));
    RangeList.RangeData<String> r = l.getRangeData(rec);
    assertEquals(2, r.getStart());
    assertEquals(3, r.getEnd());
    assertEquals(1, r.getMeta().size());
    assertEquals("chr1:3-2", r.getMeta().get(0));
  }
}
