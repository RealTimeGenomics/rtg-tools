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

package com.rtg.tabix;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import com.rtg.sam.SamRegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.BgzipFileHelper;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test class
 */
public class TabixIndexReaderTest extends TestCase {

  public void testSomeMethod() throws IOException {
    final File dir = FileUtils.createTempDir("tabixIndexer", "test");
    try {
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final TabixIndexReader tir = new TabixIndexReader(tabix);
      assertEquals(false, tir.getOptions().mZeroBased);
      assertEquals(2, tir.getOptions().mFormat);
      assertEquals((int) '#', tir.getOptions().mMeta);
      assertEquals(0, tir.getOptions().mSkip);
      assertEquals(1, tir.getOptions().mStartCol);
      assertEquals(-1, tir.getOptions().mEndCol);
      assertEquals(0, tir.getOptions().mSeqCol);
      VirtualOffsets positions = tir.getFilePointers(new SamRegionRestriction("simulatedSequence19", 0, 5000));
      assertEquals(5480L, positions.start(0));
      assertEquals(6025L, positions.end(0));
      positions = tir.getFilePointers(new SamRegionRestriction("simulatedSequence19", 500, 1000));
      assertEquals(5480L, positions.start(0));
      assertEquals(6025L, positions.end(0));
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testOverlap() throws IOException {
    final File dir = FileUtils.createTempDir("tabixIndexer", "test");
    try {
      final File tabix = new File(dir, "readerWindow1.sam.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/readerWindow1.sam.gz.tbi", tabix);
      final TabixIndexReader tir = new TabixIndexReader(tabix);
      assertEquals(false, tir.getOptions().mZeroBased);
      assertEquals(1, tir.getOptions().mFormat);
      assertEquals((int) '@', tir.getOptions().mMeta);
      assertEquals(0, tir.getOptions().mSkip);
      assertEquals(3, tir.getOptions().mStartCol);
      assertEquals(-1, tir.getOptions().mEndCol);
      assertEquals(2, tir.getOptions().mSeqCol);
      final VirtualOffsets positions = tir.getFilePointers(new SamRegionRestriction("simulatedSequence2", 32769, 33000));
      assertEquals((44947L << 16) | 22268, positions.start(0));
      assertEquals((56124L << 16) | 50222, positions.end(0));
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

  public void testErrors() throws IOException {
    final File dir = FileUtils.createTempDir("tabixIndexer", "test2");
    try {
      final File f = FileUtils.stringToFile("stuff", new File(dir, "things"));
      try {
        new TabixIndexReader(f);
        fail();
      } catch (final IOException e) {
        assertFalse(e instanceof EOFException);
        assertTrue(e.getMessage().contains("TABIX"));
        assertTrue(e.getMessage().contains("not a valid"));
        assertTrue(e.getMessage().contains("block compressed"));
      }
      final File f2 = BgzipFileHelper.bytesToBgzipFile("stuff".getBytes(), new File(dir, "things.gz"));
      try {
        new TabixIndexReader(f2);
        fail();
      } catch (final EOFException e) {
        assertTrue(e.getMessage().contains("TABIX"));
        assertTrue(e.getMessage().contains("not a valid"));
        assertTrue(e.getMessage().contains("complete header"));
      }

      final File f3 = BgzipFileHelper.bytesToBgzipFile("stuffstdfafadfhjagjadshfgdjafhajdghadsjfhadsjfhjadsfhajkdsfhajdkshfajdkfahdjdsa".getBytes(), new File(dir, "things2.gz"));
      try {
        new TabixIndexReader(f3);
        fail();
      } catch (final IOException e) {
        assertFalse(e instanceof EOFException);
        assertTrue(e.getMessage().contains("TABIX"));
        assertTrue(e.getMessage().contains("not a valid"));
        assertTrue(e.getMessage().contains("valid header"));
      }
    } finally {
      FileHelper.deleteAll(dir);
    }
  }

}
