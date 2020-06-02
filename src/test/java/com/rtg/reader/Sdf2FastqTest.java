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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Tests for corresponding class.
 */
public class Sdf2FastqTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new Sdf2Fastq();
  }

  public void testHelp() {
    checkHelp("output filename (extension added if not present)",
      "SDF containing sequences"
    );
  }


  public void testValidator() {
    final String err = checkMainInitBadFlags("-i", "blah", "-o", "blaho", "-l", "-3");
    assertTrue(err.contains("Error: Expected a nonnegative integer for parameter \"line-length\"."));
}

  static final String FULL_NAME_DATA = ""
          + "@name suffix\n"
          + "ACGTCG\n"
          + "+name suffix\n"
          + "123456\n"
          + "@second suffixes\n"
          + "ACGGGT\n"
          + "+second suffixes\n"
          + "123456\n";

  public void testFullName() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File sdf = ReaderTestUtils.getDNAFastqDir(FULL_NAME_DATA, new File(dir, "sdf"), false);
      final File fasta = new File(dir, "fs.fastq.gz");
      checkMainInitOk("-i", sdf.getPath(), "-o", fasta.getPath());
      assertEquals(FULL_NAME_DATA, FileHelper.gzFileToString(fasta));
    }
  }
}
