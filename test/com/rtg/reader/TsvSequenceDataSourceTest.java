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

import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test the corresponding class.
 */
public class TsvSequenceDataSourceTest extends TestCase {

  private File mSample = null;
  private File mSampleBzip2 = null;
  private File mDir = null;
  @Override
  public void setUp() throws Exception {
    Diagnostic.setLogStream();
    mDir = FileUtils.createTempDir("tsvseqdatasource", "Test");
    mSample = new File(mDir, "sample.tsv.gz");
    FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample.tsv"), mSample);
    mSampleBzip2 = new File(mDir, "sample.tsv.bz2");
    FileHelper.resourceToFile("com/rtg/reader/resources/sample.tsv.bz2", mSampleBzip2);
  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mDir);
    mSample = null;
    mSampleBzip2 = null;
    mDir = null;
  }

  private void checkRead(TsvSequenceDataSource ds, String expected) {
    assertEquals(expected.length(), ds.currentLength());
    final DNAFastaSymbolTable t = new DNAFastaSymbolTable();
    final byte[] b = ds.sequenceData();
    for (int i = 0; i < expected.length(); i++) {
      assertEquals(t.scanResidue(expected.charAt(i)).ordinal(), b[i]);
    }
  }

  private void checkQuality(TsvSequenceDataSource ds, String expected) {
    assertEquals(expected.length(), ds.currentLength());
    final byte[] q = ds.qualityData();
    assertNotNull(q);
    for (int i = 0; i < expected.length(); i++) {
      assertEquals(expected.charAt(i) - '!', q[i]);
    }
  }

  public void test1() throws Exception {
    try (TsvSequenceDataSource ds = new TsvSequenceDataSource(mSample, null)) {
      assertEquals(SequenceType.DNA, ds.type());
      assertTrue(ds.hasQualityData());
      assertTrue(ds.nextSequence());
      assertEquals("1-A", ds.name());
      checkRead(ds, "AAAAAAAAAA");
      checkQuality(ds, "ABCDEFGHIJ");
      assertTrue(ds.nextSequence());
      checkRead(ds, "CCCCCCCCCC");
      checkQuality(ds, "JIHGFEDCBA");
      assertEquals("1-B", ds.name());
      assertTrue(ds.nextSequence());
      checkRead(ds, "GGGGGGGGGG");
      assertEquals("2-A", ds.name());
      assertTrue(ds.nextSequence());
      checkRead(ds, "TTTTTTTTTT");
      assertEquals("2-B", ds.name());
      assertTrue(ds.nextSequence());
      checkQuality(ds, "56789:;<=>");
      checkRead(ds, "ATATATATAT");
      assertTrue(ds.nextSequence());
      checkQuality(ds, "?@ABCDEFGH");
      checkRead(ds, "ATATATATAT");
      assertFalse(ds.nextSequence());
    }
  }

  public void testBzip2() throws Exception {
    try (TsvSequenceDataSource ds = new TsvSequenceDataSource(mSampleBzip2, null)) {
      assertEquals(SequenceType.DNA, ds.type());
      assertTrue(ds.hasQualityData());
      assertTrue(ds.nextSequence());
      assertEquals("1-A", ds.name());
      checkRead(ds, "AAAAAAAAAA");
      checkQuality(ds, "ABCDEFGHIJ");
      assertTrue(ds.nextSequence());
      checkRead(ds, "CCCCCCCCCC");
      checkQuality(ds, "JIHGFEDCBA");
      assertEquals("1-B", ds.name());
      assertTrue(ds.nextSequence());
      checkRead(ds, "GGGGGGGGGG");
      assertEquals("2-A", ds.name());
      assertTrue(ds.nextSequence());
      checkRead(ds, "TTTTTTTTTT");
      assertEquals("2-B", ds.name());
      assertTrue(ds.nextSequence());
      checkQuality(ds, "56789:;<=>");
      checkRead(ds, "ATATATATAT");
      assertTrue(ds.nextSequence());
      checkQuality(ds, "?@ABCDEFGH");
      checkRead(ds, "ATATATATAT");
      assertFalse(ds.nextSequence());
    }
  }

}
