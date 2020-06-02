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

package com.rtg.variant.sv.bndeval;

import java.io.File;
import java.io.IOException;

import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

import junit.framework.TestCase;

public class BndEvalCliTest extends AbstractBndEvalTest {

  public void testInitParams() {
    checkHelp("bndeval [OPTION]... -b FILE -c FILE -o DIR"
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
    try (TestDirectory dir = new TestDirectory("bndevalcli")) {
      final File out = new File(dir, "out");
      final File calls = new File(dir, "calls");
      final File mutations = new File(dir, "mutations");
      final String[] flagStrings = {
        "-o", out.getPath()
        , "-c", calls.getPath()
        , "-b", mutations.getPath()
      };
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--baseline file", "does not exist");

      TestCase.assertTrue(mutations.createNewFile());
      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
      new TabixIndexer(mutations).saveVcfIndex();
      TestUtils.containsAllUnwrapped(checkHandleFlagsErr(flagStrings), "--calls file", "does not exist");

      FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
      new TabixIndexer(calls).saveVcfIndex();

      checkHandleFlags(flagStrings);

      TestCase.assertTrue(out.mkdir());
      checkHandleFlagsErr(flagStrings);
    }
  }

  public void testNanoSmallDefault() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small", "bndeval_small_default", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false);
  }

  public void testNanoSmallTol() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small_tol", "bndeval_small_tol", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false, "--tolerance", "4");
  }

  public void testNanoSmallAnnotate() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small", "bndeval_small_annotate", new String[] {"summary.txt", "baseline.vcf", "calls.vcf"}, false, "--no-roc", "--output-mode", "annotate");
  }

  public void testNanoSmallAll() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small", "bndeval_small_all", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false, "--vcf-score-field", "QUAL", "--all-records");
  }

  public void testNanoSmallBidirectional() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small_bidirectional", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false, "--vcf-score-field", "QUAL", "--bidirectional");
  }
  public void testNanoSmallBidirectionalAnnotate() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small_bidirectional", "bndeval_small_bidirectional_annotate", new String[] {"summary.txt", "baseline.vcf", "calls.vcf"}, false, "--vcf-score-field", "QUAL", "--bidirectional", "--output-mode", "annotate");
  }
  public void testNanoSmallBidirectional2() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small_bidirectional2", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false, "--vcf-score-field", "QUAL", "--bidirectional");
  }
  public void testNanoSmallBidirectional3() throws IOException, UnindexableDataException {
    endToEnd("bndeval_small", "bndeval_small_bidirectional3", new String[] {"summary.txt", "tp.vcf", "tp-baseline.vcf", "fp.vcf", "fn.vcf"}, false, "--vcf-score-field", "QUAL", "--bidirectional");
  }

}
