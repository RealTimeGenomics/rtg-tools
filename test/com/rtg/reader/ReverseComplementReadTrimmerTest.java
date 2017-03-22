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

package com.rtg.reader;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rtg.mode.DnaUtils;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.test.RandomDna;

/**
 */
public class ReverseComplementReadTrimmerTest {

  private void check(ReadTrimmer t, String q) {
    final PortableRandom r = new PortableRandom();
    final int length = q == null ? r.nextInt(100) : q.length();
    final String dna = RandomDna.random(length, r);
    final byte[] read = DnaUtils.encodeString(dna);
    final byte[] quals = q == null ? null : FastaUtils.asciiToRawQuality(q);

    assertEquals(length, t.trimRead(read, quals, length));
    assertEquals(DnaUtils.reverseComplement(dna), DnaUtils.bytesToSequenceIncCG(read));
    if (q != null) {
      assertEquals(StringUtils.reverse(q), FastaUtils.rawToAsciiString(quals));
    }
  }

  @Test
  public void testExamples() {
    final ReadTrimmer t = new ReverseComplementReadTrimmer();
    check(t, ";88/;5=8??<??;?=;2222*274=???0?;?????");
    check(t, ";88/");
    check(t, null);
  }
}