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

package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractEndToEndTest;
import com.rtg.launcher.MainResult;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

public class Vcf2RocplotTest extends AbstractEndToEndTest {

  @Override
  protected AbstractCli getCli() {
    return new Vcf2Rocplot();
  }

  public void testSimple() throws IOException, UnindexableDataException {
    endToEnd("vcf2rocplot/simple", new String[] {"summary.txt", "weighted_roc.tsv"}, false, "--vcf-score-field", "QUAL", "-T", "1");
  }

  @Override
  protected void endToEnd(String harnessId, String resultsId, String[] filesToCheck, boolean expectWarn, Consumer<File> extracheck, String... args) throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcf2rocplot-nano***")) {
      final File first = new File(dir, "first.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(harnessId + "_in_first.vcf"), first);
      new TabixIndexer(first).saveVcfIndex();

      final File second = new File(dir, "second.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(harnessId + "_in_second.vcf"), second);
      new TabixIndexer(second).saveVcfIndex();

      final File output = new File(dir, "output");

      final String[] fullArgs = Utils.append(args, "-o", output.getPath(), first.getPath(), second.getPath(), "-Z");
      final MainResult res = MainResult.run(getCli(), fullArgs);

      checkResults(resultsId, filesToCheck, expectWarn, extracheck, output, res);
    }
  }
}
