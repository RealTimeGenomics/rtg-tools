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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.rtg.mode.DnaUtils;

/**
 */
public class FastqSequenceTest {
  @Test
  public void test() {
    final FastqSequence fastq = exampleFastq();
    assertEquals("something", fastq.getName());
    assertArrayEquals(DnaUtils.encodeString("ACGT"), fastq.getBases());
    assertArrayEquals(new byte[] {32, 33, 34, 35}, fastq.getQualities());
    assertEquals(4, fastq.length());
  }

  @Test
  public void toFasta() {
    final FastqSequence fastq = exampleFastq();
    assertEquals(">something\nACGT\n", fastq.toFasta());
  }

  @Test
  public void toFastq() {
    final FastqSequence fastq = exampleFastq();
    assertEquals("@something\nACGT\n+\nABCD\n", fastq.toFastq());
  }
  @Test
  public void toStringIsFastq() {
    final FastqSequence fastq = exampleFastq();
    assertEquals(fastq.toFastq(), fastq.toString());
  }

  @Test
  public void reverseComplement() {
    final FastqSequence fastq = getFastq("n", "TTTACGG", new byte[] {33, 33, 34, 35, 35, 33, 65});
    fastq.rc();
    assertEquals("CCGTAAA", DnaUtils.bytesToSequenceIncCG(fastq.getBases()));
    assertArrayEquals(new byte[] {65, 33, 35, 35, 34, 33, 33}, fastq.getQualities());
  }

  @Test
  public void trim() {
    final FastqSequence fastq = getFastq("n", "TTTACGG");
    fastq.trim(new LastBasesReadTrimmer(3));
    assertEquals("TTTA", DnaUtils.bytesToSequenceIncCG(fastq.getBases()));
    assertEquals(4, fastq.getQualities().length);
  }

  private FastqSequence exampleFastq() {
    return getFastq();
  }

  private FastqSequence getFastq() {
    return getFastq("something", "ACGT", FastaUtils.asciiToRawQuality("ABCD"));
  }

  static FastqSequence getFastq(String name, String bases) {
    return getFastq(name, bases, 64);
  }

  static FastqSequence getFastq(String name, String bases, int quality) {
    final byte[] basesStr = DnaUtils.encodeStringWithHyphen(bases);
    final byte[] qualities = new byte[basesStr.length];
    Arrays.fill(qualities, (byte) Math.min(quality, 64));
    return new FastqSequence(name, basesStr, qualities, qualities.length);
  }

  static FastqSequence getFastq(String name, String bases, byte[] qualities) {
    assert bases.length() == qualities.length;
    return new FastqSequence(name, DnaUtils.encodeStringWithHyphen(bases), qualities, bases.length());
  }
}