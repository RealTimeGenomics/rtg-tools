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

package com.rtg.bed;

import junit.framework.TestCase;

/**
 */
public class BedRecordTest extends TestCase {

  public void testBedRecord() {
    BedRecord rec = new BedRecord("chr1", 2, 80, "anno1", "anno2");
    assertEquals("chr1", rec.getSequenceName());
    assertEquals(2, rec.getStart());
    assertEquals(80, rec.getEnd());
    assertEquals(2, rec.getAnnotations().length);
    assertEquals("anno1", rec.getAnnotations()[0]);
    assertEquals("anno2", rec.getAnnotations()[1]);
    assertEquals("chr1\t2\t80\tanno1\tanno2", rec.toString());
    rec = new BedRecord("chr1", 2, 80);
    assertNotNull(rec.getAnnotations());
    assertEquals(0, rec.getAnnotations().length);
  }

  public void testBedParsing() {
    final BedRecord rec = BedRecord.fromString("chr1\t2\t80\tanno1\tanno2");
    assertEquals("chr1", rec.getSequenceName());
    assertEquals(2, rec.getStart());
    assertEquals(80, rec.getEnd());
    assertEquals(2, rec.getAnnotations().length);
    assertEquals("anno1", rec.getAnnotations()[0]);
    assertEquals("anno2", rec.getAnnotations()[1]);
    assertEquals("chr1\t2\t80\tanno1\tanno2", rec.toString());
    try {
      BedRecord.fromString("chr1\tads2\t123\tdasf");
      fail();
    } catch (NumberFormatException e) {
      //Expected
    }

    try {
      BedRecord.fromString("chr1");
      fail();
    } catch (ArrayIndexOutOfBoundsException e) {
      //Expected
    }
  }
}
