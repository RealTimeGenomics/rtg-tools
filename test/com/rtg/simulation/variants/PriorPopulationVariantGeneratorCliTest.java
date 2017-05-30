/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.RandomDna;

/**
 * Test the corresponding class
 */
public class PriorPopulationVariantGeneratorCliTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new PriorPopulationVariantGeneratorCli();
  }

  public void testInitParams() {
    checkHelp("reference", "reference genome",
        "-o", "output=", "output VCF",
        "print help on command-line flag usage",
        "seed=", "seed for the random number generator");

    checkExtendedHelp("bias frequency",
        "properties file specifying the priors");
  }

  public void testSimple() throws IOException {
    try (TestDirectory dir = new TestDirectory("popsim")) {
      final File template = ReaderTestUtils.getDNASubDir(">s1\n" + RandomDna.random(4000, 43) + "\n>s2\n" + RandomDna.random(5000, 54) + "\n>s3\nacgt\n>s4\nacgt\n", dir);
      final File vcfout = new File(dir, "popsim.vcf.gz");
      final MainResult r = MainResult.run(getCli(), "-t", template.getPath(), "--seed", "42", "--output", vcfout.getPath());
      assertEquals(r.err(), 0, r.rc());
      mNano.check("popsim-simple.vcf", TestUtils.sanitizeVcfHeader(FileHelper.gzFileToString(vcfout)));
    }
  }
}
