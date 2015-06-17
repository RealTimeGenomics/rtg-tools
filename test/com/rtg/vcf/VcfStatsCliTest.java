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
import com.rtg.util.StringUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;

/**
 *
 *
 */
public class VcfStatsCliTest extends AbstractCliTest {


  @Override
  protected AbstractCli getCli() {
    return new VcfStatsCli();
  }

  public void testInitFlags() {
    checkHelp("rtg vcfstats"
            , "Display statistics"
            , "FILE", "input VCF files"
            , "lengths", "output variant length histogram"
            );
  }

  public void testStatsRun() throws IOException {
    try (TestDirectory dir = new TestDirectory("vcfstats")) {
      final File posVcf = new File(dir, "vcfstats.vcf");
      FileHelper.resourceToFile("com/rtg/vcf/resources/vcfstats.vcf", posVcf);

      checkMainInitBadFlags();
      checkMainInitWarn(posVcf.toString(), "--sample", "nosuchsample");
      String output = checkMainInitOk(posVcf.toString());
      mNano.check("vcfstats-run1.txt", StringUtils.grepMinusV(output, "Location"));

      output = checkMainInitOk(posVcf.toString(), "--sample", "sm_son1");
      mNano.check("vcfstats-run2.txt", StringUtils.grepMinusV(output, "Location"));

      output = checkMainInitOk(posVcf.toString(), "--allele-lengths");
      mNano.check("vcfstats-run3.txt", StringUtils.grepMinusV(output, "Location"));
    }

  }

}
