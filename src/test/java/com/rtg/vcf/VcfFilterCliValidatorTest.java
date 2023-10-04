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

package com.rtg.vcf;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

/**
 */
public class VcfFilterCliValidatorTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new VcfFilterCli();
  }

  public void testFlagValidator() throws IOException {
    try (final TestDirectory main = new TestDirectory("vcffiltercli")) {
      final File foo = new File(main, "foo");
      final File out = new File(main, "out.vcf.gz");
      final File out2 = new File(main, "out.vcf");
      String err = checkHandleFlagsErr("-i", foo.getPath(), "-o", out.getPath());
      TestUtils.containsAllUnwrapped(err, "The --input file ", " does not exist.");

      err = checkHandleFlagsErr("-i", main.getPath(), "-o", out.getPath());
      TestUtils.containsAllUnwrapped(err, main.getPath() + "\" is a directory");

      final File in = new File(main, "input");
      FileUtils.stringToFile(VcfFilterCliTest.INPUT4, in);
      assertTrue(out.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "out").getPath());
      TestUtils.containsAllUnwrapped(err, "The file \"" + out.getPath() + "\" already exists");

      assertTrue(out2.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out2.getPath(), "--no-gzip");
      TestUtils.containsAllUnwrapped(err, "The file \"" + out2.getPath() + "\" already exists");

      assertTrue(out.delete());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--Xmin-posterior-score must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "1.0", "-P", "0.0");
      TestUtils.containsAllUnwrapped(err, "--Xmin-posterior-score cannot be greater than the value for --Xmax-posterior-score");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--max-quality must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-q", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--min-quality must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--max-quality must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-G", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--max-genotype-quality must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-g", "-1.0");
      TestUtils.containsAllUnwrapped(err, "--min-genotype-quality must be at least 0.0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-d", "-1");
      TestUtils.containsAllUnwrapped(err, "--min-read-depth must be at least 0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-D", "-1");
      TestUtils.containsAllUnwrapped(err, "--max-read-depth must be at least 0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-c", "-1");
      TestUtils.containsAllUnwrapped(err, "--min-combined-read-depth must be at least 0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-C", "-1");
      TestUtils.containsAllUnwrapped(err, "--max-combined-read-depth must be at least 0");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "-1");
      TestUtils.containsAllUnwrapped(err, "--max-ambiguity-ratio must be in the range [0.0, 1.0]");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "1.1");
      TestUtils.containsAllUnwrapped(err, "--max-ambiguity-ratio must be in the range [0.0, 1.0]");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--density-window", "-1");
      TestUtils.containsAllUnwrapped(err, "--density-window must be at least 1");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-genotype-quality", "0", "--Xmax-posterior-score", "10");
      TestUtils.containsAllUnwrapped(err, "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--Xmin-posterior-score", "0", "--max-genotype-quality", "10");
      TestUtils.containsAllUnwrapped(err, "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-denovo-score", "10", "--sample", "alice", "--sample", "bob");
      TestUtils.containsAllUnwrapped(err, "De Novo filtering requires a single sample to be specified");

      final File bedFile = new File(main, "foo.bed");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-bed", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "The --include-bed file \"" + bedFile + "\" does not exist");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "The --exclude-bed file \"" + bedFile + "\" does not exist");

      assertTrue(bedFile.mkdir());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "The --include-vcf file \"" + bedFile + "\" is a directory");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "The --exclude-vcf file \"" + bedFile + "\" is a directory");

      assertTrue(bedFile.delete() && bedFile.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString(), "--include-bed", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "Cannot set both --include-bed and --include-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAllUnwrapped(err, "Cannot set both --exclude-bed and --exclude-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--snps-only", "--non-snps-only");
      TestUtils.containsAllUnwrapped(err, "Cannot set both --snps-only and --non-snps-only");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--fail", "BLAH", "--clear-failed-samples");
      TestUtils.containsAllUnwrapped(err, "Cannot set both --fail and --clear-failed-samples");

    }
  }
}
