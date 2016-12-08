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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.SequenceType;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorEvent;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.diagnostic.WarningEvent;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class FastqSequenceDataSourceTest extends TestCase {

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
    al.add(createStream("@test\nac\n  tg\ntnGh\n\n\t   \n+test\n!~\n  xy\nXVW@\n\n\t   \n@test2\r\nATGC+\r\n!#$%"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    final byte[] b = ds.sequenceData();
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
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.T.ordinal(), b[1]);
    assertEquals(DNA.G.ordinal(), b[2]);
    assertEquals(DNA.C.ordinal(), b[3]);
    assertTrue(!ds.nextSequence());
  }

  public void testOneSeq2() throws IOException {
    //testing read() method
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(
      "@12345" + StringUtils.LS
      + "acgtgt" + StringUtils.LS
      + "+12345" + StringUtils.LS
      + "IIIIII" + StringUtils.LS));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("12345", ds.name());
    final byte[] b = ds.sequenceData();
    assertEquals(6, ds.currentLength());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.C.ordinal(), b[1]);
    assertEquals(DNA.G.ordinal(), b[2]);
    assertEquals(DNA.T.ordinal(), b[3]);
    assertEquals(DNA.G.ordinal(), b[4]);
    assertEquals(DNA.T.ordinal(), b[5]);

    final byte[] q = ds.qualityData();
    assertEquals('I' - 33, q[0]);
    assertEquals('I' - 33, q[1]);
    assertEquals('I' - 33, q[2]);
    assertEquals('I' - 33, q[3]);
    assertEquals('I' - 33, q[4]);
    assertEquals('I' - 33, q[5]);

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
      al.add(createStream("@x\n" + "actgn\n" + "+x\n" + "ACTGN\n"));
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      assertTrue(ds.nextSequence());
      assertEquals("x", ds.name());
      final byte[] b = ds.sequenceData();
      assertEquals(5, ds.currentLength());
      assertEquals(DNA.A.ordinal(), b[0]);
      assertEquals(DNA.C.ordinal(), b[1]);
      assertEquals(DNA.T.ordinal(), b[2]);
      assertEquals(DNA.G.ordinal(), b[3]);
      assertEquals(DNA.N.ordinal(), b[4]);

      final byte[] q = ds.qualityData();
      assertEquals('A' - 33, q[0]);
      assertEquals('C' - 33, q[1]);
      assertEquals('T' - 33, q[2]);
      assertEquals('G' - 33, q[3]);
      assertEquals('N' - 33, q[4]);

      assertTrue(!ds.nextSequence());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testBadSeq() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\nacgt\n+test\n!~!~!\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    try {
      ds.nextSequence();
      fail();
    } catch (final SlimException e) {
      //yay
    }
  }

  public void testBadSeq2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("garbage@test\nacgt\n+test\n!~!~\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    try {
      ds.nextSequence();
      fail();
    } catch (final SlimException e) {
      //yay
    }
  }

  public void testBadSeq3() throws IOException {
    final boolean[] handled = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event instanceof ErrorEvent) {
          assertTrue(event.getMessage().contains("testsequencename3"));
          handled[0] = true;
        }
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream("@testsequencename3\nacgt\n"));
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
      try {
        assertTrue(ds.nextSequence());
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertTrue(handled[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }
  public void testBadSeq3b() throws IOException {
    final boolean[] handled = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event instanceof ErrorEvent) {
          assertTrue(event.getMessage().contains("testsequencename4"));
          handled[0] = true;
        }
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream("@testsequencename4\nacgt\n"));
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
      try {
        ds.nextSequence();
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertTrue(handled[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testBadSeq4() throws IOException {
    final boolean[] handled = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event instanceof ErrorEvent) {
          assertTrue(event.getMessage().contains("testsequencename"));
          handled[0] = true;
        }
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream("@testsequencename\nacgt\n+\n!~!\n"));
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
      try {
        assertTrue(ds.nextSequence());
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertTrue(handled[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testBadSeq4b() throws IOException {
    final boolean[] handled = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event instanceof ErrorEvent) {
          assertTrue(event.getMessage().contains("testsequencename2"));
          handled[0] = true;
        }
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final ArrayList<InputStream> al = new ArrayList<>();
      al.add(createStream("@testsequencename2\nacgt\n+\n!~!\n"));
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
      try {
        ds.nextSequence();
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertTrue(handled[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testBadSeq5() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\nacgt\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    try {
      assertTrue(ds.nextSequence());
      fail();
    } catch (final SlimException e) {
      //expected
    }
  }

  public void testBadSeq6() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\nacgt\n+\n!!!\u007f"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    try {
      assertTrue(ds.nextSequence());
      fail();
    } catch (final SlimException e) {
      //expected
    }
  }

  public void testOneSeqPlusQual() throws IOException {
    //testing read() method
    ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\nac\n  tg\ntnGh\n\n\t   \n+test\n!~\n  xy\nXVW@\n\n\t   \n@test2\r\nATGC+\r\n!#$%"));
    FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    byte[] b = ds.sequenceData();
    byte[] q = ds.qualityData();
    assertEquals(8, ds.currentLength());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(0, q[0]);
    assertEquals(93, q[1]);
    assertEquals(DNA.C.ordinal(), b[1]);
    assertEquals(DNA.T.ordinal(), b[2]);
    assertEquals((byte) ('x' - '!'), q[2]);
    assertEquals((byte) ('y' - '!'), q[3]);
    assertEquals(DNA.G.ordinal(), b[3]);
    assertEquals(DNA.T.ordinal(), b[4]);
    assertEquals(DNA.N.ordinal(), b[5]);
    assertEquals(DNA.G.ordinal(), b[6]);
    assertEquals((byte) ('X' - '!'), q[4]);
    assertEquals((byte) ('V' - '!'), q[5]);
    assertEquals((byte) ('W' - '!'), q[6]);
    assertEquals((byte) ('@' - '!'), q[7]);
    assertEquals(0, q[8]);
    assertEquals(DNA.N.ordinal(), b[7]);
    assertEquals(0, b[8]);

    assertTrue(ds.nextSequence());
    assertEquals("test2", ds.name());
    b = ds.sequenceData();
    q = ds.qualityData();
    assertEquals(4, ds.currentLength());
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(DNA.T.ordinal(), b[1]);
    assertEquals(DNA.G.ordinal(), b[2]);
    assertEquals(DNA.C.ordinal(), b[3]);
    assertEquals((byte) ('!' - '!'), q[0]);
    assertEquals((byte) ('#' - '!'), q[1]);
    assertEquals((byte) ('$' - '!'), q[2]);
    assertEquals((byte) ('%' - '!'), q[3]);
    assertTrue(!ds.nextSequence());
    assertEquals(1, ds.getWarningCount());

    //testing solexa quality values
    al = new ArrayList<>();
    al.add(createStream("@test\nac\n  tg\ntnGh\n\n\t   \n+test\n;~\n  xy\nXVW@\n\n\t   \n@test2\r\nATGC+\r\n!#$%"));
    ds = new FastqSequenceDataSource(al, FastQScoreType.SOLEXA);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    b = ds.sequenceData();
    q = ds.qualityData();
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(1, q[0]);
    assertEquals(DNA.C.ordinal(), b[1]);
    assertEquals(62, q[1]);
    assertTrue(ds.hasQualityData());
    assertEquals(1, ds.getWarningCount());

    //testing solexa 1.3 quality values
    al = new ArrayList<>();
    al.add(createStream("@test\nac\n  tg\ntnGh\n\n\t   \n+test\n@~\n  xy\nXVW@\n\n\t   \n@test2\r\nATGC+\r\n!#$%"));
    ds = new FastqSequenceDataSource(al, FastQScoreType.SOLEXA1_3);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    b = ds.sequenceData();
    q = ds.qualityData();
    assertEquals(DNA.A.ordinal(), b[0]);
    assertEquals(0, q[0]);
    assertEquals(DNA.C.ordinal(), b[1]);
    assertEquals(62, q[1]);
    assertTrue(ds.hasQualityData());
    assertEquals(1, ds.getWarningCount());
    al = new ArrayList<>();
    al.add(createStream("@test\nac\n  tg\ntnGh\n\n\t   \n+test\n!~\n  xy\nXVW@\n\n\t   \n@test2\r\nATGC+\r\n!#$%"));
    ds = new FastqSequenceDataSource(al, FastQScoreType.SOLEXA1_3);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());

    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(DiagnosticEvent<?> event) {
        if (event.getType().equals(ErrorType.INVALID_QUALITY)) {
          assertEquals("Error: Quality data was invalid. You may need to try a different format type.", event.getMessage());
        } else if (event.getType().equals(WarningType.BAD_TIDE)) {
          assertEquals("Unexpected symbol \"h\" in sequence \"test\" replaced with \"N\".", event.getMessage());
        } else {
          fail();
        }
      }
      @Override
      public void close() { }
    };
    Diagnostic.addListener(dl);
    try {
      ds.nextSequence();
      fail();
    } catch (final NoTalkbackSlimException ntse) {
      //expected
    } finally {
      Diagnostic.removeListener(dl);
    }

  }

  private static final String KINDA_LONG_SEQ;
  static {
    final StringBuilder sbseq = new StringBuilder();
    while (sbseq.length() < 15000) {
      sbseq.append("tttttttt");
    }
    final StringBuilder sbq = new StringBuilder();
    while (sbq.length() < 15000) {
      sbq.append("[[[[[[[[");
    }
    KINDA_LONG_SEQ = "@kindalong\n" + sbseq.toString() + "\n+\n" + sbq.toString();
  }

  public void testKindaLong() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(KINDA_LONG_SEQ));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertTrue(ds.nextSequence());
    final byte[] b = ds.sequenceData();
    final byte[] q = ds.qualityData();
    assertEquals(15000, ds.currentLength());
    for (int i = 0; i < 15000; ++i) {
      assertEquals(DNA.T.ordinal(), b[i]);
      assertEquals('[' - '!', q[i]);
    }
  }

  public void testDustingSequence() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\nac\n  Tg\ntnGh\n\n\t   \n+test\n!~\n  xy\nXUVW\n\n\t   \n@test2\r\nATGCn+\r\n!#$%!"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    ds.setDusting(true);
    FastaSequenceDataSourceTest.checkResult(ds);
  }

  public void testConsecutiveLabelsWithoutActualSequenceData() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\n+test\n@test2\n+\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals(0, ds.currentLength()); //
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence()); //
    assertEquals(0, ds.currentLength());
    assertEquals("test2", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testConsecutiveLabelsWithoutActualSequenceData2() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@test\n\r\n\r\r\r\n\n\r"
        + "+test\n\r\n\r\r\r\n\n\r"
        + "@test2\n"
        + "+test2\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("test2", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testBogusChars() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    final String bad = "\"!@#$%^&*()_1234567890-={}[]vs\\|?/<,\"\"";
    final StringWriter wr = new StringWriter();
    for (int i = 0; i < bad.length(); ++i) {
      wr.append((char) (126 - i));
    }
    final String bed = wr.toString();
    al.add(createStream(""
        + "@test\n" + bad + "\n"
        + "+test\n" + bed + "\n"
    ));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    final byte[] b = ds.sequenceData();
    assertEquals(bad.length(), ds.currentLength());
    for (int i = 0; i < bad.length(); ++i) {
      assertEquals("" + i, DNA.N.ordinal(), b[i]);
    }
    assertTrue(!ds.nextSequence());
  }

  private FastqSequenceDataSource getDataSource(final String sequence) {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(sequence));
    return new FastqSequenceDataSource(al, FastQScoreType.PHRED);
  }

  private FastqSequenceDataSource getDataSource(final String[] sequences) {
    final ArrayList<InputStream> al = new ArrayList<>();
    for (final String s : sequences) {
      al.add(createStream(s));
    }
    return new FastqSequenceDataSource(al, FastQScoreType.PHRED);
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
      final FastqSequenceDataSource ds = getDataSource(""
          + "@test\n" + "ac\n  tg\ntnGh\n\n\t   \n"
          + "+test\n" + "AC\n  TG\nTNGH\n\n\t   \n@"
      );
      final DNA[] expected = {DNA.A, DNA.C, DNA.T, DNA.G, DNA.T, DNA.N, DNA.G, DNA.N};
      final DNA[][] allexpected = {expected};
      final String[] labels = {"test", ""};
      int i = 0;
      for (final DNA[] exp : allexpected) {
        assertTrue(ds.nextSequence());
        assertEquals(labels[i++], ds.name());
        final byte[] b = ds.sequenceData();
        assertEquals(exp.length, ds.currentLength());
        int j = 0;
        for (final DNA e : exp) {
          assertEquals(e.ordinal(), b[j]);
          ++j;
        }
      }
      assertTrue(!ds.nextSequence());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testSkipping() throws Exception {
    FastqSequenceDataSource ds = getDataSource(""
        + "@test\n" + "ac\n  tg\ntnGh\n\n\t   \n>"
        + "+test\n" + "ac\n  tg\ntnGh\n\n\t   \n>");
    ds.nextSequence();
    assertEquals("test", ds.name());
    assertTrue(!ds.nextSequence());
    assertEquals(SequenceType.DNA , ds.type());
    ds = getDataSource(new String[] {""
        + "@test\n" + "acgt\n"
        + "+test\n" + "acgt\n"
        + "@hobo\n" + "tgca\n"
        + "+hobo\n" + "tgca\n"
        + "@", "", ""
        + "@more\n" + "+more\n", ""
        + "\n", ""
        + "@again\n" + "tttt"
        + "+again\n" + "tttt"
    });
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("hobo", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("more", ds.name());
    assertTrue(ds.nextSequence());
    assertEquals("again", ds.name());
    assertTrue(!ds.nextSequence());
  }
  public void testMultiSource() throws IOException {
    final FastqSequenceDataSource ds = getDataSource(new String[] {
        "@test\n" + "acgt\n"
        + "+test\n" + "1234\n"
        + "@hobo\n" + "tgca"
        + "+hobo\n" + "5678",
        "@more\n" + "catg\n"
        + "+more\n" + "9012\n@",
        "@again\ntttt\n"
        + "+again\n7777\n"
    });
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
        assertEquals(exp.ordinal(), b[j]);
        ++j;
      }
    }
    assertTrue(!ds.nextSequence());
  }
  public InputStream getSuperLongStream(final int megabytes) {
    final int meg = megabytes << 20;
    final byte[] sequenceOfAs = new byte[2 * meg]; // 1 MB
    final byte[] header = "\n@test\n".getBytes();
    final byte[] qual = "\n+test\n".getBytes();
    assert qual.length == header.length;
    Arrays.fill(sequenceOfAs, (byte) 'A');
    System.arraycopy(header, 0, sequenceOfAs, 0, header.length);
    System.arraycopy(qual, 0, sequenceOfAs, meg, qual.length);
    return new ByteArrayInputStream(sequenceOfAs);
  }

  public void testSuperLongSeq() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(getSuperLongStream(20));

    //    al.add(getSuperLongStream(2049)); // i.e. test >2GB
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    assertNull("Haven't called nextSequence, should be null", ds.sequenceData());
    assertTrue(ds.nextSequence());
    assertEquals("test", ds.name());
    assertTrue(!ds.nextSequence());
  }

  public void testMultipleFiles() throws IOException {
    final ArrayList<File> list = new ArrayList<>();
    try {
      final String s1 = "@first\nacgt\n+first\nacgt";
      list.add(FastaSequenceDataSourceTest.getFile("tt1", s1));
      final String s2 = "@second\nacgt\n+second\nacgt\n";
      list.add(FastaSequenceDataSourceTest.getFile("tt2", s2));
      final String s3 = "@third\nacgt\n+third\nacgt\n";
      list.add(FastaSequenceDataSourceTest.getFile("tt3", s3));
      multipleFileTest(list);
    } finally {
      for (final File f : list) {
        FileHelper.deleteAll(f);
      }
    }
  }

  private void multipleFileTest(ArrayList<File> list) throws IOException {
    try (FastqSequenceDataSource ds = new FastqSequenceDataSource(list, FastQScoreType.PHRED, true, null)) {
      assertTrue(ds.nextSequence());
      assertEquals("first", ds.name());
      byte[] b = ds.sequenceData();
      assertEquals(4, ds.currentLength());
      assertEquals(DNA.A.ordinal(), b[0]);
      assertEquals(DNA.C.ordinal(), b[1]);
      assertEquals(DNA.G.ordinal(), b[2]);
      assertEquals(DNA.T.ordinal(), b[3]);
      assertTrue(ds.nextSequence());
      assertEquals("second", ds.name());
      b = ds.sequenceData();
      assertEquals(4, ds.currentLength());
      assertEquals(DNA.A.ordinal(), b[0]);
      assertEquals(DNA.C.ordinal(), b[1]);
      assertEquals(DNA.G.ordinal(), b[2]);
      assertEquals(DNA.T.ordinal(), b[3]);
      assertTrue(ds.nextSequence());
      assertEquals("third", ds.name());
      b = ds.sequenceData();
      assertEquals(4, ds.currentLength());
      assertEquals(DNA.A.ordinal(), b[0]);
      assertEquals(DNA.C.ordinal(), b[1]);
      assertEquals(DNA.G.ordinal(), b[2]);
      assertEquals(DNA.T.ordinal(), b[3]);
      assertFalse(ds.nextSequence());
    }
  }

  public void testMultipleFilesGzipped() throws IOException {
    final ArrayList<File> list = new ArrayList<>();
    try {
      final String s1 = "@first\nacgt\n+first\nacgt";
      final File f1 = FastaSequenceDataSourceTest.getGZFile("tt1", s1);
      list.add(f1);
      final String s2 = "@second\nacgt\n+second\nacgt\n";
      final File f2 = FastaSequenceDataSourceTest.getGZFile("tt2", s2);
      list.add(f2);
      final String s3 = "@third\nacgt\n+third\nacgt\n";
      final File f3 = FastaSequenceDataSourceTest.getGZFile("tt3", s3);
      list.add(f3);
      multipleFileTest(list);
    } finally {
      for (final File f : list) {
        FileHelper.deleteAll(f);
      }
    }
  }

  public void testWarning() throws IOException {
    final boolean[] warning = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        assertTrue(event instanceof WarningEvent);
        warning[0] = true;
      }
      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      FastqSequenceDataSource ds = getDataSource(new String[] {
        "@test\n" + "acgt\n"
                + "+test\n" + "acgt\n"
      });
      assertTrue(ds.nextSequence());
      byte[] q = ds.qualityData();
      assertEquals(4, ds.currentLength());
      assertTrue(q[0] >= 0);
      assertTrue(q[1] >= 0);
      assertTrue(q[2] >= 0);
      assertTrue(q[3] >= 0);
      assertTrue(warning[0]);

      warning[0] = false;
      ds = getDataSource("@test3\nacgt\n+\nZZZZ");
      assertTrue(ds.nextSequence());
      q = ds.qualityData();
      assertEquals(4, ds.currentLength());
      assertTrue(q[0] >= 0);
      assertTrue(q[1] >= 0);
      assertTrue(q[2] >= 0);
      assertTrue(q[3] >= 0);
      assertTrue(!warning[0]);
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testEnum() {
    TestUtils.testEnum(FastQScoreType.class, "[PHRED, SOLEXA, SOLEXA1_3]");
  }

  private static class MyListener implements DiagnosticListener {

    private ErrorEvent mEvent;

    private final MemoryPrintStream mWarnings = new MemoryPrintStream();

    @Override
    public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
      if (event instanceof ErrorEvent) {
        mEvent = (ErrorEvent) event;
      } else if (event instanceof WarningEvent) {
        mWarnings.printStream().println(event.getMessage());
        mWarnings.printStream().flush();
      }
    }
    @Override
    public void close() {
    }
  }

  public void testTypeErrors() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\t\t\t\nac\n  tg\ntnGh\n\n\t   \n>test2 "));
    final MyListener dl = new MyListener();
    Diagnostic.addListener(dl);
    try {
      FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      try {
        ds.nextSequence();
        fail();
      } catch (final SlimException e) {
        e.printErrorNoLog();
      }
      assertNotNull(dl.mEvent);
      assertEquals(ErrorType.FASTA, dl.mEvent.getType());
      assertEquals("Error: At least one input file looks like FASTA format rather than FASTQ. Try processing with the fasta format option.", dl.mEvent.getMessage());

      dl.mEvent = null;
      al.clear();
      al.add(createStream("flying spaghetti monster"));
      ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
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

  public void testMismatchLabelWarnings() throws IOException {
    mismatchTest(9);
    mismatchTest(10);
    mismatchTest(11);
  }

  private void mismatchTest(int numberOfMismatches) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    final StringBuilder fqStr = new StringBuilder();
    final StringBuilder expectedWarnings = new StringBuilder();
    for (int i = 0; i < numberOfMismatches; ++i) {
      fqStr.append("@seq").append(i).append(" extraBlah\n");
      fqStr.append("AGGGCCCCTTTTAGT\n");
      fqStr.append("+seq").append(i).append("\n");
      fqStr.append("222222222222222\n");

      if (i < 10) {
        expectedWarnings.append("Sequence label \"seq").append(i)
        .append(" extraBlah\" not the same as quality label \"seq")
        .append(i).append("\".").append(StringUtils.LS);
      }
      if (i == 9) {
        expectedWarnings.append("Subsequent warnings of this type will not be shown.").append(StringUtils.LS);
      }
    }
    al.add(createStream(fqStr.toString()));
    final MyListener dl = new MyListener();
    Diagnostic.addListener(dl);
    try {
      final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
      int numSeq = 0;
      while (ds.nextSequence()) {
        ++numSeq;
      }
      assertEquals(numberOfMismatches, numSeq);
      assertEquals(expectedWarnings.toString(), dl.mWarnings.toString());
    } finally {
      Diagnostic.removeListener(dl);
    }
  }
}
