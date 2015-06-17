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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.UUID;

import com.rtg.util.Resources;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.SimpleArchive;
import com.rtg.util.test.FileHelper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for corresponding class.
 *
 */
public class IndexFileTest extends TestCase {

  public IndexFileTest(final String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(IndexFileTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public void testDetectNewerVersion() throws IOException {
    final File dir = ReaderTestUtils.getDNADir("");

    try {
      // Mutate version
      try (RandomAccessFile f = new RandomAccessFile(new File(dir, SdfFileUtils.INDEX_FILENAME), "rws")) {
        f.seek(0);
        f.writeLong(IndexFile.VERSION + 5);
      }

      try {
        final IndexFile idx = new IndexFile(dir);
        fail("Should have failed to read index file with version " + idx.getVersion());
      } catch (NoTalkbackSlimException e) {
        // Expect appropriate message
        assertTrue(e.getMessage().contains("newer version"));
      }
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void test() throws IOException {
    final IndexFile f = new IndexFile(1000, 1, 300, 998, 997, 2);
    final long[] l = {100L, 200L};
    final long[] histo = new long[SdfWriter.MAX_HISTOGRAM];
    histo[0] = 20L;
    histo[1] = 40L;
    histo[2] = 50L;
    final long[] poshisto = new long[SdfWriter.MAX_HISTOGRAM];
    poshisto[0] = 25L;
    poshisto[1] = 45L;
    poshisto[2] = 55L;

    final double[] posAverage = new double[SdfWriter.MAX_HISTOGRAM];
    posAverage[0] = 23.4;
    posAverage[1] = 26.4;
    posAverage[2] = 0.000004;

    f.setNBlocksCount(5);
    f.setLongestNBlock(50);
    f.setResidueCounts(l);
    f.setNHistogram(histo);
    f.setGlobalQSAverage(1234.4566);
    f.setPosHistogram(poshisto);
    f.setDataChecksum(12323123123L);
    f.setQualityChecksum(2323123123L);
    f.setNameChecksum(323123123L);
    f.setQSPostionAverageHistogram(posAverage);

    f.setPrereadArm(PrereadArm.RIGHT);
    f.setPrereadType(PrereadType.SOLEXA);
    f.setSdfId(new SdfId(234324235325L));
    f.setCommandLine("-b 1 -a 1 -w 10");
    f.setComment("omg comment roflcopter hangar");

    assertEquals(1000, f.getMaxFileSize());
    assertEquals(1, f.getSequenceType());
    assertEquals(300, f.getTotalLength());
    assertEquals(998, f.getMaxLength());
    assertEquals(997, f.getMinLength());
    assertEquals(2, f.getNumberSequences());
    assertEquals(2, f.getResidueCounts().length);
    assertEquals(100L, f.getResidueCounts()[0]);
    assertEquals(200L, f.getResidueCounts()[1]);

    assertEquals(20L, f.getNHistogram()[0]);
    assertEquals(40L, f.getNHistogram()[1]);
    assertEquals(50L, f.getNHistogram()[2]);

    assertEquals(25L, f.getPosHistogram()[0]);
    assertEquals(45L, f.getPosHistogram()[1]);
    assertEquals(55L, f.getPosHistogram()[2]);

    assertEquals(23.4, f.getQSPositionAverageHistogram()[0]);
    assertEquals(26.4, f.getQSPositionAverageHistogram()[1]);
    assertEquals(0.000004, f.getQSPositionAverageHistogram()[2]);

    assertEquals(50, f.getLongestNBlock());
    assertEquals(5, f.getNBlockCount());

    assertEquals(1234.4566, f.getQSAverage());

    assertEquals(12323123123L, f.getDataChecksum());
    assertEquals(2323123123L, f.getQualityChecksum());
    assertEquals(323123123L, f.getNameChecksum());

    assertEquals("-b 1 -a 1 -w 10", f.getCommandLine());
    assertEquals("omg comment roflcopter hangar", f.getComment());

    final long version = f.getVersion();
    assertTrue(version > 0);
    final File x = FileHelper.createTempDirectory();
    try {
      f.save(x);
      assertTrue(new File(x, SdfFileUtils.INDEX_FILENAME).exists());
      IndexFile ff = new IndexFile(x);
      assertEquals(1000, ff.getMaxFileSize());
      assertEquals(1, ff.getSequenceType());
      assertEquals(300, ff.getTotalLength());
      assertEquals(998, ff.getMaxLength());
      assertEquals(997, ff.getMinLength());
      assertEquals(2, ff.getNumberSequences());
      assertEquals(version, ff.getVersion());
      assertEquals(2, ff.getResidueCounts().length);
      assertEquals(100L, ff.getResidueCounts()[0]);
      assertEquals(200L, ff.getResidueCounts()[1]);

      assertEquals(20L, ff.getNHistogram()[0]);
      assertEquals(40L, ff.getNHistogram()[1]);
      assertEquals(50L, ff.getNHistogram()[2]);
      assertEquals(25L, ff.getPosHistogram()[0]);
      assertEquals(45L, ff.getPosHistogram()[1]);
      assertEquals(55L, ff.getPosHistogram()[2]);

      assertEquals(23.4, ff.getQSPositionAverageHistogram()[0]);
      assertEquals(26.4, ff.getQSPositionAverageHistogram()[1]);
      assertEquals(0.000004, ff.getQSPositionAverageHistogram()[2]);

      assertEquals(50, ff.getLongestNBlock());
      assertEquals(5, ff.getNBlockCount());
      assertEquals(1234.4566, ff.getQSAverage());

      assertEquals(new SdfId(new UUID(0, 234324235325L)), ff.getSdfId());
      assertEquals(PrereadArm.RIGHT, ff.getPrereadArm());
      assertEquals(PrereadType.SOLEXA, ff.getPrereadType());

      assertEquals("-b 1 -a 1 -w 10", ff.getCommandLine());
      assertEquals("omg comment roflcopter hangar", ff.getComment());

      f.setComment(null);
      f.setCommandLine(null);
      f.save(x);
      ff = new IndexFile(x);
      assertNull(ff.getComment());
      assertNull(ff.getCommandLine());
    } finally {
      assertTrue(FileHelper.deleteAll(x));
    }
  }

  public void testSdfVersion4() throws IOException {
    final File testDir = FileUtils.createTempDir("sdfstats", "ver4");
    try {
      try (InputStream archive = Resources.getResourceAsStream("com/rtg/reader/resources/sdfver4.arch")) {
        SimpleArchive.unpackArchive(archive, testDir);
      }
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final PrintStream ps = new PrintStream(os);
      Diagnostic.setLogStream(ps);
      try {
        new IndexFile(testDir);

      } catch (final NoTalkbackSlimException ntse) {
        ps.flush();
        assertTrue(os.toString().contains("is version 4, which is unsupported by rtg .NET"));
      } finally {
        Diagnostic.setLogStream();
      }

    } finally {
      assertTrue(FileHelper.deleteAll(testDir));
    }
  }

  public void testSdfVersion5() throws IOException {
    final File testDir = FileUtils.createTempDir("sdfstats", "ver5");
    try {
      try (InputStream archive = Resources.getResourceAsStream("com/rtg/reader/resources/sdfver5.arch")) {
        SimpleArchive.unpackArchive(archive, testDir);
      }
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final PrintStream ps = new PrintStream(os);
      Diagnostic.setLogStream(ps);
      try {
        final IndexFile inf = new IndexFile(testDir);

        assertNull(inf.getCommandLine());
        assertNull(inf.getComment());

        ps.flush();
        assertTrue(os.size() == 0);
      } finally {
        Diagnostic.setLogStream();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(testDir));
    }
  }
}

