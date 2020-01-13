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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;

import junit.framework.TestCase;

/**
 * Test class
 */
public class CompressedMemorySequencesWriterTest extends TestCase {
  private static final String SEQUENCE_1 = "acgtacgtacgtacgt";
  private static final String QUALITY_1 =  "%^&*%^&*%^&*%^&*";
  private static final String SEQUENCE_2 = "cgatcgatcgatcgatcgat";
  private static final String QUALITY_2 =  "%^&*%^&*%^&*%^&*%^&*";
  private static final String FASTA = ">sequence_the_first\n" + SEQUENCE_1 + "\n>sequence_the_second\n" + SEQUENCE_2;
  private static final String FASTQ = "@sequence_the_first\n" + SEQUENCE_1 + "\n+sequence_the_first\n" + QUALITY_1 + "\n@sequence_the_second\n" + SEQUENCE_2 + "\n+sequence_the_second\n" + QUALITY_2;

  public void testFasta() throws IOException {
    final SequencesWriter sw = new SequencesWriter(getFastaSource(FASTA), null, PrereadType.UNKNOWN, true);
    final CompressedMemorySequencesReader cmsr = sw.processSequencesInMemory(null, true, new SimpleNames(), new SimpleNames(), LongRange.NONE);
    assertEquals(2, cmsr.numberSequences());
    assertEquals("sequence_the_first", cmsr.name(0));
    assertEquals(16L, cmsr.length(0));
    byte[] exp = {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4};
    byte[] res = cmsr.read(0);
    assertTrue("Exp: " + Arrays.toString(exp) + "\nact: " + Arrays.toString(res), Arrays.equals(exp, res));
    assertEquals("sequence_the_second", cmsr.name(1));
    assertEquals(20L, cmsr.length(1));
    exp = new byte[] {2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4};
    res = cmsr.read(1);
    assertTrue("Exp: " + Arrays.toString(exp) + "\nact: " + Arrays.toString(res), Arrays.equals(exp, res));
  }

  public void testFastq() throws IOException {
    final SequencesWriter sw = new SequencesWriter(getFastqSource(FASTQ), null, PrereadType.UNKNOWN, true);
    final CompressedMemorySequencesReader cmsr = sw.processSequencesInMemory(null, true, new SimpleNames(), new SimpleNames(), LongRange.NONE);
    assertEquals(2, cmsr.numberSequences());
    assertEquals("sequence_the_first", cmsr.name(0));
    assertEquals(16L, cmsr.length(0));
    byte[] exp = {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4};
    byte[] res = cmsr.read(0);
    assertTrue("Exp: " + Arrays.toString(exp) + "\nact: " + Arrays.toString(res), Arrays.equals(exp, res));
    byte[] expQual = {4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9};
    byte[] resQual = cmsr.readQuality(0);
    assertTrue("Exp: " + Arrays.toString(expQual) + "\nact: " + Arrays.toString(resQual), Arrays.equals(expQual, resQual));

    assertEquals("sequence_the_second", cmsr.name(1));
    assertEquals(20L, cmsr.length(1));
    exp = new byte[] {2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4, 2, 3, 1, 4};
    res = cmsr.read(1);
    assertTrue("Exp: " + Arrays.toString(exp) + "\nact: " + Arrays.toString(res), Arrays.equals(exp, res));
    expQual = new byte[] {4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9};
    resQual = cmsr.readQuality(1);
    assertTrue("Exp: " + Arrays.toString(expQual) + "\nact: " + Arrays.toString(resQual), Arrays.equals(expQual, resQual));
  }

  public void testFastqRegion() throws IOException {
    final SequencesWriter sw = new SequencesWriter(getFastqSource(FASTQ), null, PrereadType.UNKNOWN, true);
    final CompressedMemorySequencesReader cmsr = sw.processSequencesInMemory(null, true, new SimpleNames(), new SimpleNames(), new LongRange(0, 1));
    assertEquals(1, cmsr.numberSequences());
    assertEquals("sequence_the_first", cmsr.name(0));
    assertEquals(16L, cmsr.length(0));
    final byte[] exp = {1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4};
    final byte[] res = cmsr.read(0);
    assertTrue("Exp: " + Arrays.toString(exp) + "\nact: " + Arrays.toString(res), Arrays.equals(exp, res));
    final byte[] expQual = {4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9, 4, 61, 5, 9};
    final byte[] resQual = cmsr.readQuality(0);
    assertTrue("Exp: " + Arrays.toString(expQual) + "\nact: " + Arrays.toString(resQual), Arrays.equals(expQual, resQual));
  }

  public void testFastqRegionLarge() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Diagnostic.setLogStream(new PrintStream(baos));
    final SequencesWriter sw = new SequencesWriter(getFastqSource(FASTQ), null, PrereadType.UNKNOWN, true);
    final CompressedMemorySequencesReader cmsr = sw.processSequencesInMemory(null, true, new SimpleNames(), new SimpleNames(), new LongRange(0, 10));

    assertTrue("missing out of range message", baos.toString().contains("The end sequence id \"10\" is out of range, it must be from \"1\" to \"2\". Defaulting end to \"2\""));

    assertEquals(2, cmsr.numberSequences());
    assertEquals("sequence_the_first", cmsr.name(0));
    assertEquals(16L, cmsr.length(0));

    assertEquals("sequence_the_second", cmsr.name(1));
    assertEquals(20L, cmsr.length(1));
    Diagnostic.setLogStream();
  }

  private FastaSequenceDataSource getFastaSource(String str) {
    return new FastaSequenceDataSource(new ByteArrayInputStream(str.getBytes()), new DNAFastaSymbolTable());
  }


  private FastqSequenceDataSource getFastqSource(String str) {
    return new FastqSequenceDataSource(new ByteArrayInputStream(str.getBytes()), QualityFormat.SANGER);
  }
}
