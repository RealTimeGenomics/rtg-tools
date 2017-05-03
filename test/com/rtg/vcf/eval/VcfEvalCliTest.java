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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;

import com.rtg.reader.ReaderTestUtils;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

public class VcfEvalCliTest extends AbstractVcfEvalTest {

  public void testInitParams() {
    checkHelp("vcfeval [OPTION]... -b FILE -c FILE -o DIR -t SDF"
      , "Evaluates"
      , "called variants"
      , "baseline variants"
      , "directory for output"
      , "reference genome"
      , "--sample", "the name of the sample to select"
      , "-f,", "--vcf-score-field", "the name of the VCF FORMAT field to use as the ROC score"
      , "-O,", "--sort-order", "the order in which to sort the ROC scores"
      , "--no-gzip", "-Z,", "do not gzip the output"
    );
  }

  public void testValidator() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfevalcli")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "calls");
      final File mutations = new File(dir, "mutations");
      final File template = new File(dir, "template");
      final String[] flagStrings = {
        "-o", out.getPath()
        , "-c", calls.getPath()
        , "-b", mutations.getPath()
        , "-t", template.getPath()
      };
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--baseline file", "does not exist");

      assertTrue(mutations.createNewFile());
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
      new TabixIndexer(mutations).saveVcfIndex();
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--calls file", "does not exist");

      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();
      checkHandleFlagsErr(flagStrings);
      ReaderTestUtils.getDNADir(">t" + StringUtils.LS + "ACGT" + StringUtils.LS, template);

      TestUtils.containsAllUnwrapped(checkHandleFlagsErr("-o", out.getPath(), "-c", calls.getPath(), "-b", mutations.getPath(), "-t", template.getPath(),
        "--output-mode", "combine", "--sample", "ALT"), "--output-mode=combine cannot be used");

      checkHandleFlags(flagStrings);

      assertTrue(out.mkdir());
      checkHandleFlagsErr(flagStrings);
    }
  }

  public void testEmpty() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfevalcli")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "calls.vcf.gz");
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();
      final File mutations = new File(dir, "mutations.vcf.gz");
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
      new TabixIndexer(mutations).saveVcfIndex();
      final File template = new File(dir, "template");
      ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "A", template, null);
      checkMainInitBadFlags("-o", out.getPath()
        , "-c", calls.getPath()
        , "-b", mutations.getPath()
        , "-t", template.getPath());
    }
  }
}
