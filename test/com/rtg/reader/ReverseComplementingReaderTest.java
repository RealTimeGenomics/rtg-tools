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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 */
public class ReverseComplementingReaderTest extends DefaultSequencesReaderTest {

  public static Test suite() {
    return new TestSuite(ReverseComplementingReaderTest.class);
  }

  @Override
  protected SequencesReader createSequencesReader(final File dir) throws IOException {
    return new ReverseComplementingReader(SequencesReaderFactory.createDefaultSequencesReader(dir));
  }

  @Override
  protected byte[] getExpectedQuality() {
    final byte[] r = super.getExpectedQuality();
    ReverseComplementingReader.reverse(r, 0, r.length);
    return r;
  }

  // Can't do protein stuff with this reader

  @Override
  public void testEquals() { }

  @Override
  public void testResidueCountsWithoutDusterOnProtein() { }

  @Override
  public void testResidueCountsWithDusterOnProtein() { }

  @Override
  public void testReadRoll() { }

  @Override
  public void testRawRead() { }

  @Override
  public void testReadQualityMultifile() { }

  @Override
  public void testRead() { }

  @Override
  public void testResidueCountsWithDusterOnDNA() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    ds.setDusting(true);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertEquals(mDir, dsr.path());
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{0, 2, 0, 0, 0, 0, 0, 0});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{3, 2, 1, 4});
      assertEquals(1, dsr.residueCounts()[DNA.T.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.C.ordinal()]);
      assertEquals(1, dsr.residueCounts()[DNA.G.ordinal()]);
      assertEquals(1, dsr.residueCounts()[DNA.A.ordinal()]);
      assertEquals(7, dsr.residueCounts()[DNA.N.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }

  @Override
  public void testResidueCountsWithoutDusterOnDNA() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,  new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //testing the read (stolen from SequencesWriterTest)
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertEquals(mDir, dsr.path());
      assertEquals(12, dsr.totalLength());
      assertTrue(it.nextSequence());
      assertEquals(0, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{0, 2, 0, 1, 2, 1, 3, 4});
      assertTrue(it.nextSequence());
      assertEquals(1, it.currentSequenceId());
      SequencesWriterTest.checkEquals(it, new byte[]{3, 2, 1, 4});
      assertEquals(2, dsr.residueCounts()[DNA.N.ordinal()]);
      assertEquals(3, dsr.residueCounts()[DNA.A.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.T.ordinal()]);
      assertEquals(3, dsr.residueCounts()[DNA.C.ordinal()]);
      assertEquals(2, dsr.residueCounts()[DNA.G.ordinal()]);
      assertFalse(it.nextSequence());
    }
  }

  public void testRC() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test1\nacngta\n"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20000, PrereadType.UNKNOWN, false);
    sw.processSequences();
    try (SequencesReader dsr = createSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      final byte[] x = new byte[27];
      assertEquals(6, it.readCurrent(x));
      assertEquals(DNA.T.ordinal(), x[0]);
      assertEquals(DNA.A.ordinal(), x[1]);
      assertEquals(DNA.C.ordinal(), x[2]);
      assertEquals(DNA.N.ordinal(), x[3]);
      assertEquals(DNA.G.ordinal(), x[4]);
      assertEquals(DNA.T.ordinal(), x[5]);
      assertFalse(it.nextSequence());
    }
  }
}

