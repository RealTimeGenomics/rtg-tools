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
      final File out = new File(main, "out.gz");
      final File out2 = new File(main, "out.txt");
      String err = checkHandleFlagsErr("-i", foo.getPath(), "-o", out.getPath());
      TestUtils.containsAll(TestUtils.unwrap(err), "Given file ", " does not exist.");

      err = checkHandleFlagsErr("-i", main.getPath(), "-o", out.getPath());
      TestUtils.containsAll(TestUtils.unwrap(err), main.getPath() + "\" is a directory");

      final File in = new File(main, "input");
      FileUtils.stringToFile(VcfFilterCliTest.INPUT4, in);
      assertTrue(out.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", new File(main, "out").getPath());
      TestUtils.containsAll(TestUtils.unwrap(err), "The file \"" + out.getPath() + "\" already exists");

      assertTrue(out2.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out2.getPath(), "--no-gzip");
      TestUtils.containsAll(TestUtils.unwrap(err), "The file \"" + out2.getPath() + "\" already exists");

      assertTrue(out.delete());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--Xmin-posterior-score\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-p", "1.0", "-P", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--Xmax-posterior-score\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-q", "1.0", "-Q", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-Q", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-G", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-g", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-g", "1.0", "-G", "0.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + 0.0 + "\". It should be greater than or equal to " + 1.0 + " and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-G", "-1.0");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-genotype-quality\" has invalid value \"" + -1.0 + "\". It should be greater than or equal to 0.0 and less than Infinity.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-d", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-D", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-c", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--min-combined-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-C", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-combined-read-depth\" has invalid value \"-1\". It should be greater than or equal to 0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-ambiguity-ratio\" has invalid value \"-1.0\". It should be greater than or equal to 0.0 and less than or equal to 1.0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "-A", "1.1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--max-ambiguity-ratio\" has invalid value \"1.1\". It should be greater than or equal to 0.0 and less than or equal to 1.0.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--density-window", "-1");
      TestUtils.containsAll(TestUtils.unwrap(err), "The specified flag \"--density-window\" has invalid value \"-1\". It should be greater than or equal to \"1\".");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-genotype-quality",  "0" , "--Xmax-posterior-score", "10");
      TestUtils.containsAll(TestUtils.unwrap(err), "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--Xmin-posterior-score",  "0" , "--max-genotype-quality", "10");
      TestUtils.containsAll(TestUtils.unwrap(err), "Use genotype-quality or posterior filters, not both.");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--min-denovo-score",  "10", "--sample", "alice", "--sample", "bob");
      TestUtils.containsAll(TestUtils.unwrap(err), "De Novo filtering requires a single sample to be specified");

      final File bedFile = new File(main, "foo.bed");
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--include-bed\" file: \"" + bedFile + "\" doesn't exist");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--exclude-bed\" file: \"" + bedFile + "\" doesn't exist");

      assertTrue(bedFile.mkdir());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--include-vcf\" file: \"" + bedFile + "\" is a directory");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "The \"--exclude-vcf\" file: \"" + bedFile + "\" is a directory");

      assertTrue(bedFile.delete() && bedFile.createNewFile());
      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--include-vcf", bedFile.toString(), "--include-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --include-bed or --include-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--exclude-vcf", bedFile.toString(), "--exclude-bed", bedFile.toString());
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --exclude-bed or --exclude-vcf");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--snps-only", "--non-snps-only");
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --snps-only or --non-snps-only");

      err = checkHandleFlagsErr("-i", in.getPath(), "-o", out.getPath(), "--fail", "BLAH", "--clear-failed-samples");
      TestUtils.containsAll(TestUtils.unwrap(err), "Only one of --fail or --clear-failed-samples");

    }
  }
}
