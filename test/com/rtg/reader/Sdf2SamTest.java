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
import com.rtg.util.StringUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Tests for corresponding class.
 */
public class Sdf2SamTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new Sdf2Sam();
  }

  public void testHelp() {
    checkHelp("output filename (extension added if not present)",
      "SDF containing sequences"
    );
  }

  static final String FULL_NAME_DATA_1 = ""
    + "@name suffix" + StringUtils.LS
    + "ACGTCG" + StringUtils.LS
    + "+name suffix" + StringUtils.LS
    + "123456" + StringUtils.LS
    + "@second suffixes" + StringUtils.LS
    + "ACGGGT" + StringUtils.LS
    + "+second suffixes" + StringUtils.LS
    + "445565" + StringUtils.LS;

  static final String FULL_NAME_DATA_2 = ""
    + "@name suffix" + StringUtils.LS
    + "TTAATA" + StringUtils.LS
    + "+name suffix" + StringUtils.LS
    + "178876" + StringUtils.LS
    + "@second suffixes" + StringUtils.LS
    + "CGGCCG" + StringUtils.LS
    + "+second suffixes" + StringUtils.LS
    + "177888" + StringUtils.LS;

  public void testSingleEnd() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getReaderDNAFastq(FULL_NAME_DATA_1, sdf, false);
      final File sam = new File(dir, "data.sam.gz");
      checkMainInitOk("-i", sdf.getPath(), "-o", sam.getPath());
      mNano.check("sdf2sam-se-sam", StringUtils.grepMinusV(FileHelper.gzFileToString(sam), "@PG"));
    }
  }

  public void testPairedEnd() throws IOException {
    try (TestDirectory dir = new TestDirectory()) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.createPairedReaderDNAFastq(FULL_NAME_DATA_1, FULL_NAME_DATA_2, sdf, new SdfId(0));
      final File sam = new File(dir, "data.sam.gz");
      checkMainInitOk("-i", sdf.getPath(), "-o", sam.getPath());
      mNano.check("sdf2sam-pe-sam", StringUtils.grepMinusV(FileHelper.gzFileToString(sam), "@PG"));
    }
  }
}
