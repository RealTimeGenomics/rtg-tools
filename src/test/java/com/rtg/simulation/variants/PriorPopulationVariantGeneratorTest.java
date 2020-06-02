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
import java.util.List;

import com.rtg.launcher.AbstractNanoTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.variant.GenomePriorParams;

/**
 */
public class PriorPopulationVariantGeneratorTest extends AbstractNanoTest {

  private static final String REF = ">ref" + StringUtils.LS
          + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
          + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
          + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
          + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
          + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca";

  public void testVariantGenerator() throws IOException, InvalidParamsException {
    final File dir = FileUtils.createTempDir("genomemut2_", "test");
    try {
      final SequencesReader sr = ReaderTestUtils.getReaderDnaMemory(REF);

      // Make some priors that will let things happen more often
      final GenomePriorParams priors = GenomePriorParams.builder()
      .genomeSnpRateHetero(0.05).genomeSnpRateHomo(0.05)           // The priors probably should have just one rather than splitting
      .genomeMnpBaseRateHetero(0.05).genomeMnpBaseRateHomo(0.05)   // The priors probably should have just one rather than splitting
      .genomeIndelEventRate(0.05)
      .create();

      // Generate variants
      final int seed = 10;
      final PriorPopulationVariantGenerator gen = new PriorPopulationVariantGenerator(sr, new PopulationMutatorPriors(priors), new PortableRandom(seed), 1);
      final List<PopulationVariantGenerator.PopulationVariant> variants = gen.generatePopulation();
      final File popVcf = new File(dir, "popVcf.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(popVcf, variants, sr, seed);
      final String popVarStr = FileHelper.gzFileToString(popVcf);
      //System.out.println("-- Population Variants --");
      //System.out.println(popVarStr);
      final String vars = StringUtils.grepMinusV(popVarStr, "^#");
      mNano.check("popvars", vars);
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
