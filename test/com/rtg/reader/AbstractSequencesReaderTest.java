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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.DnaUtils;
import com.rtg.mode.Protein;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Tests for <code>SequencesReader</code> implementations
 */
public abstract class AbstractSequencesReaderTest extends TestCase {

  protected SequencesReader createSequencesReader(final File dir) throws IOException {
    return createSequencesReader(dir, LongRange.NONE);
  }

  protected abstract SequencesReader createSequencesReader(final File dir, LongRange region) throws IOException;

  protected File mDir;

  @Override
  public void setUp() throws Exception {
    mDir = FileHelper.createTempDirectory();
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() throws Exception {
    FileHelper.deleteAll(mDir);
    mDir = null;
  }

  protected InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  public void testRegions() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">seq1\na\n>seq2\nta\n>seq3\ntag\n>seq4\ntagt\n>seq5\nacctt\n>seq6\natatat"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 30, PrereadType.UNKNOWN, false);
    sw.processSequences();
    final LongRange region = new LongRange(3, 5);
    try (SequencesReader dsr = createSequencesReader(mDir, region)) {
      final SequencesIterator it = dsr.iterator();
      assertEquals(2, dsr.numberSequences());

      // First sequence should be seq4
      assertTrue(it.nextSequence());
      assertEquals("seq4", it.currentName());
      assertEquals(4, it.currentLength());
      byte[] data = new byte[it.currentLength()];
      assertEquals(4, it.readCurrent(data));
      assertEquals("TAGT", DnaUtils.bytesToSequenceIncCG(data));

      assertEquals(0, it.currentSequenceId()); // Or should it be 3 or 0, as the first sequence in the reader

      // Seek to seq5
      it.seek(1);                              // Or should this be seek 4??
      assertEquals("seq5", it.currentName());
      assertEquals(5, it.currentLength());
      data = new byte[it.currentLength()];
      assertEquals(5, it.readCurrent(data));
      assertEquals("ACCTT", DnaUtils.bytesToSequenceIncCG(data));

      // Direct access to seq4
      final int seqId = 0;                            // Or should this be 3
      assertEquals("seq4", dsr.name(seqId));
      assertEquals(4, dsr.length(seqId));
      data = new byte[dsr.length(seqId)];
      assertEquals(4, dsr.read(seqId, data));
      assertEquals("TAGT", DnaUtils.bytesToSequenceIncCG(data));

    }
  }


  public void testLabel() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgt\n>bob\ntagt\n>hobos r us\naccc"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 30, PrereadType.UNKNOWN, false);
    sw.processSequences();
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(Exam.integrity(dsr));

      assertEquals(mDir, dsr.path());
      try {
        it.currentName();
        fail();
      } catch (final IllegalStateException e) {
        assertEquals("Last call to nextSequence() or seek() failed and left current information unavailable.", e.getMessage());
      }
      assertTrue(it.nextSequence());
      assertEquals("test", it.currentName());
      assertEquals("test", it.currentName());
      assertTrue(it.nextSequence());
      assertEquals("bob", it.currentName());
      assertTrue(it.nextSequence());
      assertEquals("hobos", it.currentName());
      it.seek(1);
      assertEquals("bob", it.currentName());
      it.seek(0);
      assertEquals("test", it.currentName());
      it.seek(2);
      assertEquals("hobos", it.currentName());
    }
  }

  public void testLengthBetween() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    //32
    //17
    //20
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(32, dsr.lengthBetween(0, 1));
      assertEquals(17, dsr.lengthBetween(1, 2));
      assertEquals(20, dsr.lengthBetween(2, 3));
      assertEquals(49, dsr.lengthBetween(0, 2));
      assertEquals(37, dsr.lengthBetween(1, 3));
      assertEquals(69, dsr.lengthBetween(0, 3));
      assertEquals(0, dsr.lengthBetween(3, 3));
    }
  }

  public void testLengthBetween2() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nactggtcatgca\n>bob the buuilder\ntagttcagcatc\n>hobos r us\naccccaccccac"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(12, dsr.lengthBetween(0, 1));
      assertEquals(12, dsr.lengthBetween(1, 2));
      assertEquals(12, dsr.lengthBetween(2, 3));
      assertEquals(24, dsr.lengthBetween(0, 2));
      assertEquals(24, dsr.lengthBetween(1, 3));
      assertEquals(36, dsr.lengthBetween(0, 3));
      assertEquals(0, dsr.lengthBetween(3, 3));
    }
  }

  public void testLengths() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    //32
    //17
    //20
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertTrue(Arrays.equals(new int[]{32, 17, 20}, dsr.sequenceLengths(0, 3)));
      assertTrue(Arrays.equals(new int[]{17, 20}, dsr.sequenceLengths(1, 3)));
      assertTrue(Arrays.equals(new int[]{20}, dsr.sequenceLengths(2, 3)));
      assertTrue(Arrays.equals(new int[]{32, 17}, dsr.sequenceLengths(0, 2)));
      assertTrue(Arrays.equals(new int[]{17}, dsr.sequenceLengths(1, 2)));
      assertTrue(Arrays.equals(new int[]{32}, dsr.sequenceLengths(0, 1)));
    }
  }

  public void testLengths2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    //32
    //17
    //20
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 2000000, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertTrue(Arrays.equals(new int[]{32, 17, 20}, dsr.sequenceLengths(0, 3)));
      assertTrue(Arrays.equals(new int[]{17, 20}, dsr.sequenceLengths(1, 3)));
      assertTrue(Arrays.equals(new int[]{20}, dsr.sequenceLengths(2, 3)));
      assertTrue(Arrays.equals(new int[]{32, 17}, dsr.sequenceLengths(0, 2)));
      assertTrue(Arrays.equals(new int[]{17}, dsr.sequenceLengths(1, 2)));
      assertTrue(Arrays.equals(new int[]{32}, dsr.sequenceLengths(0, 1)));
    }
  }

  public void testReadRoll() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();


    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(Exam.integrity(dsr));

      assertEquals(mDir, dsr.path());
      try {
        it.currentName();
        fail();
      } catch (final IllegalStateException e) {
        assertEquals("Last call to nextSequence() or seek() failed and left current information unavailable.", e.getMessage());
      }
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      assertEquals("test", it.currentName());
      assertEquals(32, it.currentLength());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 2, 3, 4, 3, 4, 3, 4, 3, 4, 2, 4, 4, 1, 3, 3, 3, 2, 4, 2, 1, 2, 4, 3, 3, 4, 2, 1, 4, 3, 2, 1});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      assertEquals("bob", it.currentName());
      assertEquals(17, it.currentLength());
      SequencesWriterTest.checkEquals(it, new byte[]{4, 1, 3, 4, 4, 2, 1, 3, 2, 1, 4, 2, 3, 1, 4, 2, 1});
      assertTrue(Exam.integrity(it));
      assertTrue(it.nextSequence());
      assertEquals(2, it.currentSequenceId());
      assertEquals("hobos", it.currentName());
      assertEquals(20, it.currentLength());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1});
      it.seek(1);
      assertEquals(1, it.currentSequenceId());
      assertEquals("bob", it.currentName());
      assertEquals(17, it.currentLength());
      SequencesWriterTest.checkEquals(it, new byte[]{4, 1, 3, 4, 4, 2, 1, 3, 2, 1, 4, 2, 3, 1, 4, 2, 1});
      it.reset();
      final byte[] seqs = new byte[32];
      final int amount = dsr.read(0, seqs);
      DNA[] expected = {DNA.A, DNA.C, DNA.G, DNA.T, DNA.G, DNA.T, DNA.G, DNA.T,
        DNA.G, DNA.T, DNA.C, DNA.T, DNA.T, DNA.A, DNA.G, DNA.G, DNA.G, DNA.C, DNA.T,
        DNA.C, DNA.A, DNA.C, DNA.T, DNA.G, DNA.G, DNA.T, DNA.C, DNA.A, DNA.T, DNA.G,
        DNA.C, DNA.A};
      assertEquals(expected.length, amount);
      for (int i = 0; i < seqs.length; i++) {
        assertEquals(expected[i].ordinal(), seqs[i]);
      }
      assertTrue(Exam.integrity(dsr));
      it.seek(0);
      expected = new DNA[]{DNA.A, DNA.C, DNA.G, DNA.T, DNA.G, DNA.T, DNA.G, DNA.T,
        DNA.G, DNA.T, DNA.C, DNA.T, DNA.T, DNA.A, DNA.G, DNA.G, DNA.G, DNA.C, DNA.T,
        DNA.C, DNA.A, DNA.C, DNA.T, DNA.G, DNA.G, DNA.T, DNA.C, DNA.A, DNA.T, DNA.G,
        DNA.C, DNA.A};
      byte[] bytes = new byte[expected.length];
      it.readCurrent(bytes);
      DNA[] dnaValues = DNA.values();
      for (int i = 0; i < bytes.length; i++) {
        assertEquals(expected[i], dnaValues[bytes[i]]);
      }

      it.seek(0);
      expected = new DNA[]{DNA.G, DNA.T, DNA.G, DNA.T,
        DNA.G, DNA.T, DNA.C, DNA.T, DNA.T, DNA.A, DNA.G, DNA.G, DNA.G, DNA.C, DNA.T,
        DNA.C, DNA.A, DNA.C, DNA.T, DNA.G, DNA.G, DNA.T, DNA.C, DNA.A, DNA.T, DNA.G,
        DNA.C, DNA.A};
      bytes = new byte[expected.length];
      it.readCurrent(bytes, 4, expected.length);
      dnaValues = DNA.values();
      for (int i = 0; i < bytes.length; i++) {
        assertEquals(expected[i], dnaValues[bytes[i]]);
      }

    }
  }

  public void testRead() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //testing the read (stolen from SequencesWriterTest)
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertEquals(mDir, dsr.path());
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 2, 4, 3, 4, 0, 3, 0});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 4, 3, 2});
      assertFalse(it.nextSequence());
    }
  }

  protected byte[] getExpectedQuality() {
    return new byte[] {'!' - '!', '<' - '!', '>' - '!', '<' - '!', '#' - '!', '#' - '!', '!' - '!', '<' - '!'};
  }

  public void testReadQuality() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@testQuality\n"
                        + "actgcatc\n"
                        + "+\n"
                        + "!<><##!<"));
    final FastqSequenceDataSource fq = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(fq, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      assertEquals("testQuality", it.currentName());
      final byte[] qual = new byte[it.currentLength()];
      assertEquals(qual.length, it.readCurrentQuality(qual));
      final byte[] exp = getExpectedQuality();

      assertTrue(Arrays.equals(exp, qual));

      it.reset();
      assertEquals(qual.length, dsr.readQuality(0, qual));
      assertTrue(Arrays.equals(exp, qual));

      it.seek(0);
      assertFalse(it.nextSequence());
      try {
        it.readCurrentQuality(qual);
        fail();
      } catch (final IllegalStateException e) {
        // correct
      }
    }
  }

  public void testReadQualityMultifile() throws IOException {
    //check rolling works
    final byte[] expQualBytes = new byte[60];
    final StringBuilder quals = new StringBuilder();
    for (int i = 0; i < expQualBytes.length; i++) {
      expQualBytes[i] = (byte) (60 - i);
      quals.append((char) (60 - i + '!'));
    }
    final byte[] expDnaBytes = new byte[60];
    final StringBuilder dnas = new StringBuilder();
    for (int i = 0; i < expDnaBytes.length; i++) {
      expDnaBytes[i] = (byte) DNA.A.ordinal();
      dnas.append(DNA.A.toString());
    }
    final String seq = "@TotallyUniqueName\n" + dnas.toString() + "\n+\n" + quals.toString();
    final ArrayList<InputStream> streams = new ArrayList<>();
    streams.add(createStream(seq));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(streams, FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      final byte[] result = new byte[60];
      assertEquals(60, it.currentLength());
      assertEquals("TotallyUniqueName", it.currentName());
      assertEquals(60, it.readCurrentQuality(result));
      assertTrue(Arrays.equals(expQualBytes, result));
    }
  }

  protected DNA[] getExpected0() {
    return new DNA[] {DNA.A, DNA.C, DNA.G, DNA.T, DNA.G, DNA.T, DNA.G, DNA.T,
                                   DNA.G, DNA.T, DNA.C, DNA.T, DNA.T, DNA.A, DNA.G, DNA.G, DNA.G, DNA.C, DNA.T,
                                   DNA.C, DNA.A, DNA.C, DNA.T, DNA.G, DNA.G, DNA.T, DNA.C, DNA.A, DNA.T, DNA.G,
                                   DNA.C, DNA.A};
  }

  protected DNA[] getExpected1() {
    return new DNA[] {DNA.T, DNA.A, DNA.G, DNA.T, DNA.T, DNA.C, DNA.A, DNA.G, DNA.C,
                                   DNA.A, DNA.T, DNA.C, DNA.G, DNA.A, DNA.T, DNA.C, DNA.A};
  }

  protected DNA[] getExpected2() {
    return new DNA[] {DNA.A, DNA.C, DNA.C, DNA.C, DNA.C, DNA.A, DNA.C, DNA.C, DNA.C,
                      DNA.C, DNA.A, DNA.C, DNA.A, DNA.A, DNA.A, DNA.C, DNA.C, DNA.C, DNA.A, DNA.A};
  }

  public void testRawRead() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();


    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final byte[] bigenough = new byte[32];
      final byte[] notbigenough = new byte[19];
      final DNA[] expected0 = getExpected0();
      final DNA[] expected1 = getExpected1();
      final DNA[] expected2 = getExpected2();
      dsr.read(0, bigenough);
      for (int i = 0; i < expected0.length; i++) {
        assertEquals(expected0[i].ordinal(), bigenough[i]);
      }
      dsr.read(1, bigenough);
      for (int i = 0; i < expected1.length; i++) {
        assertEquals(expected1[i].ordinal(), bigenough[i]);
      }
      dsr.read(1, bigenough, 8, expected1.length - 8);
      for (int i = 8; i < expected1.length; i++) {
        assertEquals(String.valueOf(i), expected1[i].ordinal(), bigenough[i - 8]);
      }
      dsr.read(2, bigenough);
      for (int i = 0; i < expected2.length; i++) {
        assertEquals(expected2[i].ordinal(), bigenough[i]);
      }
      dsr.read(1, notbigenough);
      for (int i = 0; i < expected1.length; i++) {
        assertEquals(expected1[i].ordinal(), notbigenough[i]);
      }
      try {
        dsr.read(2, notbigenough);
        fail("Should have complained about array size");
      } catch (final IllegalArgumentException e) {
        assertEquals("Array too small got: " + notbigenough.length + " required: 20", e.getMessage());
      }
      try {
        dsr.read(0, notbigenough);
        fail("Should have complained about array size");
      } catch (final IllegalArgumentException e) {
        assertEquals("Array too small got: " + notbigenough.length + " required: 32", e.getMessage());
      }
    }
  }

  public void testRead2() throws Exception {
    final ArrayList<InputStream> al1 = new ArrayList<>();
    al1.add(createStream(">x1\nACGTACGNNNNN\n"));
    final FastaSequenceDataSource ds1 = new FastaSequenceDataSource(al1,
                                                              new DNAFastaSymbolTable());
    final SequencesWriter sw1 = new SequencesWriter(ds1, mDir, 20, PrereadType.UNKNOWN, false);
    sw1.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertEquals(dsr.totalLength(), 12);

      it.nextSequence();
      assertEquals(dsr.totalLength(), it.currentLength());
      assertEquals(dsr.totalLength(), dsr.maxLength());
      assertEquals(dsr.totalLength(), dsr.minLength());
      final byte[] bytes = new byte[(int) dsr.maxLength()];
      it.readCurrent(bytes);
      assertTrue(!it.nextSequence());
      try {
        it.readCurrent(bytes);
        fail("Should throw exception.");
      } catch (final IllegalStateException e) {
        //
      }
    }
  }

  public void testEquals() throws Exception {
    final File dir1 = FileUtils.createTempDir("dir1", "11");
    final File dir2 = FileUtils.createTempDir("dir2", "22");
    //create data source
    final ArrayList<InputStream> al1 = new ArrayList<>();
    al1.add(createStream(">test\naH\n  tg\ntXGj\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds1 = new FastaSequenceDataSource(al1,
                                                              new ProteinFastaSymbolTable());
    final SequencesWriter sw1 = new SequencesWriter(ds1, dir1, 20, PrereadType.UNKNOWN, false);
    sw1.processSequences();


    final ArrayList<InputStream> al2 = new ArrayList<>();
    al2.add(createStream(">test\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds2 = new FastaSequenceDataSource(al2,
                                                              new DNAFastaSymbolTable());
    final SequencesWriter sw2 = new SequencesWriter(ds2, dir2, 20, PrereadType.UNKNOWN, false);
    sw2.processSequences();

    try (SequencesReader dsr1 = createSequencesReader(dir1)) {
      try (SequencesReader dsr2 = createSequencesReader(dir2); SequencesReader dsr22 = createSequencesReader(dir2)) {
        assertTrue(!dsr1.equals(null));
        assertTrue(!dsr1.equals(dsr2));
        assertTrue(!dsr2.equals(dsr1));
        assertTrue(dsr2.equals(dsr22));
        assertEquals(dsr2.hashCode(), dsr22.hashCode());
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir1));
      assertTrue(FileHelper.deleteAll(dir2));
    }
  }

  public void testResidueCountsWithoutDusterOnDNA() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //testing the read (stolen from SequencesWriterTest)
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();

      assertEquals(12, dsr.totalLength());
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 2, 4, 3, 4, 0, 3, 0});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 4, 3, 2});
      assertEquals(2, dsr.residueCounts()[DNA.A.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.C.ordinal()]);
      assertEquals(3, dsr.residueCounts()[DNA.G.ordinal()]);
      assertEquals(3, dsr.residueCounts()[DNA.T.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.N.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }

  public void testResidueCountsWithDusterOnDNA() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new DNAFastaSymbolTable());
    ds.setDusting(true);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //testing the read (stolen from SequencesWriterTest)
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{0, 0, 0, 0, 0, 0, 3, 0});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{1, 4, 3, 2});
      assertEquals(1, dsr.residueCounts()[DNA.A.ordinal()]);
      assertEquals(1, dsr.residueCounts()[DNA.C.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.G.ordinal()]);
      assertEquals(1, dsr.residueCounts()[DNA.T.ordinal()]);
      assertEquals(7, dsr.residueCounts()[DNA.N.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }

  public void testResidueCountsWithoutDusterOnProtein() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\naH\n  tg\ntXGj\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new ProteinFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //testing the read (stolen from SequencesWriterTest)
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      SequencesWriterTest.checkEquals(it, new byte[]{2, 10, 18, 9, 18, 0, 9, 0});
      assertTrue(it.nextSequence());
      SequencesWriterTest.checkEquals(it, new byte[]{2, 18, 9, 6});
      assertEquals(2, dsr.residueCounts()[Protein.A.ordinal()]);
      assertEquals(1, dsr.residueCounts()[Protein.C.ordinal()]);
      assertEquals(1, dsr.residueCounts()[Protein.H.ordinal()]);
      assertEquals(3, dsr.residueCounts()[Protein.T.ordinal()]);
      assertEquals(3, dsr.residueCounts()[Protein.G.ordinal()]);
      assertEquals(2, dsr.residueCounts()[Protein.X.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }

  public void testResidueCountsWithDusterOnProtein() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\naH\n  tg\ntXGj\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                                                             new ProteinFastaSymbolTable());
    ds.setDusting(true);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = createSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      SequencesWriterTest.checkEquals(it, new byte[]{0, 10, 0, 0, 0, 0, 9, 0});
      assertTrue(it.nextSequence());
      SequencesWriterTest.checkEquals(it, new byte[]{2, 18, 9, 6});
      assertEquals(1, dsr.residueCounts()[Protein.A.ordinal()]);
      assertEquals(1, dsr.residueCounts()[Protein.C.ordinal()]);
      assertEquals(1, dsr.residueCounts()[Protein.H.ordinal()]);
      assertEquals(1, dsr.residueCounts()[Protein.T.ordinal()]);
      assertEquals(2, dsr.residueCounts()[Protein.G.ordinal()]);
      assertEquals(6, dsr.residueCounts()[Protein.X.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }
}

