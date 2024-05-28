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

package com.rtg.variant.cnv.cnveval;

import java.io.File;
import java.io.IOException;

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

public class CnvEvalCliTest extends AbstractCnvEvalTest {

  public void testFlags() {
    checkHelp("cnveval [OPTION]... -b FILE -c FILE -e FILE -o DIR"
      , "Evaluate"
      , "called variants"
      , "baseline variants"
      , "directory for output"
      , "-f,", "--vcf-score-field", "the name of the VCF FORMAT field to use as the ROC score"
      , "-O,", "--sort-order", "the order in which to sort the ROC scores"
      , "--no-gzip", "-Z,", "do not gzip the output"
    );
  }

  public void testValidator() throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("cnvevalcli")) {
      final File out = new File(dir, "out");
      final File baseline = new File(dir, "baseline.vcf");
      final File regions = new File(dir, "regions.vcf");
      final File calls = new File(dir, "calls.vcf");
      final String[] flagStrings = {
        "-b", baseline.getPath(),
        "-e", regions.getPath(),
        "-c", calls.getPath(),
        "-o", out.getPath()
      };
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--baseline file", "does not exist");

      assertTrue(baseline.createNewFile());
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", baseline);
      new TabixIndexer(baseline).saveVcfIndex();
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--calls file", "does not exist");

      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();

      checkHandleFlags(flagStrings);

      assertTrue(out.mkdir());
      checkHandleFlagsErr(flagStrings);
    }
  }

  public void testNanoSmallAnnotate() throws IOException, UnindexableDataException {
    endToEnd("cnveval_small", "cnveval_small", new String[] {"summary.txt", "baseline.vcf", "calls.vcf", "weighted_roc.tsv", "del_roc.tsv", "dup_roc.tsv"}, false, "--vcf-score-field", "INFO.SQS", "--roc-subset", "del,dup");
  }

  public void testNanoSmallAnnotateTrivialCrossJoin() throws IOException, UnindexableDataException {
    // Using the same resultsID here - adding --roc-cross-join should not cause baseline rescaling to change
    endToEnd("cnveval_small", "cnveval_small", new String[] {"summary.txt", "baseline.vcf", "calls.vcf", "weighted_roc.tsv", "del_roc.tsv", "dup_roc.tsv"}, false, "--vcf-score-field", "INFO.SQS", "--roc-subset", "del,dup", "--roc-cross-join");
  }

  public void testNanoSmallAnnotateNoRoc() throws IOException, UnindexableDataException {
    endToEnd("cnveval_small", "cnveval_small_noroc", new String[] {"summary.txt", "baseline.vcf", "calls.vcf"}, false, "--no-roc");
  }
}
