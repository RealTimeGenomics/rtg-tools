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

package com.rtg.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.Test;

import com.rtg.mode.DnaUtils;

/**
 */
public class FastqIteratorTest {
  private static final String FASTQ = "@name\nACGTACGT\n+name\nBBBBBBBB\n"
    + "@second\nATTT\n+second\nAAAA\n";

  static void checkRead(FastqSequence sequence, String name, String bases) {
    assertEquals(name, sequence.getName());
    assertTrue(String.format("Expected <%s> but was <%s>", bases, DnaUtils.bytesToSequenceIncCG(sequence.getBases())),
      Arrays.equals(DnaUtils.encodeString(bases), sequence.getBases()));
  }

  @Test
  public void testEmptyDataSource() {
    final FastqSequenceDataSource fastqSequenceDataSource = new FastqSequenceDataSource(new ByteArrayInputStream(new byte[0]), QualityFormat.SOLEXA);
    final FastqIterator fastqIterator = new FastqIterator(fastqSequenceDataSource);
    assertFalse(fastqIterator.hasNext());
  }
  @Test
  public void testSequenceDataSource() {
    final FastqSequenceDataSource fastqSequenceDataSource = new FastqSequenceDataSource(new ByteArrayInputStream(FASTQ.getBytes()), QualityFormat.SOLEXA);
    final FastqIterator fastqIterator = new FastqIterator(fastqSequenceDataSource);
    assertTrue(fastqIterator.hasNext());
    checkRead(fastqIterator.next(), "name", "ACGTACGT");
    assertTrue(fastqIterator.hasNext());
    checkRead(fastqIterator.next(), "second", "ATTT");
    assertFalse(fastqIterator.hasNext());
  }

}
