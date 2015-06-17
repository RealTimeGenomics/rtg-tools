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
import java.util.Map;
import java.util.UUID;

import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * JUnit tests for the ReaderUtils class.
 *
 */
public class ReaderUtilsTest extends TestCase {

  public void testIsPairedEndDirectory() throws IOException {
    final File dir = FileUtils.createTempDir("rut", "dontcare");
    try {
      assertFalse(ReaderUtils.isPairedEndDirectory(null));
      assertFalse(ReaderUtils.isPairedEndDirectory(dir));
      final File left = new File(dir.getAbsolutePath() + File.separator + "left");
      final File right = new File(dir.getAbsolutePath() + File.separator + "right");
      assertTrue(left.mkdir());
      assertFalse(ReaderUtils.isPairedEndDirectory(dir));
      assertTrue(right.mkdir());
      assertTrue(ReaderUtils.isPairedEndDirectory(dir));
      assertEquals(left, ReaderUtils.getLeftEnd(dir));
      assertEquals(right, ReaderUtils.getRightEnd(dir));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testValidateNotEmpty() {
    Diagnostic.setLogStream();
    try {
      ReaderUtils.validateNotEmpty(null);
    } catch (NoTalkbackSlimException e) {
      assertEquals("The SDF \"<Unknown>\" was empty.", e.getMessage());
    }
    try {
      ReaderUtils.validateNotEmpty(new MockSequencesReader(SequenceType.DNA, 0));
    } catch (NoTalkbackSlimException e) {
      assertEquals("The SDF \"<Unknown>\" was empty.", e.getMessage());
    }
    try {
      ReaderUtils.validateNotEmpty(new MockSequencesReader(SequenceType.DNA, 0) {
        @Override
        public File path() {
          return new File("boo");
        }
      });
    } catch (NoTalkbackSlimException e) {
      assertEquals("The SDF \"" + new File("boo").getAbsolutePath() + "\" was empty.", e.getMessage());
    }
    ReaderUtils.validateNotEmpty(new MockSequencesReader(SequenceType.DNA, 1));
  }

  public void testgetGuid() throws IOException {
    Diagnostic.setLogStream();
    final File dir = FileUtils.createTempDir("readerutils", "getGuid");
    try {
      final File left = new File(dir, "left");
      ReaderTestUtils.getReaderDNA(">A\nACGT", left, new SdfId(5L)).close();
      final File right = new File(dir, "right");
      ReaderTestUtils.getReaderDNA(">A\nACGT", right, new SdfId(5L)).close();

      assertEquals(new SdfId(new UUID(0, 5L)), ReaderUtils.getSdfId(left));
      assertEquals(new SdfId(new UUID(0, 5L)), ReaderUtils.getSdfId(dir));
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  public void testSequencNameMap() throws IOException {
    final SequencesReader reader = new MockSequencesReader(SequenceType.DNA, 10, 10);
    checkNameMap(reader);
  }

  private void checkNameMap(SequencesReader reader) throws IOException {
    final Map<String, Long> names = ReaderUtils.getSequenceNameMap(reader);
    assertEquals(10, names.size());
    for (int i = 0; i < names.size(); i++) {
      final String key = "seq" + i;
      assertTrue(names.containsKey(key));
      assertEquals(i, (long) names.get(key));
    }
  }

}
