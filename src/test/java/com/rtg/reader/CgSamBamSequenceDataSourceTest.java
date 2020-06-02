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

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.launcher.MainResult;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 * Tests corresponding class
 */
public class CgSamBamSequenceDataSourceTest extends AbstractNanoTest {

  public void testSdf2CgVersion2() throws Exception {
    try (final TestDirectory tempDir = new TestDirectory("sdf2cg")) {
      final File sdf = new File(tempDir, "sdf");
      final File sample = new File(tempDir, "sample.sam.gz");
      FileHelper.stringToGzFile(FileHelper.resourceToString("com/rtg/reader/resources/sample-cg-v2.sam"), sample);
      MainResult res = MainResult.run(new FormatCli(), sample.getPath(), "--format", "sam-cg", "-o", sdf.getPath());
      assertEquals(res.err(), 0, res.rc());

      final File out = new File(tempDir, "out.tsv.gz");
      res = MainResult.run(new Sdf2Cg(), "-i", sdf.getPath(), "-o", out.getPath());
      assertEquals(res.err(), 0, res.rc());
      mNano.check("format-cg-sam-v2.tsv", FileHelper.gzFileToString(out));
    }
  }

}

