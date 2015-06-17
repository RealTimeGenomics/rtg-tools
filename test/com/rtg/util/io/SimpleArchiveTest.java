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
package com.rtg.util.io;

import java.io.File;
import java.io.IOException;

import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 * Test for corresponding class
 */
public class SimpleArchiveTest extends TestCase {

  private static final String CONTENTS_1 = "a small brown fox";
  private static final String FILENAME_1 = "testFile1";
  private static final String CONTENTS_2 = "lived in a hole";
  private static final String FILENAME_2 = "testFile2";

  public void test() throws IOException {
    final File testDir = FileUtils.createTempDir("simplearchive", "test");
    try {
      final File orig = new File(testDir, "orig");
      final File archive = new File(testDir, "test.dwa");
      final File origFile1 = new File(orig, FILENAME_1);
      final File origFile2 = new File(orig, FILENAME_2);
      assertTrue(orig.mkdir());
      FileUtils.stringToFile(CONTENTS_1, origFile1);
      FileUtils.stringToFile(CONTENTS_2, origFile2);
      SimpleArchive.writeArchive(archive, origFile1, origFile2);
      final File extractDir = new File(testDir, "extract");
      SimpleArchive.unpackArchive(archive, extractDir);
      assertEquals(CONTENTS_1, FileUtils.fileToString(new File(extractDir, FILENAME_1)));
      assertEquals(CONTENTS_2, FileUtils.fileToString(new File(extractDir, FILENAME_2)));
    } finally {
      assertTrue(FileHelper.deleteAll(testDir));
    }
  }


}
