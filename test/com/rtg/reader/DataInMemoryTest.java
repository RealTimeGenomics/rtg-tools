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
import java.util.Arrays;

import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 */
public class DataInMemoryTest extends TestCase {

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
  }

  private static final String SEQ_DATA = ""
          + "@read0" + StringUtils.LS
          + "GCGTGAGTACGTGACTGAGCGGCATGCTGAAATCC" + StringUtils.LS           //35
          + "+read0" + StringUtils.LS
          + "OSQSSRNS'&*DSQNPM%KPO$PQRHHHRNPQIRS" + StringUtils.LS
          + "@read1" + StringUtils.LS
          + "AAATCG" + StringUtils.LS                                        //6
          + "+read1" + StringUtils.LS
          + "ERQSRI" + StringUtils.LS
          + "@read2" + StringUtils.LS
          + "GAGGATCCTTAAGTGTC" + StringUtils.LS                             //17
          + "+read2" + StringUtils.LS
          + "OHPPGRQSORFRSRQSP" + StringUtils.LS
          + "@read3" + StringUtils.LS
          + "ACTTTTCAGCAGGTCGAGTTCTCCTATACTGAAAG" + StringUtils.LS           //35
          + "+read3" + StringUtils.LS
          + "SQNQNMPNIPLIPGQRI?PMPOOSMRISSRHPMSR" + StringUtils.LS
          + "@read4" + StringUtils.LS
          + "ACTCGTCTAGTACGTTGTCCACGAACCAGG" + StringUtils.LS                //30
          + "+read4" + StringUtils.LS
          + "SLOODPNLR=$NNRQOPSRINRNRSPSOMR" + StringUtils.LS;

  private static final int[] LENGTHS = {35, 6, 17, 35, 30};
  private static final int[][] POINTERS = {//assuming 20 bases per file
          new int[] {0, 20},
          new int[] {15, 20},
          new int[] {1, 18, 20},
          new int[] {20},
          new int[] {13, 20},
          new int[] {20},
          new int[] {3}
  };

  public void testLengths() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 20);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 5);
      assertEquals(35, mem.lengthBetween(0, 1));
      assertEquals(6, mem.lengthBetween(1, 2));
      assertEquals(17, mem.lengthBetween(2, 3));
      assertEquals(35, mem.lengthBetween(3, 4));
      assertEquals(30, mem.lengthBetween(4, 5));
      assertEquals(58, mem.lengthBetween(1, 4));
      for (int i = 0; i < LENGTHS.length; ++i) {
        for (int j = i + 1; j < LENGTHS.length; ++j) {
          final int[] lengths = mem.sequenceLengths(i, j);
          assertTrue(Arrays.equals(Arrays.copyOfRange(LENGTHS, i, j), lengths));
        }
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testSubRange() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 20);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 6);
      final FastqSequenceDataSource fastq = new FastqSequenceDataSource(Arrays.asList((InputStream) new ByteArrayInputStream(SEQ_DATA.getBytes())), QualityFormat.SANGER);
      assertTrue(fastq.nextSequence());
        final byte[] exp = Arrays.copyOfRange(fastq.sequenceData(), 25, 30);
        final byte[] act = new byte[5];
        assertEquals(exp.length, act.length);
        mem.readSequence(0, act, 25, 5);
        assertTrue("i: " + 0 + " \nExp: " + Arrays.toString(exp) + " \nACT: " + Arrays.toString(act), Arrays.equals(exp, act));
        final byte[] expQual = Arrays.copyOfRange(fastq.qualityData(), 25, 30);
        final byte[] actQual = new byte[5];
        mem.readQuality(0, actQual, 25, 5);
        assertTrue(Arrays.equals(expQual, actQual));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testFileBoundary() throws IOException {
    // Testing second long in second file of sdf...
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      final String data = FileHelper.resourceToString("com/rtg/reader/resources/reads100.fastq");
      ReaderTestUtils.getReaderDNAFastq(data, dir, new SdfId(), 1234);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 1000);
      final FastqSequenceDataSource fastq = new FastqSequenceDataSource(Arrays.asList((InputStream) new ByteArrayInputStream(data.getBytes())), QualityFormat.SANGER);

      int index = 0;
      while (fastq.nextSequence()) {
        final byte[] exp = Arrays.copyOf(fastq.sequenceData(), fastq.currentLength());
        final byte[] act = new byte[exp.length];
        final int length = mem.readSequence(index, act, 0, act.length);
        assertEquals("index " + index + "\n" + exp.length + " " + length, exp.length, length);
        assertTrue("index " + index + " \nExp: " + Arrays.toString(exp) + " \nACT: " + Arrays.toString(act), Arrays.equals(exp, act));
        ++index;
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }


  public void testData() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 1234);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 6);
      final FastqSequenceDataSource fastq = new FastqSequenceDataSource(Arrays.asList((InputStream) new ByteArrayInputStream(SEQ_DATA.getBytes())), QualityFormat.SANGER);
      int i = 0;
      while (fastq.nextSequence()) {
        final byte[] exp = Arrays.copyOf(fastq.sequenceData(), fastq.currentLength());
        final byte[] act = new byte[mem.length(i)];
        assertEquals(exp.length, act.length);
        mem.readSequence(i, act, 0, Integer.MAX_VALUE);
        assertTrue("i: " + i + " \nExp: " + Arrays.toString(exp) + " \nACT: " + Arrays.toString(act), Arrays.equals(exp, act));
        final byte[] expQual = Arrays.copyOf(fastq.qualityData(), fastq.currentLength());
        final byte[] actQual = new byte[act.length];
        mem.readQuality(i, actQual, 0, Integer.MAX_VALUE);
        assertTrue(Arrays.equals(expQual, actQual));
        ++i;
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testRange() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 20);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 1, 4);
      final FastqSequenceDataSource fastq = new FastqSequenceDataSource(Arrays.asList((InputStream) new ByteArrayInputStream(SEQ_DATA.getBytes())), QualityFormat.SANGER);
      final int iadj = 1;
      final int imax = 4;
      int i = 0;
      while (fastq.nextSequence()) {
        try {
          if (i < iadj) {
            continue;
          } else if (i >= imax) {
            break;
          }
          final byte[] exp = Arrays.copyOf(fastq.sequenceData(), fastq.currentLength());
          final byte[] act = new byte[mem.length(i - iadj)];
          assertEquals(exp.length, act.length);
          mem.readSequence(i - iadj, act, 0, Integer.MAX_VALUE);
          assertTrue("i: " + i + " \nExp: " + Arrays.toString(exp) + " \nACT: " + Arrays.toString(act), Arrays.equals(exp, act));
          final byte[] expQual = Arrays.copyOf(fastq.qualityData(), fastq.currentLength());
          final byte[] actQual = new byte[act.length];
          mem.readQuality(i - iadj, actQual, 0, Integer.MAX_VALUE);
          assertTrue(Arrays.equals(expQual, actQual));
        } finally {
          ++i;
        }
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
  public void testPointers() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 20);
      final IndexFile f = new IndexFile(dir);
      final DataInMemory.PointerLoader foo = new DataInMemory.PointerLoader(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 10);
      foo.loadPointers();
      for (int i = 0; i < POINTERS.length; ++i) {
        assertTrue("i: " + i + " EXP: " + Arrays.toString(POINTERS[i]) + " ACT: " + Arrays.toString(foo.mPointers[i]),  Arrays.equals(POINTERS[i], foo.mPointers[i]));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));

    }
  }
  public void test() throws IOException {
    final File dir = FileUtils.createTempDir("test", "dir");
    try {
      ReaderTestUtils.getReaderDNAFastq(SEQ_DATA, dir, new SdfId(), 20);
      final IndexFile f = new IndexFile(dir);

      final DataInMemory mem = DataInMemory.loadDelayQuality(dir, f, DataFileIndex.loadSequenceDataFileIndex(f.dataIndexVersion(), dir), 0, 10);
      assertTrue(Arrays.equals(LENGTHS, mem.sequenceLengths(0, 5)));
      for (int i = 0; i < LENGTHS.length; ++i) {
        assertEquals("i: "  + i, LENGTHS[i], mem.length(i));
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
