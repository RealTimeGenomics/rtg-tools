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
package com.rtg.sam;

import htsjdk.samtools.SAMRecord;

import junit.framework.TestCase;

/**
 */
public class SamFilterChainTest extends TestCase {

  public void testChaining() {
    final SamFilter filterAs = new SamFilter() {
      @Override
      public boolean acceptRecord(SAMRecord rec) {
        return !"a".equals(rec.getReadName());
      }
    };

    final SamFilter filterBs = new SamFilter() {
      @Override
      public boolean acceptRecord(SAMRecord rec) {
        return !"b".equals(rec.getReadName());
      }
    };

    final SamFilterChain chain = new SamFilterChain(filterAs, filterBs);

    final SAMRecord r1 = new SAMRecord(null);
    r1.setReadName("a");
    final SAMRecord r2 = new SAMRecord(null);
    r2.setReadName("b");
    final SAMRecord r3 = new SAMRecord(null);
    r3.setReadName("c");

    assertFalse(chain.acceptRecord(r1));
    assertFalse(chain.acceptRecord(r2));
    assertTrue(chain.acceptRecord(r3));
  }
}
