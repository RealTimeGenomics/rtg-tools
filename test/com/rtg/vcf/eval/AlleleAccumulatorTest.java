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

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfMerge;

/**
 */
public class AlleleAccumulatorTest extends AbstractNanoTest {

  public void testAccumulate() throws IOException, UnindexableDataException {
    endToEnd("vcfeval_small_recode");
  }

  public void endToEnd(String id) throws IOException, UnindexableDataException {
    try (TestDirectory dir = new TestDirectory("vcfeval-accumulate")) {
      final File template = new File(dir, "template");
      ReaderTestUtils.getReaderDNA(mNano.loadReference(id + "_in_template.fa"), template, new SdfId(0));

      final File empty = new File(dir, "empty.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(id + "_in_empty.vcf"), empty);
      new TabixIndexer(empty).saveVcfIndex();

      final File samples = new File(dir, "samples.vcf.gz");
      FileHelper.stringToGzFile(mNano.loadReference(id + "_in_samples.vcf"), samples);
      new TabixIndexer(samples).saveVcfIndex();

      // Accumulate 4 samples into population alleles
      final File current = accumulate(dir, template, samples, empty);
      mNano.check(id + "_alleles.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(current)));

      // Run recode of the original samples w.r.t. population alleles
      final File merged = recode(dir, template, samples, current);
      mNano.check(id + "_recoded.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(merged)));
    }
  }

  public File recode(TestDirectory dir, File template, File samples, File alleles) {
    final String[] mergeArgs = new String[6];
    for (int i = 1; i < 5; i++) {
      final File output = new File(dir, "recode" + i);
      final MainResult res = MainResult.run(new VcfEvalCli(),
        "-o", output.getPath(), "--sample", "SAMPLE" + i,
        "-t", template.getPath(), "-b", alleles.getPath(), "-c", samples.getPath(),
        "--ref-overlap",
        "--XXcom.rtg.vcf.eval.custom-path-processor=recode",
        "--XXcom.rtg.vcf.eval.custom-variant-factory=all,sample",
        "--XXcom.rtg.vcf.eval.maximize=calls-min-base");
      assertTrue(res.err(), res.rc() == 0);
      mergeArgs[i + 1] = new File(output, "sample.vcf.gz").getPath();
    }

    // Merge results into one VCF
    final File merged = new File(dir, "recoded.vcf.gz");
    mergeArgs[0] = "-o";
    mergeArgs[1] = merged.getPath();
    final MainResult res = MainResult.run(new VcfMerge(), mergeArgs);
    assertTrue(res.err(), res.rc() == 0);
    return merged;
  }

  public File accumulate(TestDirectory dir, File template, File samples, File initial) {
    File alleles = initial;
    for (int i = 1; i < 5; i++) {
      final File output = new File(dir, "accum" + i);
      final MainResult res = MainResult.run(new VcfEvalCli(),
        "-o", output.getPath(), "--sample", "SAMPLE" + i,
        "-t", template.getPath(), "-b", alleles.getPath(), "-c", samples.getPath(),
        "--ref-overlap",
        "--XXcom.rtg.vcf.eval.custom-path-processor=alleles",
        "--XXcom.rtg.vcf.eval.custom-variant-factory=all,sample",
        "--XXcom.rtg.vcf.eval.explicit-half-call=false",
        "--XXcom.rtg.vcf.eval.maximize=calls-min-base");
      assertTrue(res.err(), i == 1 || res.rc() == 0);
      alleles = new File(output, "alleles.vcf.gz");
      assertTrue(res.err(), alleles.exists());
    }
    return alleles;
  }
}
