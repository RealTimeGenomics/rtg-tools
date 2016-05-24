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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.Protein;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.mode.SequenceType;
import com.rtg.reader.FastqSequenceDataSource.FastQScoreType;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.DiagnosticEvent;
import com.rtg.util.diagnostic.DiagnosticListener;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.diagnostic.WarningEvent;
import com.rtg.util.diagnostic.WarningType;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.test.FileHelper;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 */
public class SequencesWriterTest extends TestCase {

  private File mDir = null;

  @Override
  public void setUp() throws Exception {
    mDir = FileHelper.createTempDirectory();
    Diagnostic.setLogStream();

  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mDir);
    mDir = null;
  }

  public void testLabels() throws Exception {
    final FastaSequenceDataSource ds = getDataSource(">\u007fharrred\nactg");
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    //assertEquals(20, sw.getSizeLimit());
    try {
      sw.processSequences();
      fail();
    } catch (NoTalkbackSlimException e) {
      assertEquals(ErrorType.BAD_CHARS_NAME, e.getErrorType());
      assertTrue(e.getMessage().contains("\u007fharrred"));
    }
  }


  private static boolean sHandled = false;
  private static void setHandled() {
    sHandled = true;
  }

  public void testEmptySequence() throws Exception {
    final FastaSequenceDataSource ds = getDataSource("");
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    final DiagnosticListener dl = new DiagnosticListener() {
      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (sHandled) {
          Assert.fail();
        }
        Assert.assertTrue(event instanceof WarningEvent);
        Assert.assertEquals(WarningType.NOT_FASTA_FILE, event.getType());
        setHandled();
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      sw.processSequences();
    } finally {
      Diagnostic.removeListener(dl);
    }
    assertTrue(sHandled);
  }

  public void testNoSequence() throws Exception {
    final DiagnosticListener dl = new DiagnosticListener() {
        int mCount = -1;
        @Override
        public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
          if (event.getType() != WarningType.INFO_WARNING) {
            switch (++mCount) {
              case 0:
                Assert.assertEquals(WarningType.NO_SEQUENCE, event.getType());
                Assert.assertEquals("The sequence \"rrred\" has no data.", event.getMessage());
                break;
              case 1:
              case 2:
                Assert.assertEquals(WarningType.NO_NAME, event.getType());
                Assert.assertEquals("Sequence with no name was assigned name \"Unnamed_sequence_" + (mCount - 1) + "\".", event.getMessage());
                break;
              case 3:
                Assert.assertEquals(WarningType.BAD_CHAR_WARNINGS, event.getType());
                break;
              default:
                fail();
                break;
            }
          }
        }

        @Override
        public void close() {
        }
      };
    Diagnostic.addListener(dl);
    try {
      final FastaSequenceDataSource ds = getDataSource(">rrred\n>\nactg\n>\ntgac");
      final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
      sw.processSequences();
      final SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir);
        assertEquals(3, dsr.numberSequences()); //blank sequence still makes a sequence - so that paired end data still matches up
    } finally {
      Diagnostic.removeListener(dl);
    }
  }

  public void testErrors() throws Exception {
    try {
      new SequencesWriter(null, null, 20, PrereadType.UNKNOWN, false);
      fail("Should throw NullPointerException");
    } catch (final NullPointerException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Provided data source was null."));
    }

    final String seq = ">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa";
    FastaSequenceDataSource ds = getDataSource(seq);
    try {
      try {
        new SequencesWriter(ds, null, 20, PrereadType.UNKNOWN, false);
        fail("Should throw NullPointerException");
      } catch (final NullPointerException e) {
        // ok
      }
      final File f = FileHelper.createTempFile();
      try {
        new SequencesWriter(ds, f, 20, PrereadType.UNKNOWN, false);
        fail("Should throw SlimException");
      } catch (final SlimException e) {
        // ok
      } finally {
        assertTrue(f.delete());
      }
      try {
        new SequencesWriter(ds, mDir, 19, PrereadType.UNKNOWN, false).processSequences();
        fail("Should throw IllegalArgumentException");
      } catch (final IllegalArgumentException e) {
        assertEquals("Size limit of: " + 19 + " is not within bounds of: " + SdfWriter.MIN_SIZE_LIMIT + " and " + SdfWriter.MAX_SIZE_LIMIT,
                     e.getMessage());
      }
      ds = getDataSource(seq);
      long limit = 1024L * 1024 * 1024 * 1024 + 1;
      try {
        new SequencesWriter(ds, mDir, limit, PrereadType.UNKNOWN, false).processSequences();
        fail("Should throw IllegalArgumentException");
      } catch (final IllegalArgumentException e) {
        assertEquals("Size limit of: " + limit + " is not within bounds of: " + SdfWriter.MIN_SIZE_LIMIT + " and " + SdfWriter.MAX_SIZE_LIMIT,
                     e.getMessage());
      }
      ds = getDataSource(seq);
      limit = (long) Integer.MAX_VALUE + 1;
      try {
        final SequencesWriter sw = new SequencesWriter(ds, mDir, limit, PrereadType.UNKNOWN, false);
        sw.processSequences();
        fail("Should throw IllegalArgumentException");
      } catch (final IllegalArgumentException e) {
        assertEquals("Currently only support int pointers",
                     e.getMessage());
      }
    } finally {
      ds.close();
    }
  }

  private InputStream createStream(final String data) {
    return new ByteArrayInputStream(data.getBytes());
  }

  public void testPointerRoll() throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">1\na\n>2\nc\n>3\ng\n>4\nt\n>5\na\n>6\nc\n>7\ng\n>8\nt\n"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                        new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    assertEquals(0, sw.getTotalLength());
    assertEquals(Long.MAX_VALUE, sw.getMinLength());
    assertEquals(Long.MIN_VALUE, sw.getMaxLength());
    assertEquals(0, sw.getNumberOfSequences());
    //assertEquals(0, sw.getChunkNumber());       //doesn't use chunks (progress related)
    assertEquals(20, sw.getSizeLimit());
    sw.processSequences();
    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      //dsr.globalIntegrity();
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();
      //assertTrue(dsr.nextSequence());
      final String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8"};
      final byte[][] expected = {{1}, {2}, {3}, {4}, {1}, {2}, {3}, {4}};
      for (int i = 0; i < expected.length; i++) {
        assertTrue(it.nextSequence());
        assertEquals(labels[i], it.currentName());
        checkEquals(it, expected[i]);
      }
      assertTrue(!it.nextSequence());
    }
    final File[] files = mDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(final File dir, final String name) {
        if (name.startsWith(SdfFileUtils.SEQUENCE_POINTER_FILENAME)) {
          return true;
        }
        if (name.startsWith(SdfFileUtils.LABEL_POINTER_FILENAME)) {
          return true;
        }
        return false;
      }
    });
    assertNotNull(files);
    Arrays.sort(files);
    assertEquals(4, files.length);
    assertEquals(20, files[0].length());
    assertEquals(12, files[1].length());
    assertEquals(20, files[2].length());
    assertEquals(21, files[3].length());
  }

  public void testHistogram() throws IOException {
    final String seq = ""
            + ">1" + StringUtils.LS + "a" + StringUtils.LS
            + ">2" + StringUtils.LS + "c" + StringUtils.LS
            + ">3" + StringUtils.LS + "g" + StringUtils.LS
            + ">4" + StringUtils.LS + "t" + StringUtils.LS
            + ">5" + StringUtils.LS + "a" + StringUtils.LS
            + ">6" + StringUtils.LS + "c" + StringUtils.LS
            + ">7" + StringUtils.LS + "g" + StringUtils.LS
            + ">8" + StringUtils.LS + "t" + StringUtils.LS;
    final SequencesWriter sw = new SequencesWriter(getDataSource(seq), mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      final long[] histogram = dsr.histogram();
      assertEquals(8, histogram[0]);
      for (int i = 1; i < histogram.length; i++) {
        assertEquals(0, histogram[i]);
      }
    }
  }
  public void testHistogram2() throws IOException {
    final String seq = ""
            + ">a=1" + StringUtils.LS + "acgt" + StringUtils.LS
            + ">b=1" + StringUtils.LS + "ncgt" + StringUtils.LS
            + ">c=1" + StringUtils.LS + "antc" + StringUtils.LS
            + ">d=1" + StringUtils.LS + "acnt" + StringUtils.LS
            + ">e=1" + StringUtils.LS + "acgn" + StringUtils.LS
            + ">f=1" + StringUtils.LS + "nngt" + StringUtils.LS
            + ">g=1" + StringUtils.LS + "annt" + StringUtils.LS
            + ">h=1" + StringUtils.LS + "acnn" + StringUtils.LS
            + ">i=1" + StringUtils.LS + "nnnt" + StringUtils.LS
            + ">j=1" + StringUtils.LS + "annn" + StringUtils.LS
            + ">k=1" + StringUtils.LS + "nnnn" + StringUtils.LS;
    final SequencesWriter sw = new SequencesWriter(getDataSource(seq), mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      final long[] histogram = dsr.histogram();
      assertEquals(1, histogram[0]);
      assertEquals(4, histogram[1]);
      assertEquals(3, histogram[2]);
      assertEquals(2, histogram[3]);
      assertEquals(1, histogram[4]);
      assertEquals(10, dsr.nBlockCount());
      assertEquals(4, dsr.longestNBlock());
    }
  }

  public void testHistogram3() throws Exception {
    final String seq = ""
      + "@12345" + StringUtils.LS
      + "acgtgt" + StringUtils.LS
      + "+12345" + StringUtils.LS
      + "IIIIII" + StringUtils.LS;
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(seq));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      assertEquals("12345", it.currentName());
      final byte[] q = new byte[6];
      it.readCurrentQuality(q);
      assertTrue(Arrays.equals(new byte[]{'I' - 33, 'I' - 33, 'I' - 33, 'I' - 33, 'I' - 33, 'I' - 33}, q));

      for (int i = 0; i < 6; i++) {
        assertEquals(1.0E-4, dsr.positionQualityAverage()[i]);
      }
      assertEquals(0.0, dsr.positionQualityAverage()[7]);
    }
  }

  public static void checkEquals(final SequencesIterator r, final byte[] expected) throws IOException {
    final byte[] t = new byte[expected.length];
    assertEquals(expected.length, r.readCurrent(t));
    assertTrue(Arrays.equals(expected, t));
  }

  public static void checkQualityEquals(final SequencesIterator r, final byte[] expected) throws IOException {
    final byte[] t = new byte[expected.length];
    assertEquals(expected.length, r.readCurrentQuality(t));
    assertTrue(Arrays.equals(expected, t));
  }


  public void testRoll() throws Exception {
    //create data source
    //mDir = new File("/home2/david/cgltest");
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                        new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
    assertEquals(3, sw.getNumberOfSequences());
    assertEquals(0, sw.getNumberOfExcludedSequences());
    assertEquals(17, sw.getMinLength());
    assertEquals(32, sw.getMaxLength());
    assertEquals(69, sw.getTotalLength());
    //expected files:
    //main index
    //seqdata0, size 20
    //seqdata1, size 20
    //seqdata2, size 20
    //seqdata3, size 9
    //seqpointer0, size 4
    //seqpointer1, size 4
    //seqpointer2, size 4
    //seqpointer3, size 0
    //namedata0, size 5
    //namedata1, size 17
    //namedata2, size 11
    //namepointer0, size 4
    //namepointer1, size 4,
    //namepointer2, size 4


    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertEquals(mDir, dsr.path());
      final SequencesIterator it = dsr.iterator();
      assertTrue(it.nextSequence());
      assertEquals("1234567890123456789", it.currentName());
      assertEquals(32, it.currentLength());
      checkEquals(it, new byte[]{1, 2, 3, 4, 3, 4, 3, 4, 3, 4, 2, 4, 4, 1, 3, 3, 3, 2, 4, 2, 1, 2, 4, 3, 3, 4, 2, 1, 4, 3, 2, 1});
      assertTrue(it.nextSequence());
      assertEquals("bob", it.currentName());
      assertEquals(17, it.currentLength());
      checkEquals(it, new byte[]{4, 1, 3, 4, 4, 2, 1, 3, 2, 1, 4, 2, 3, 1, 4, 2, 1});
      assertTrue(it.nextSequence());
      assertEquals("hobos", it.currentName());
      assertEquals(20, it.currentLength());
      checkEquals(it, new byte[]{1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1});
    }
  }

  public void testRollWithQuality() throws Exception {
    checkRollWithQuality(false);
    checkRollWithQuality(true);
  }
  public void checkRollWithQuality(boolean compress) throws Exception {
    //create data source
    //mDir = new File("/home2/david/cgltest");
    createQualityData(compress);
    //expected files:
    //main index
    //seqdata0, size 20
    //seqdata1, size 20
    //seqdata2, size 20
    //seqdata3, size 9
    //qualdata0, size 20
    //qualdata1, size 20
    //qualdata2, size 20
    //qualdata3, size 9
    //seqpointer0, size 4
    //seqpointer1, size 4
    //seqpointer2, size 4
    //seqpointer3, size 0
    //namedata0, size 0
    //namedata1, size 20
    //namedata2, size 10
    //namepointer0, size 4
    //namepointer1, size 4,
    //namepointer2, size 4
    if (!compress) {
      assertEquals(20, SdfFileUtils.sequenceDataFile(mDir, 0).length());
      assertEquals(20, SdfFileUtils.sequenceDataFile(mDir, 1).length());
      assertEquals(20, SdfFileUtils.sequenceDataFile(mDir, 2).length());
      assertEquals(9, SdfFileUtils.sequenceDataFile(mDir, 3).length());
      assertEquals(20, SdfFileUtils.qualityDataFile(mDir, 0).length());
      assertEquals(20, SdfFileUtils.qualityDataFile(mDir, 1).length());
      assertEquals(20, SdfFileUtils.qualityDataFile(mDir, 2).length());
      assertEquals(9, SdfFileUtils.qualityDataFile(mDir, 3).length());
      assertEquals(6, SdfFileUtils.sequencePointerFile(mDir, 0).length());
      assertEquals(6, SdfFileUtils.sequencePointerFile(mDir, 1).length());
      assertEquals(6, SdfFileUtils.sequencePointerFile(mDir, 2).length());
      assertEquals(2, SdfFileUtils.sequencePointerFile(mDir, 3).length());
      assertEquals(0, SdfFileUtils.labelDataFile(mDir, 0).length()); //tries to fit in current but cannot
      assertEquals(20, SdfFileUtils.labelDataFile(mDir, 1).length()); //cannot fit in brand new so truncates at max file size
      assertEquals(12, SdfFileUtils.labelDataFile(mDir, 2).length()); //2nd and 3rd written normally
      assertEquals(0, SdfFileUtils.labelPointerFile(mDir, 0).length());
      assertEquals(4, SdfFileUtils.labelPointerFile(mDir, 1).length());
      assertEquals(8, SdfFileUtils.labelPointerFile(mDir, 2).length());
    }

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      final SequencesIterator it = dsr.iterator();
      assertTrue(dsr.hasQualityData());
      assertEquals(mDir, dsr.path());
      assertTrue(it.nextSequence());
      assertEquals("1234567890123456789", it.currentName());
      assertEquals(32, it.currentLength());
      checkEquals(it, new byte[]{1, 2, 3, 4, 3, 4, 3, 4, 3, 4, 2, 4, 4, 1, 3, 3, 3, 2, 4, 2, 1, 2, 4, 3, 3, 4, 2, 1, 4, 3, 2, 1});
      byte[] qualExp = new byte[32];
      for (int i = 0; i < 32; i++) {
        final char c = "!!ASDFFSAFASHSKFSDIUR<<SA><>S<<<".charAt(i);
        qualExp[i] = (byte) (c - '!');
        if (qualExp[i] > 63) {
          qualExp[i] = 63;
        }
      }
      checkQualityEquals(it, qualExp);
      assertTrue(it.nextSequence());
      assertEquals("bob", it.currentName());
      assertEquals(17, it.currentLength());

      qualExp = new byte[17];
      for (int i = 0; i < 17; i++) {
        final char c = "!ADSFG<<{()))[[[]".charAt(i);
        qualExp[i] = (byte) (c - '!');
        if (qualExp[i] > 63) {
          qualExp[i] = 63;
        }
      }
      checkEquals(it, new byte[]{4, 1, 3, 4, 4, 2, 1, 3, 2, 1, 4, 2, 3, 1, 4, 2, 1});
      checkQualityEquals(it, qualExp);
      assertTrue(it.nextSequence());
      assertEquals("hobos-r", it.currentName());
      assertEquals(20, it.currentLength());
      qualExp = new byte[20];
      for (int i = 0; i < 20; i++) {
        final char c = "ADSFAD[[<<<><<[[;;FS".charAt(i);
        qualExp[i] = (byte) (c - '!');
        if (qualExp[i] > 63) {
          qualExp[i] = 63;
        }
      }
      checkEquals(it, new byte[]{1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1});
      checkQualityEquals(it, qualExp);

    }
  }

  private void createQualityData(boolean compress) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n"
                      + "+123456789012345678901\n!!ASDFFSAFASHSKFSDIUR<<SA><>S<<<\n"
                      + "@bob the buuilder\ntagttcagcatcgatca\n"
                      + "+bob the buuilder\n!ADSFG<<{()))[[[]\n"
                      + "@hobos-r us\naccccaccccacaaacccaa\n"
                      + "+hobos-r us\nADSFAD[[<<<><<[[;;FS\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.SOLEXA, compress);
    sw.processSequences();
  }

  /*private void createQualDataCG() {
    InputStream cc = new ArrayList<>();
    cc.add(createStream("GTTTC" + StringUtils.LS);
    FastqSequenceDataSource ds = new FastqSequenceDataSource(al, false);
    // set mDir
    SequencesWriter sw = new SequencesWriter(ds, mDir, 20, InputFormat.SOLEXA);
    sw.processSequences();
  }*/

 public void testWrite() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\nac\n  tg\ntnGh\n\n\t   \n>xxbaddog\r\nFFF\n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final ArrayList<String> sup = new ArrayList<>();
    sup.add("baddog");
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 1024, sup, PrereadType.UNKNOWN, false, null);
    assertEquals(0, sw.getNumberOfExcludedSequences());
    sw.processSequences();

    //check files
   try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(mDir, SdfFileUtils.INDEX_FILENAME)))) {
     assertEquals(13, dis.readLong());
     assertEquals(1024, dis.readLong());
     assertEquals(SequenceType.DNA.ordinal(), dis.readInt());
     assertEquals(1, sw.getNumberOfExcludedSequences());
   }

   try (FileInputStream fis = new FileInputStream(new File(mDir, SdfFileUtils.SEQUENCE_DATA_FILENAME + "0"))) {
     final DNA[] expected = {DNA.A, DNA.C, DNA.T, DNA.G, DNA.T, DNA.N, DNA.G, DNA.N, DNA.A, DNA.T, DNA.G, DNA.C};
     int i = 0;
     int d;
     while ((d = fis.read()) != -1 && i < expected.length) {
       assertEquals("Index: " + i, expected[i++].ordinal(), d);
     }
     assertEquals(expected.length, i);
   }
   try (DataInputStream dis2 = new DataInputStream(new FileInputStream(new File(mDir, SdfFileUtils.SEQUENCE_POINTER_FILENAME + "0")))) {
     dis2.read();
     assertEquals(0, dis2.readInt());
     dis2.read();
     assertEquals(8, dis2.readInt());
     try {
       dis2.readInt();
       fail("Should be eof");
     } catch (final EOFException e) {
       //
     }
   }

   //This tests it is read correctly, of course this relies on the fact that the reader works :/
   try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
     final SequencesIterator it = dsr.iterator();
     assertEquals(12, dsr.totalLength());
     assertEquals(8, dsr.maxLength());
     assertEquals(4, dsr.minLength());
     assertFalse(dsr.hasQualityData());
     assertFalse(dsr.hasQualityData());
     assertTrue(it.nextSequence());
     assertTrue(it.nextSequence());
     assertFalse(it.nextSequence());
     assertFalse(dsr.hasQualityData());
   }
  }

  public void testWriteProtein() throws Exception {
    //create data source
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">test\naH\n  tg\ntXGj\n\n\t   \n>test2\r\nATGC"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al,
                        new ProteinFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 1024, PrereadType.UNKNOWN, false);
    sw.processSequences();

    //check files
    try (DataInputStream dis = new DataInputStream(new FileInputStream(new File(mDir, SdfFileUtils.INDEX_FILENAME)))) {
      assertEquals(13, dis.readLong());
      assertEquals(1024, dis.readLong());
      assertEquals(SequenceType.PROTEIN.ordinal(), dis.readInt());
    }

    try (FileInputStream fis = new FileInputStream(new File(mDir, SdfFileUtils.SEQUENCE_DATA_FILENAME + "0"))) {
      final Protein[] expected = {Protein.A, Protein.H, Protein.T, Protein.G, Protein.T, Protein.X, Protein.G, Protein.X, Protein.A, Protein.T, Protein.G, Protein.C};
      int i = 0;
      int d;
      while ((d = fis.read()) != -1 && i < expected.length) {
        assertEquals("Index: " + i, expected[i++].ordinal(), d);
      }
      assertEquals(expected.length, i);
    }
    try (DataInputStream dis2 = new DataInputStream(new FileInputStream(new File(mDir, SdfFileUtils.SEQUENCE_POINTER_FILENAME + "0")))) {
      dis2.read();
      assertEquals(0, dis2.readInt());
      dis2.read();
      assertEquals(8, dis2.readInt());
      try {
        dis2.readInt();
        fail("Should be eof");
      } catch (final EOFException e) {
        //
      }
    }

  }

  public void testProteinWarning() throws Exception {
    final FastaSequenceDataSource fsds = getProteinDataSource(">totally_a_protein\nacgttcagtantatcgaattgcagn");
    final boolean[] gotWarning = new boolean[1];
    final DiagnosticListener dl = new DiagnosticListener() {

      @Override
      public void handleDiagnosticEvent(final DiagnosticEvent<?> event) {
        if (event.getMessage().length() > 0) {
          assertTrue(event.getType() == WarningType.POSSIBLY_NOT_PROTEIN);
          gotWarning[0] = true;
        }
      }

      @Override
      public void close() {
      }
    };
    Diagnostic.addListener(dl);
    try {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    } finally {
      Diagnostic.removeListener(dl);
    }
    assertTrue(gotWarning[0]);
  }

  private FastaSequenceDataSource getDataSource(final String sequence) {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(sequence));
    return new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
  }

  private FastaSequenceDataSource getProteinDataSource(final String sequence) {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(sequence));
    return new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
  }

  public void testBadSeqNames() {
    assertTrue(AbstractSdfWriter.SequenceNameHandler.fixSequenceName("a=b", new StringBuilder()));
    assertTrue(AbstractSdfWriter.SequenceNameHandler.fixSequenceName("a=", new StringBuilder()));
    assertFalse(AbstractSdfWriter.SequenceNameHandler.fixSequenceName("=b", new StringBuilder()));
    assertFalse(AbstractSdfWriter.SequenceNameHandler.fixSequenceName("*b", new StringBuilder()));
    assertTrue(AbstractSdfWriter.SequenceNameHandler.fixSequenceName("a*b", new StringBuilder()));
  }

  public void testBadCharsWarning() throws Exception {

    final FastaSequenceDataSource fsds = getDataSource(">=1\nacgt\n>=1\nacgt\n>=1\nacgt\n>=1\nacgt\n>=1\nacgt\n>=1\nacgt\n>=1\nacgt\n");
    final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
    try {
      sw.processSequences();
      fail();
    } catch (NoTalkbackSlimException e) {
      assertEquals(ErrorType.BAD_CHARS_NAME, e.getErrorType());
      assertTrue(e.getMessage().contains("=1"));
    }
  }

  public void testType() throws Exception {
    createQualityData(false);

    try (SequencesReader dsr = SequencesReaderFactory.createDefaultSequencesReader(mDir)) {
      assertEquals(PrereadArm.UNKNOWN, dsr.getArm());
      assertEquals(PrereadType.SOLEXA, dsr.getPrereadType());
      assertTrue(dsr.getSdfId().available());
    }
  }

  public void testZeroLengthRead() throws Exception {
    try (FastaSequenceDataSource fsds = getDataSource(">null\n\n")) {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    }

    SequencesReaderFactory.createDefaultSequencesReader(mDir);
    try (SequencesReader sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(mDir, true, true, LongRange.NONE)) {
      assertEquals(1, sr.numberSequences());
      assertEquals(0, sr.length(0));
    }
  }
  public void testTwoZeroLengthReads() throws Exception {
    try (FastaSequenceDataSource fsds = getDataSource(">null1\n\n>null\n\n")) {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    }

    SequencesReaderFactory.createDefaultSequencesReader(mDir);
    try (SequencesReader sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(mDir, true, true, LongRange.NONE)) {
      assertEquals(2, sr.numberSequences());
      assertEquals(0, sr.length(0));
      assertEquals(0, sr.length(1));
    }
  }

  private void checkZeroLengthReadWithOtherReadsFirst(final LongRange range) throws Exception {
    try (FastaSequenceDataSource fsds = getDataSource(">read\nACGT\n>null\n\n")) {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    }

    SequencesReader sr = SequencesReaderFactory.createDefaultSequencesReader(mDir);
    try {
      try {
        assertEquals(2, sr.numberSequences());
        final SequencesIterator it = sr.iterator();
        it.nextSequence();
        assertEquals(4, it.currentLength());
        it.nextSequence();
        assertEquals(0, it.currentLength());
      } finally {
        sr.close();
      }
      sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(mDir, true, true, range);
      final SequencesIterator it = sr.iterator();
      assertEquals(2, sr.numberSequences());
      it.nextSequence();
      assertEquals(4, it.currentLength());
      it.nextSequence();
      assertEquals(0, it.currentLength());
    } finally {
      sr.close();
    }
  }

  public void testZeroLengthReadWithOtherReadsFirst() throws Exception {
    checkZeroLengthReadWithOtherReadsFirst(LongRange.NONE);
  }

  public void testZeroLengthReadWithOtherReadsFirstNoEnd() throws Exception {
    checkZeroLengthReadWithOtherReadsFirst(new LongRange(0, LongRange.MISSING));
  }

  public void testZeroLengthReadWithOtherReadsFirstRange() throws Exception {
    checkZeroLengthReadWithOtherReadsFirst(new LongRange(0, 2));
  }

  public void testZeroLengthReadWithOtherReadsAfter() throws Exception {
    try (FastaSequenceDataSource fsds = getDataSource(">null\n\n>read\nACGT\n")) {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    }

    SequencesReaderFactory.createDefaultSequencesReader(mDir);

    try (SequencesReader sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(mDir, true, true, LongRange.NONE)) {
      assertEquals(2, sr.numberSequences());
      final SequencesIterator it = sr.iterator();
      it.nextSequence();
      assertEquals(0, it.currentLength());
      it.nextSequence();
      assertEquals(4, it.currentLength());
    }
  }
  public void testZeroLengthReadWithOtherReadsBeforeAndAfter() throws Exception {
    try (FastaSequenceDataSource fsds = getDataSource(">read1\nACGTG\n>null\n\n>read\nACGT\n")) {
      final SequencesWriter sw = new SequencesWriter(fsds, mDir, 10000000, PrereadType.UNKNOWN, false);
      sw.processSequences();
    }

    SequencesReaderFactory.createDefaultSequencesReader(mDir);

    try (SequencesReader sr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(mDir, true, true, LongRange.NONE)) {
      assertEquals(3, sr.numberSequences());
      final SequencesIterator it = sr.iterator();
      it.nextSequence();
      assertEquals(5, it.currentLength());
      it.nextSequence();
      assertEquals(0, it.currentLength());
      it.nextSequence();
      assertEquals(4, it.currentLength());
    }
  }

  public void testTrim() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream("@123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n"
                      + "+123456789012345678901\n!!ASDFFSAFASHSKFSDIUR<<SA><>S<<<\n"
                      + "@bob the buuilder\ntagttcagcatcgatca\n"
                      + "+bob the buuilder\n!ADSFG<<{()))[[[]\n"
                      + "@hobos-r us\naccccaccccacaaacccaa\n"
                      + "+hobos-r us\nADSFAD[[<<<><<[[;;FS\n"));
    final FastqSequenceDataSource ds = new FastqSequenceDataSource(al, FastQScoreType.PHRED);
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 100000, null, PrereadType.SOLEXA, false, 50);
    final SequencesReader sr = sw.processSequencesInMemory(null, true, null, null, LongRange.NONE);

    assertEquals(3, sr.numberSequences());
    assertEquals(0, sr.length(0));
    assertEquals(17, sr.length(1));
    assertEquals(20, sr.length(2));
  }
  public void testNoTrimNoQuals() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">123456789012345678901\n"
        + "acgtgtgtgtcttagggctcactggtcatgca\n"
        + ">bob the buuilder\n"
        + "tagttcagcatcgatca\n"
        + ">hobos-r us"
        + "\naccccaccccacaaacccaa\n"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 100000, null, PrereadType.SOLEXA, false, 50);
    final File proxy = new File("proxy");
    final SequencesReader sr = sw.processSequencesInMemory(proxy, true, null, null, LongRange.NONE);

    assertEquals(3, sr.numberSequences());
    assertEquals(32, sr.length(0));
    assertEquals(17, sr.length(1));
    assertEquals(20, sr.length(2));
    assertEquals(proxy, sr.path());
  }
}

