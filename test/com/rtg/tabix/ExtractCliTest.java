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

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.BgzipFileHelper;
import com.rtg.util.test.FileHelper;

/**
 * Test class
 */
public class ExtractCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ExtractCli();
  }

  public void testHelp() {
    checkHelp("rtg extract"
        , "Extract records from an indexed genomic position data file."
        , "FILE the indexed block compressed genome position data file to extract"
        , "REGION+ the range to display"
        , "--header", "print out header also"
        , "-h,", "--help", "print help on command-line flag usage"
        );
  }

  public void testSomeFlags() {
    checkHandleFlagsErr();
    checkHandleFlagsOut("foo");
    checkHandleFlagsOut("foo", "bar");
    checkHandleFlagsOut("foo", "bar", "bang");
  }

  public void testNormalVcf() throws IOException {
    try (TestDirectory dir = new TestDirectory("extractcli")) {
      final File tabix = new File(dir, "snp_only.vcf.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz.tbi", tabix);
      final File input = new File(dir, "snp_only.vcf.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/snp_only.vcf.gz", input);
      MainResult res = MainResult.run(getCli(), input.getPath(), "simulatedSequence19:500-1000");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract500-1000", res.out());

      res = MainResult.run(getCli(), input.getPath(), "simulatedSequence19:1000-5000");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract1000-5000", res.out());

      res = MainResult.run(getCli(), input.getPath(), "simulatedSequence19:500-5000");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract500-5000", res.out());

      res = MainResult.run(getCli(), input.getPath(), "simulatedSequence19:500-1000", "simulatedSequence19:1000-5000");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract500-1000-5000", res.out());

      res = MainResult.run(getCli(), input.getPath());
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract-norestrict", res.out());

      res = MainResult.run(getCli(), input.getPath(), "--header-only");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract-header-only", res.out());
    }
  }

  public void testSamMulti() throws IOException {
    try (TestDirectory dir = new TestDirectory("extractcli")) {
      final File tabix = new File(dir, "test3.sam.gz.tbi");
      FileHelper.resourceToFile("com/rtg/sam/resources/test3.sam.gz.tbi", tabix);
      final File input = new File(dir, "test3.sam.gz");
      FileHelper.resourceToFile("com/rtg/sam/resources/test3.sam.gz", input);
      final MainResult res = MainResult.run(getCli(), input.getPath(), "simulatedSequence2", "simulatedSequence10", "simulatedSequence1");
      assertEquals(res.err(), 0, res.rc());
      mNano.check("extract-sam-multi", res.out());
    }
  }

  public void testErrors() throws IOException {
    try (TestDirectory dir = new TestDirectory("extractcli")) {
      File input = FileUtils.stringToFile("stuff", new File(dir, "afile"));
      MainResult res = MainResult.run(getCli(), input.getPath());
      assertEquals(1, res.rc());
      assertTrue(res.err().contains("bgzip format"));
      assertTrue(res.err().contains(input.getPath()));
      input = BgzipFileHelper.bytesToBgzipFile("stuff".getBytes(), input);

      res = MainResult.run(getCli(), input.getPath());
      assertEquals(1, res.rc());
      assertTrue(res.err().contains("Index not found"));
      assertTrue(res.err().contains(input.getPath()));
      assertTrue(res.err().contains(input.getPath() + TabixIndexer.TABIX_EXTENSION));
    }
  }

}
