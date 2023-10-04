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

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractEndToEndTest;
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

public abstract class AbstractVcfEvalTest extends AbstractEndToEndTest {

  @Override
  protected AbstractCli getCli() {
    return new VcfEvalCli();
  }

  @Override
  protected void endToEnd(String harnessId, String resultsId, int expectRc, boolean expectWarn, OutputChecker outputChecker, String... args) throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfeval-nano")) {
      final File template = new File(dir, "template");
      ReaderTestUtils.getReaderDNA(mNano.loadReference(harnessId + "_in_template.fa"), template, new SdfId(0));

      final File baseline = new File(dir, "baseline.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(harnessId + "_in_baseline.vcf"), baseline);
      new TabixIndexer(baseline).saveVcfIndex();

      final File calls = new File(dir, "calls.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(harnessId + "_in_calls.vcf"), calls);
      new TabixIndexer(calls).saveVcfIndex();

      final File output = new File(dir, "output");

      final String[] fullArgs = Utils.append(args, "-o", output.getPath(), "-c", calls.getPath(), "-b", baseline.getPath(), "-t", template.getPath(), "-Z");
      final MainResult res = MainResult.run(getCli(), fullArgs);

      checkResults(resultsId, expectRc, expectWarn, outputChecker, output, res);
    }
  }
}
