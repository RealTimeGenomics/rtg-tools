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

import com.rtg.reader.AbstractStreamManager.RollingFile;
import com.rtg.util.Resources;
import com.rtg.util.StringUtils;
import com.rtg.util.array.longindex.LongArray;
import com.rtg.util.array.longindex.LongCreate;
import com.rtg.util.array.longindex.LongIndex;
import com.rtg.util.bytecompression.ByteArray;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.SimpleArchive;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class PointerFileHandlerTest extends TestCase {

  private static final String SEQUENCE =  ""
                                        + "@Name1" + StringUtils.LS
                                        + "ACGTACGTACGTACG" + StringUtils.LS
                                        + "+" + StringUtils.LS
                                        + "```````````````" + StringUtils.LS
                                        + "@Name2" + StringUtils.LS
                                        + "ACGTACGTACGTACG" + StringUtils.LS
                                        + "+" + StringUtils.LS
                                        + "```````````````" + StringUtils.LS
                                        + "@Name3" + StringUtils.LS
                                        + "ACGTACGTACGTACG" + StringUtils.LS
                                        + "+" + StringUtils.LS
                                        + "```````````````" + StringUtils.LS
                                        + "@Name4" + StringUtils.LS
                                        + "ACGTACGTACGTACG" + StringUtils.LS
                                        + "+" + StringUtils.LS
                                        + "```````````````" + StringUtils.LS;
  public void testSomeMethod() throws IOException {
    final File dir = FileUtils.createTempDir("tset", "pointerhandler");
    try {
      final File sdf = new File(dir, "sdf");
      final SequencesReader sdfReader = ReaderTestUtils.getReaderDNAFastq(SEQUENCE, sdf, 20, false);
      sdfReader.close();
      final IndexFile mainIndex = new IndexFile(sdf);
      final PointerFileHandler f = PointerFileHandler.getHandler(mainIndex, PointerFileHandler.SEQUENCE_POINTER);
      final DataFileIndex index = DataFileIndex.loadSequenceDataFileIndex(mainIndex.dataIndexVersion(), sdf);
      final RollingFile rf = new RollingFile(sdf, SdfFileUtils.SEQUENCE_POINTER_FILENAME, index.numberEntries());
      try {
        assertEquals(2, index.numberSequences(0));
        rf.openDataFile(0);
        f.initialisePosition(0, 0, rf, index);
        assertEquals(15, f.seqLength());
        assertEquals(0, f.seqPosition());
        assertEquals((byte) 230, f.checksum());
        assertEquals((byte) 244, f.qualityChecksum());
        f.initialisePosition(0, 1, rf, index);
        assertEquals(15, f.seqLength());
        assertEquals(15, f.seqPosition());
        assertEquals((byte) 230, f.checksum());
        assertEquals((byte) 244, f.qualityChecksum());

        assertEquals(1, index.numberSequences(1));
        assertTrue(rf.rollFile());
        f.initialisePosition(1, 0, rf, index);
        assertEquals(15, f.seqLength());
        assertEquals(10, f.seqPosition());
        assertEquals((byte) 230, f.checksum());
        assertEquals((byte) 244, f.qualityChecksum());
        assertEquals(1, index.numberSequences(2));
        assertTrue(rf.rollFile());
        f.initialisePosition(2, 0, rf, index);
        assertEquals(15, f.seqLength());
        assertEquals(5, f.seqPosition());
        assertEquals((byte) 230, f.checksum());
        assertEquals((byte) 244, f.qualityChecksum());
        assertFalse(rf.rollFile());
        assertEquals(0, f.readPointer(SdfFileUtils.sequencePointerFile(sdf, 0), 0));
        assertEquals(15, f.readPointer(SdfFileUtils.sequencePointerFile(sdf, 0), 1));
        final LongIndex li = LongCreate.createIndex(2);
        final LongIndex li2 = LongCreate.createIndex(2);
        final ByteArray ch = ByteArray.allocate(2);
        final ByteArray ch2 = ByteArray.allocate(2);
        f.readPointers(SdfFileUtils.sequencePointerFile(sdf, 0), 0, 2, li, 0, 13, ch, ch2);
        li2.set(0, 13);
        li2.set(1, 28);
        assertEquals(li2.toString(), li.toString());
      } finally {
        rf.close();
      }
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testOriginal() throws Exception {
    final File testDir = FileUtils.createTempDir("sdfstats", "ver4");
    try {
      try (InputStream archive = Resources.getResourceAsStream("com/rtg/reader/resources/sdfver9.arch")) {
        SimpleArchive.unpackArchive(archive, testDir);
      }
      final IndexFile inf = new IndexFile(testDir);
      final PointerFileHandler pfh = PointerFileHandler.getHandler(inf, PointerFileHandler.SEQUENCE_POINTER);
      assertEquals(4, pfh.mEntrySize);
      assertEquals(0, pfh.mChecksumSize);
      assertEquals(9, pfh.readPointer(new File(testDir, SdfFileUtils.INDEX_FILENAME), 1L));
      final LongIndex li = new LongArray(10);
      assertEquals(2, pfh.readPointers(new File(testDir, SdfFileUtils.INDEX_FILENAME), 1, 3, li, 0, 2, null, null));
    } finally {
      FileHelper.deleteAll(testDir);
    }
  }

}
