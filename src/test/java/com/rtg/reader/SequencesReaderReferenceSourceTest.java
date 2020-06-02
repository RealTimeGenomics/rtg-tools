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

import java.io.IOException;

import com.rtg.AbstractTest;
import com.rtg.util.io.TestDirectory;

import htsjdk.samtools.SAMSequenceRecord;

/**
 */
public class SequencesReaderReferenceSourceTest extends AbstractTest {

  public void test() throws IOException {
    try (TestDirectory templ = new TestDirectory()) {
      ReaderTestUtils.getDNADir(">seq1\nactg\n>seq2\ngtca", templ);
      try (final SequencesReader r = SequencesReaderFactory.createDefaultSequencesReader(templ)) {
        final SequencesReaderReferenceSource sr = r.referenceSource();
        assertNull(sr.getReferenceBases(new SAMSequenceRecord("foo", 1), false));
        final byte[] s1 = sr.getReferenceBases(new SAMSequenceRecord("seq1", 1), false);
        assertNotNull(s1);
        assertEquals("ACTG", new String(s1));
        final byte[] s2 = sr.getReferenceBases(new SAMSequenceRecord("seq2", 1), false);
        assertNotNull(s2);
        assertEquals("GTCA", new String(s2));
      }
    }
  }
}
