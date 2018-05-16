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

import com.rtg.launcher.MainResult;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.Sex;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfUtils;

import junit.framework.TestCase;

/**
 */
public class DeNovoSampleSimulatorTest extends TestCase {

  @Override
  public void setUp() {
    Diagnostic.setLogStream();
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

  public void testSampleSimulator() throws IOException, InvalidParamsException {
    final File dir = FileUtils.createTempDir("genomemut2_", "test");
    try {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getDNADir(REF, sdf);
      final SequencesReader sr = SequencesReaderFactory.createMemorySequencesReader(sdf, true, LongRange.NONE);
      FileUtils.stringToFile(REFTXT, new File(sdf, ReferenceGenome.REFERENCE_FILE));

      // Generate variants
      final int seed = 10;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 30, new Mutator("X"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File popVcf = new File(dir, "popVcf.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(popVcf, variants, sr, seed);
      //String popVarStr = FileHelper.gzFileToString(popVcf);
      //System.out.println("-- Population Variants --");
      //System.out.println(popVarStr);

      // Generate a couple of samples w.r.t variants
      final SampleSimulator dadsim = new SampleSimulator(sr, new PortableRandom(42), ReferencePloidy.AUTO);
      final File dadVcf = new File(dir, "sample_dad.vcf.gz");
      dadsim.mutateIndividual(popVcf, dadVcf, "dad", Sex.MALE);
      final SampleSimulator momsim = new SampleSimulator(sr, new PortableRandom(43), ReferencePloidy.AUTO);
      final File momVcf = new File(dir, "sample_mom.vcf.gz");
      momsim.mutateIndividual(dadVcf, momVcf, "mom", Sex.FEMALE);


      // Now generate genotypes containing de novo variants
      final File dad2Vcf = new File(dir, "sample_dad2.vcf.gz");
      final MainResult r = MainResult.run(new DeNovoSampleSimulatorCli(), "-t", sdf.getPath(),
        "--seed", "63", "--num-mutations", "20",
        "--original", "dad", "--sample", "dad2",
        "-i", momVcf.getPath(), "-o", dad2Vcf.getPath());
      assertEquals(r.err(), 0, r.rc());

      final File mom2Vcf = new File(dir, "sample_mom2.vcf.gz");
      final File mom2Sdf = new File(dir, "sample_mom2.sdf");
      final MainResult r2 = MainResult.run(new DeNovoSampleSimulatorCli(), "-t", sdf.getPath(),
        "--seed", "64", "--num-mutations", "20",
        "--original", "mom", "--sample", "mom2",
        "-i", dad2Vcf.getPath(), "-o", mom2Vcf.getPath(), "--output-sdf", mom2Sdf.getPath());
      assertEquals(r2.err(), 0, r2.rc());
      assertTrue(mom2Sdf.exists());

      String sampleVcf = FileHelper.gzFileToString(mom2Vcf);
      //System.out.println("-- Final VCF --");
      //System.out.println(sampleVcf);
      sampleVcf = StringUtils.grepMinusV(sampleVcf, "^#");
      sampleVcf = StringUtils.grep(sampleVcf, VcfUtils.FORMAT_DENOVO);
      final String[] sampleVars = TestUtils.splitLines(sampleVcf);
      assertEquals(33, sampleVars.length); // Slightly less than 40 due to sex chromosomes, expect 35 +/- randomness.
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }
}
