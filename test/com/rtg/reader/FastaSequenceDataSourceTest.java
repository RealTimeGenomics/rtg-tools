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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.Protein;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorEvent;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.diagnostic.WarningEvent;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Tests corresponding class
 */
public class FastaSequenceDataSourceTest extends TestCase {

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    Diagnostic.setLogStream();
  }

  private InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  public void testOneSeq() throws IOException {
    //testing read() method
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    byte[] b = ds.sequenceData();
    assertEquals(8, ds.currentLength());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.C.ordinal(), b[1]);
    assertEquals(DNA.T.ordinal(), b[2]);
    assertEquals(DNA.G.ordinal(), b[3]);
    assertEquals(DNA.T.ordinal(), b[4]);
    assertEquals(DNA.N.ordinal(), b[5]);
    assertEquals(DNA.G.ordinal(), b[6]);
    assertEquals(DNA.N.ordinal(), b[7]);
    assertEquals(0, b[8]);
    assertTrue(ds.nextSequence());
    assertEquals("test2", ds.name());
    b = ds.sequenceData();
    assertEquals(4, ds.currentLength());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.T.ordinal(), b[1]);
    assertEquals(DNA.G.ordinal(), b[2]);
    assertEquals(DNA.C.ordinal(), b[3]);
    assertTrue(!ds.nextSequence());
  }

  public void testSequences() throws IOException {
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        fail();
      }
      @Override
      public void close() { }
    };
    Diagnostic.addListener(dl);
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream(">x\n" + "actgn\n>"));
      final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      assertTrue(ds.nextSequence());
      assertEquals("x", ds.name());
      final byte[] b = ds.sequenceData();
      assertEquals(5, ds.currentLength());
      assertEquals(DNA.A.ordinal(), b[0]);
      assertEquals(DNA.C.ordinal(), b[1]);
      assertEquals(DNA.T.ordinal(), b[2]);
      assertEquals(DNA.G.ordinal(), b[3]);
      assertEquals(DNA.N.ordinal(), b[4]);

      assertNull(ds.qualityData());
      assertTrue(!ds.nextSequence());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testWarning() throws IOException {
    final InputStream is = createStream("      ");
    final boolean[] done = new boolean[1];
    final ArrayList<InputStream> l = new ArrayList<>();
    l.add(is);
    final FastaSequenceDataSource fsds = new FastaSequenceDataSource(l, new DNAFastaSymbolTable());
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event instanceof WarningEvent) {
          final WarningEvent we = (WarningEvent) event;
          assertEquals("The supplied file \"<Not known>\" is not a FASTA file or has no sequences.", we.getMessage());
          done[0] = true;
        }
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      assertFalse(fsds.nextSequence());
      assertTrue(done[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }


  public void testDustingSequence() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  Tg\ntnGh\n\n\t   \n>test2\r\nATGCc"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    ds.setDusting(true);
    checkResult(ds);
  }

  static void checkResult(final SequenceDataSource ds) throws IOException {
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    byte[] b = ds.sequenceData();
    assertEquals(8, ds.currentLength());
    assertEquals(DNA.N.ordinal(), b[0]);
    assertEquals(DNA.N.ordinal(), b[1]);
    assertEquals(DNA.T.ordinal(), b[2]);
    assertEquals(DNA.N.ordinal(), b[3]);
    assertEquals(DNA.N.ordinal(), b[4]);
    assertEquals(DNA.N.ordinal(), b[5]);
    assertEquals(DNA.G.ordinal(), b[6]);
    assertEquals(DNA.N.ordinal(), b[7]);
    assertEquals(0, b[8]);
    assertTrue(ds.nextSequence());
    b = ds.sequenceData();
    assertEquals(5, ds.currentLength());
    assertEquals("test2", ds.name());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.T.ordinal(), b[1]);
    assertEquals(DNA.G.ordinal(), b[2]);
    assertEquals(DNA.C.ordinal(), b[3]);
    assertEquals(DNA.N.ordinal(), b[4]);
    assertFalse(ds.nextSequence());
  }

  public void testConsecutiveLabelsWithoutActualSequenceData() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\n>test2\n"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("test2", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testConsecutiveLabelsWithoutActualSequenceData2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\t\n\r\n\r\r\r\n\n\r>>test2\n"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test\t", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals(">test2", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testBogusChars() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    final String bad = "`!@#$%^&*()_+1234567890-={}[]vs\\|?/<,.'\"";
    al.add(createStream(">test\n" + bad));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    final byte[] b = ds.sequenceData();
    assertEquals(bad.length(), ds.currentLength());
    for (int i = 0; i < bad.length(); ++i) {
      assertEquals("" + i, DNA.N.ordinal(), b[i]);
    }
    assertFalse(ds.nextSequence());
  }

  public void testOneSeqFunnyLabel() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\t dog meow\nac\n  tg\ntnGh\n\n\t   \n>test2 pox\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test\t dog meow", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("test2 pox", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testOneSeqFunnyLabel2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\r\nac\n  tg\ntnGh\n\n\t   \n>test2 \nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("test2 ", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testOneSeqFunnyLabel3() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\t\t\t\nac\n  tg\ntnGh\n\n\t   \n>test2 "));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test\t\t\t", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testProtein() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\naH\n  tg\ntXGj\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
    final Protein[] expected = {Protein.A, Protein.H, Protein.T, Protein.G, Protein.T, Protein.X, Protein.G, Protein.X};
    final Protein[] expected2 = {Protein.A, Protein.T, Protein.G, Protein.C};
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());

    byte[] b = ds.sequenceData();
    assertEquals(expected.length, ds.currentLength());
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i].ordinal(), b[i]);
    }
    assertTrue(ds.nextSequence());
    b = ds.sequenceData();
    assertEquals(expected2.length, ds.currentLength());
    for (int i = 0; i < expected2.length; ++i) {
      assertEquals(expected2[i].ordinal(), b[i]);
    }
    assertFalse(ds.nextSequence());
  }

  public void testProtein2() throws Exception {
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        fail("Should produce no warnings");
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final StringBuilder seq = new StringBuilder(">test\n");
      for (final Protein p : Protein.values()) {
        if (p.equals(Protein.STOP)) {
          continue;
        }
        seq.append(p.name());
      }
      seq.append("BZ");
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream(seq.toString()));
      final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
      assertTrue(ds.nextSequence());
      assertEquals("test", ds.name());

      final byte[] b = ds.sequenceData();
      assertEquals(Protein.values().length + 1, ds.currentLength());
      int i = 0;
      int j = 0;
      for (; i < Protein.values().length; ++i) {
        if (Protein.values()[i].equals(Protein.STOP)) {
          continue;
        }
        assertEquals(Protein.values()[i].ordinal(), b[j]);
        ++j;
      }
      assertEquals(Protein.X.ordinal(), b[i + 1]);
      assertEquals(Protein.X.ordinal(), b[i + 2]);
      assertEquals(SequenceType.PROTEIN, ds.type());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  private FastaSequenceDataSource getDataSource(final String sequence) {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(sequence));
    return new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
  }

  private FastaSequenceDataSource getDataSource(final String[] sequences) {
    final ArrayList<InputStream> al = new ArrayList<>();
    for (final String s : sequences) {
      al.add(createStream(s));
    }
    return new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
  }

  public void testBadFile() throws Exception {
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        assertEquals(WarningType.BAD_TIDE, event.getType());
        assertEquals("Unexpected symbol \"h\" in sequence \"test\" replaced with \"N\".", event.getMessage());
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final FastaSequenceDataSource ds = getDataSource(">test\nac\n  tg\ntnGh\n\n\t   \n>");
      final DNA[] expected = {DNA.A, DNA.C, DNA.T, DNA.G, DNA.T, DNA.N, DNA.G, DNA.N};
      final DNA[][] allexpected = {expected};
      final String[] labels = {"test", ""};
      int i = 0;
      for (final DNA[] exp : allexpected) {
        assertTrue(ds.nextSequence());
        final byte[] b = ds.sequenceData();
        assertEquals(exp.length, ds.currentLength());
        assertEquals(labels[i++], ds.name());
        int j = 0;
        for (final DNA e : exp) {
          assertEquals(e.ordinal(), b[j++]);
        }
      }
      assertFalse(ds.nextSequence());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testSkipping() throws Exception {
    FastaSequenceDataSource ds = getDataSource(">test\nac\n  tg\ntnGh\n\n\t   \n");
    ds.nextSequence();
    assertEquals("test", ds.name());
    assertFalse(ds.nextSequence());
    assertEquals(SequenceType.DNA , ds.type());
    ds = getDataSource(new String[] {">test\nacgt\n>hobo\ntgca\n", "     ", ">more\ncatg", ">again\ntttt"});
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("hobo", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("more", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("again", ds.name());
    assertFalse(ds.nextSequence());
  }

  public void testMultiSource() throws IOException {
    final FastaSequenceDataSource ds = getDataSource(new String[] {">test\nacgt\n>hobo\ntgca", ">more\ncatg\n", ">again\ntttt"});
    final DNA[][] allexpected = {{DNA.A, DNA.C, DNA.G, DNA.T},
        {DNA.T, DNA.G, DNA.C, DNA.A},
        {DNA.C, DNA.A, DNA.T, DNA.G},
        {DNA.T, DNA.T, DNA.T, DNA.T}};
    final String[] labels = {"test", "hobo", "more", "again"};
    for (int i = 0; i < labels.length; ++i) {
      assertTrue(ds.nextSequence());
      assertEquals(labels[i], ds.name());
      final byte[] b = ds.sequenceData();
      assertEquals(allexpected[i].length, ds.currentLength());
      int j = 0;
      for (final DNA exp : allexpected[i]) {
        assertEquals(exp.ordinal(), b[j++]);
      }
    }
    assertFalse(ds.nextSequence());
  }

  public InputStream getSuperLongStream(final int megabytes) {
    final byte[] sequenceOfAs = new byte[megabytes << 20]; // 1 MB
    final byte[] header = ">test\n".getBytes();
    Arrays.fill(sequenceOfAs, (byte) 'A');
    System.arraycopy(header, 0, sequenceOfAs, 0, header.length);
    return new ByteArrayInputStream(sequenceOfAs);
  }

  public void testSuperLongSeq() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(getSuperLongStream(20));
    //    al.add(getSuperLongStream(2049)); // i.e. test >2GB
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testMultipleFiles() throws IOException {
    final ArrayList<File> list = new ArrayList<>();
    try {
      final String s1 = ">first\nacgt";
      list.add(getFile("tt1", s1));
      final String s2 = ">second\nacgt";
      list.add(getFile("tt2", s2));
      final String s3 = ">third\nacgt";
      list.add(getFile("tt3", s3));
      try (FastaSequenceDataSource fsds = new FastaSequenceDataSource(list, new DNAFastaSymbolTable(), true, null)) {
        assertTrue(fsds.nextSequence());
        assertEquals("first", fsds.name());
        byte[] b = fsds.sequenceData();
        assertEquals(4, fsds.currentLength());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);

        assertTrue(fsds.nextSequence());
        b = fsds.sequenceData();
        assertEquals(4, fsds.currentLength());
        assertEquals("second", fsds.name());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);

        assertTrue(fsds.nextSequence());
        b = fsds.sequenceData();
        assertEquals(4, fsds.currentLength());
        assertEquals("third", fsds.name());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);

        assertFalse(fsds.nextSequence());
      }
    } finally {
      for (final File f : list) {
        FileHelper.deleteAll(f);
      }
    }
  }

  public void testMultipleFilesGzipped() throws IOException {
    final ArrayList<File> list = new ArrayList<>();
    try {
      final String s1 = ">first\nacgt";
      final File f1 = getGZFile("tt1", s1);
      list.add(f1);
      final String s2 = ">second\nacgt";
      final File f2 = getGZFile("tt2", s2);
      list.add(f2);
      final String s3 = ">third\nacgt";
      final File f3 = getGZFile("tt3", s3);
      list.add(f3);
      try (FastaSequenceDataSource ds = new FastaSequenceDataSource(list, new DNAFastaSymbolTable(), true, null)) {
        assertTrue(ds.nextSequence());
        byte[] b = ds.sequenceData();
        assertEquals(4, ds.currentLength());
        assertEquals("first", ds.name());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);
        assertTrue(ds.nextSequence());
        b = ds.sequenceData();
        assertEquals(4, ds.currentLength());
        assertEquals("second", ds.name());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);
        assertTrue(ds.nextSequence());
        b = ds.sequenceData();
        assertEquals(4, ds.currentLength());
        assertEquals("third", ds.name());
        assertEquals(DNA.A.ordinal(), b[0]);
        assertEquals(DNA.C.ordinal(), b[1]);
        assertEquals(DNA.G.ordinal(), b[2]);
        assertEquals(DNA.T.ordinal(), b[3]);
        assertFalse(ds.nextSequence());
      }
    } finally {
      for (final File f : list) {
        FileHelper.deleteAll(f);
      }
    }
  }

  static File getFile(final String name, final String seq) throws IOException {
    final File f = File.createTempFile(name, null);
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(f));
      bw.write(seq);
      bw.close();
      return f;
    } finally {
      if (bw != null) {
        bw.close();
      }
    }
  }

  static File getGZFile(final String name, final String seq) throws IOException {
    final File f = File.createTempFile(name, ".gz");
    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f))))) {
      bw.write(seq);
    }
    return f;
  }

  private static class MyListener implements DiagnosticListener {

    private ErrorEvent mEvent;

    @Override
    public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
      if (event instanceof ErrorEvent) {
        mEvent = (ErrorEvent) event;
      }
    }
    @Override
    public void close() {
    }
  }

  public void testTypeErrors() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\t\t\t\nac\n  tg\ntnGh\n\n\t   \n>test2 "));
    final MyListener dl = new MyListener();
    Diagnostic.addListener(dl);
    try {
      FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      try {
        ds.nextSequence();
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertNotNull(dl.mEvent);
      assertEquals(ErrorType.FASTQ, dl.mEvent.getType());
      assertEquals("Error: At least one input file looks like FASTQ format rather than FASTA. Try processing with the fastq format option.", dl.mEvent.getMessage());

      dl.mEvent = null;
      al.clear();
      al.add(createStream("flying spaghetti monster"));
      ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
      try {
        ds.nextSequence();
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertNotNull(dl.mEvent);
      assertEquals(ErrorType.BAD_FASTA_LABEL, dl.mEvent.getType());
      assertEquals("Error: Unrecognized symbols appeared before label symbol. Last sequence read was: \"<none>\"", dl.mEvent.getMessage());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }
}
