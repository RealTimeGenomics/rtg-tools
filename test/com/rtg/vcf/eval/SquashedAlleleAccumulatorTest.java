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

import com.rtg.launcher.MainResult;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.Utils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class SquashedAlleleAccumulatorTest extends AlleleAccumulatorTest {

  @Override
  public void testAccumulate() throws IOException, UnindexableDataException {
    endToEnd("vcfeval_small_recode"); // Gives same results on this test as diploid accumulation
  }

  @Override
  public File accumulate(TestDirectory dir, File template, File samples, File initial) {
    final String[] commonArgs = {
      "-t", template.getPath(), "--ref-overlap", "--squash-ploidy",
      "--XXcom.rtg.vcf.eval.custom-path-processor=alleles",
      "--XXcom.rtg.vcf.eval.custom-variant-factory=all,sample",
      "--XXcom.rtg.vcf.eval.explicit-unknown-alleles=false",
      "--XXcom.rtg.vcf.eval.maximize=calls-min-base"
    };
    File alleles = initial;
    for (int i = 1; i < 5; ++i) {
      final File output = new File(dir, "accum-a-" + i);
      final MainResult res = MainResult.run(new VcfEvalCli(), Utils.append(commonArgs,
        "-o", output.getPath(), "--sample", "SAMPLE" + i, "-b", alleles.getPath(), "-c", samples.getPath()));
      assertTrue(res.err(), i == 1 || res.rc() == 0);
      alleles = new File(output, "alleles.vcf.gz");
      final File residual = new File(output, "alternate.vcf.gz");

      final File output2 = new File(dir, "accum-b-" + i);
      final MainResult res2 = MainResult.run(new VcfEvalCli(), Utils.append(commonArgs,
        "-o", output2.getPath(), "--sample", "SAMPLE" + i, "-b", alleles.getPath(), "-c", residual.getPath()));
      assertTrue(res2.err(), res2.rc() == 0);
      alleles = new File(output2, "alleles.vcf.gz");
    }
    return alleles;
  }
}
