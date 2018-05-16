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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.Sex;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.mendelian.MendeliannessChecker;

/**
 */
public class ChildSampleSimulatorTest extends AbstractCliTest {

  @Override
  protected AbstractCli getCli() {
    return new ChildSampleSimulatorCli();
  }

  private static final String REF =
      ">ref1" + StringUtils.LS
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca" + StringUtils.LS
      + ">ref2" + StringUtils.LS
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca" + StringUtils.LS
      + ">ref3" + StringUtils.LS
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac" + StringUtils.LS
      + ">ref4" + StringUtils.LS
      + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
      + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca"
      + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac" + StringUtils.LS;

  private static final String REFTXT =
      "# Simulated reference" + StringUtils.LS
      + "version 1" + StringUtils.LS
      + "either  def     diploid linear" + StringUtils.LS
      + "# ref1 ~= ChrX, ref2 ~= ChrY" + StringUtils.LS
      + "male    seq     ref1    haploid linear  ref2" + StringUtils.LS
      + "male    seq     ref2    haploid linear  ref1" + StringUtils.LS
      + "female  seq     ref1    diploid linear" + StringUtils.LS
      + "female  seq     ref2    none    linear" + StringUtils.LS
      + "either  seq     ref3    polyploid       circular" + StringUtils.LS;

  public void testSampleSimulator() throws IOException {
    try (final TestDirectory dir = new TestDirectory("childsim")) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getDNADir(REF, sdf);
      final SequencesReader sr = SequencesReaderFactory.createMemorySequencesReader(sdf, true, LongRange.NONE);
      FileUtils.stringToFile(REFTXT, new File(sdf, ReferenceGenome.REFERENCE_FILE));

      // Generate variants
      final int seed = 10;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File popVcf = new File(dir, "popVcf.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(popVcf, variants, sr, seed);
      //String popVarStr = FileHelper.gzFileToString(popVcf);
      //System.out.println("-- Population Variants --");
      //System.out.println(popVarStr);

      // Generate sample w.r.t variants
      final SampleSimulator dadsim = new SampleSimulator(sr, new PortableRandom(15), ReferencePloidy.AUTO);
      final File dadVcf = new File(dir, "sample_dad.vcf.gz");
      dadsim.mutateIndividual(popVcf, dadVcf, "dad", Sex.MALE);

      final SampleSimulator momsim = new SampleSimulator(sr, new PortableRandom(65), ReferencePloidy.AUTO);
      final File momVcf = new File(dir, "sample_mom.vcf.gz");
      momsim.mutateIndividual(dadVcf, momVcf, "mom", Sex.FEMALE);

      // Generate children w.r.t variants
      final ChildSampleSimulator sonsim = new ChildSampleSimulator(sr, new PortableRandom(76), ReferencePloidy.AUTO, 0, false);
      final File sonVcf = new File(dir, "sample_son.vcf.gz");
      sonsim.mutateIndividual(momVcf, sonVcf, "son", Sex.MALE, "dad", "mom");

      // Using CLI for extra mutation testing
      final File daughterVcf = new File(dir, "sample_daughter.vcf.gz");
      final File daughterSdf = new File(dir, "sample_daughter.sdf");
      final MainResult r = MainResult.run(new ChildSampleSimulatorCli(), "-t", sdf.getPath(),
        "-i", sonVcf.getPath(), "-o", daughterVcf.getPath(), "--output-sdf", daughterSdf.getPath(),
        "--seed", "13", "--num-crossovers", "0", "--sex", "female",
        "--mother", "mom", "--father", "dad", "--sample", "daughter");
      assertEquals(r.err(), 0, r.rc());
      assertTrue(daughterSdf.exists());

      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        final MendeliannessChecker chk = new MendeliannessChecker();

        chk.mainInit(new String[] {"-t", sdf.getPath(), "-i", daughterVcf.getPath()}, bos, new PrintStream(bos));
      } finally {
        bos.close();
      }
      final String s = bos.toString().replaceAll("Checking: [^\n]*\n", "Checking: \n");
      TestUtils.containsAll(s,
          "Family: [dad + mom] -> [daughter, son]",
          "(0.00%) records did not conform to expected call ploidy",
          "(0.00%) records contained a violation of Mendelian constraints"
          );

      String sampleVcf = FileHelper.gzFileToString(daughterVcf);
      //System.out.println("-- Including sample foo --");
      //System.out.println(sampleVcf);
      sampleVcf = StringUtils.grepMinusV(sampleVcf, "^#");
      mNano.check("childsim", sampleVcf);
    }
  }
}
