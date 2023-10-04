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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Tests corresponding class
 */
public class Sdf2CgTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new Sdf2Cg();
  }

  public void testHelp() {
    checkHelp("rtg sdf2cg"
      , "Converts SDF formatted data into Complete Genomics TSV"
      , "-i,", "SDF containing sequences"
      , "-o,", "output filename"
    );
  }

  public void testSdf2CgVersion1() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("sdf2cg")) {
      final File sdf = new File(tempDir, "sdf");
      final File sample = new File(tempDir, "sample.tsv.gz");
      FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample-v1.tsv"), sample);
      MainResult.run(new Cg2Sdf(), sample.getPath(), "-o", sdf.getPath());

      final File out = new File(tempDir, "out.tsv.gz");
      checkMainInitOk("-i", sdf.getAbsolutePath(), "-o", out.getPath());
      mNano.check("sdf2cg-v1.tsv", FileHelper.gzFileToString(out));
    }
  }

  public void testSdf2CgVersion2() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("sdf2cg")) {
      final File sdf = new File(tempDir, "sdf");
      final File sample = new File(tempDir, "sample.tsv.gz");
      FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample-v2.tsv"), sample);
      MainResult.run(new Cg2Sdf(), sample.getPath(), "-o", sdf.getPath());

      final File out = new File(tempDir, "out.tsv.gz");
      checkMainInitOk("-i", sdf.getAbsolutePath(), "-o", out.getPath());
      mNano.check("sdf2cg-v2.tsv", FileHelper.gzFileToString(out));
    }
  }

  public void testCg2SdfNoQuality() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("cg2sdf")) {
      final File sdf = new File(tempDir, "sdf");
      final File sample = new File(tempDir, "sample.tsv.gz");
      FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample-v1.tsv"), sample);
      MainResult.run(new Cg2Sdf(), sample.getPath(), "-o", sdf.getPath(), "--no-quality");

      final File out = new File(tempDir, "out.tsv.gz");
      final String res = checkMainInitBadFlags("-i", sdf.getAbsolutePath(), "-o", out.getPath());
      TestUtils.containsAll(res, "does not contain quality");
    }
  }
}

