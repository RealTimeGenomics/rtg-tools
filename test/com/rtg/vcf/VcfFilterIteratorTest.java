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

package com.rtg.vcf;

import java.io.IOException;

import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class VcfFilterIteratorTest extends TestCase {

  private class ArrayVcfIterator implements VcfIterator {
    private final VcfHeader mHeader;
    private final VcfRecord[] mRecords;
    private int mPos;
    ArrayVcfIterator(VcfHeader h, VcfRecord... recs) {
      mHeader = h;
      mRecords = recs;
      mPos = 0;
    }
    @Override
    public VcfHeader getHeader() {
      return mHeader;
    }
    @Override
    public boolean hasNext() throws IOException {
      return mPos < mRecords.length;
    }
    @Override
    public VcfRecord next() throws IOException {
      return mRecords[mPos++];
    }
    @Override
    public void close() throws IOException { }
  }

  public void test() throws IOException {
    final VcfHeader h = new VcfHeader();
    final VcfRecord ra = new VcfRecord("pretend", 42, "A");
    final VcfRecord rt = new VcfRecord("pretend", 42, "T");
    final VcfFilterIterator r = new VcfFilterIterator(new ArrayVcfIterator(h, ra, ra, rt, ra), new VcfFilter() {
      @Override
      public boolean accept(VcfRecord record) {
        return record.getRefCall().equals("A");
      }
      @Override
      public void setHeader(VcfHeader header) { }
    });
    assertEquals(h, r.getHeader());
    assertTrue(r.hasNext());
    assertEquals(ra, r.next());
    assertTrue(r.hasNext());
    assertEquals(ra, r.next());
    assertTrue(r.hasNext());
    assertEquals(ra, r.next());
    assertFalse(r.hasNext());
  }
}
