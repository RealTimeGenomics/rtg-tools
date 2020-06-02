/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
package com.rtg.vcf;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.TestUtils;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.BgzipFileHelper;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 * Test class
 */
public class VcfSplitCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new VcfSplitCli();
  }

  public void testFlags() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfsplit")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "input.vcf.gz");
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE1\tSAMPLE2\n", calls);
      new TabixIndexer(calls).saveVcfIndex();
      checkMainInitBadFlags("-o", out.getPath(), "-i", calls.getPath(), "--keep-sample", "SAMPLE1", "--remove-sample", "SAMPLE2");
    }
  }

  public void checkSplit(String id, String resource, String... argsIn) throws Exception {
    try (final TestDirectory dir = new TestDirectory("vcfsplit")) {
      final File input = BgzipFileHelper.bytesToBgzipFile(FileHelper.resourceToString("com/rtg/vcf/resources/" + resource).getBytes(), new File(dir, "input.vcf.gz"));
      new TabixIndexer(input, TabixIndexer.indexFileName(input)).saveVcfIndex();
      final File output = new File(dir, "output");
      checkMainInit(Utils.append(argsIn, "-Z", "-o", output.toString(), "-i", input.toString()));
      assertTrue(output.exists());
      assertTrue(output.isDirectory());
      File[] outfiles = output.listFiles((dir1, name) -> name.endsWith(".vcf"));
      assertNotNull(outfiles);
      for (File f: outfiles) {
        mNano.check("vcfsplit_out_" + id + "_" + f.getName(), TestUtils.sanitizeVcfHeader(FileHelper.fileToString(f)), false);
      }
    }
  }

  public void testSplit() throws Exception {
    checkSplit("simple", "vcfsplit-pop.vcf");
  }

  public void testSelect() throws Exception {
    checkSplit("select", "vcfsplit-pop.vcf", "--keep-sample", "son1,son2");
  }

  public void testRemove() throws Exception {
    checkSplit("remove", "vcfsplit-pop.vcf", "--remove-sample", "mother,son1,son2,daughter2");
  }

}
