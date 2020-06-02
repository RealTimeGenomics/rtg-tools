/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import com.rtg.util.Counter;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

/**
 * Tests the corresponding class.
 */
public class FilterVcfWriterTest extends TestCase {

  public void test() throws IOException {
    final VcfHeader h = new VcfHeader();
    final VcfRecord ra = new VcfRecord("pretend", 42, "A");
    final VcfRecord rt = new VcfRecord("pretend", 42, "T");
    Counter c = new Counter();
    final FilterVcfWriter w = new FilterVcfWriter(new VcfWriter() {
      @Override
      public void close() { }

      @Override
      public VcfHeader getHeader() {
        return h;
      }
      @Override
      public void write(VcfRecord record) {
        c.increment();
      }
    },
      new VcfFilter() {
      @Override
      public boolean accept(VcfRecord record) {
        return record.getRefCall().equals("A");
      }
      @Override
      public void setHeader(VcfHeader header) { }
    });

    assertEquals(h, w.getHeader());
    assertEquals(0, c.count());
    w.write(ra);
    assertEquals(1, c.count());
    w.write(ra);
    assertEquals(2, c.count());
    w.write(rt);
    assertEquals(2, c.count());
    w.write(rt);
    assertEquals(2, c.count());
    w.write(ra);
    assertEquals(3, c.count());
  }
}
