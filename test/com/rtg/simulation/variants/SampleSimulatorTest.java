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

import com.rtg.AbstractTest;
import com.rtg.launcher.MainResult;
import com.rtg.mode.DnaUtils;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome;
import com.rtg.util.PortableRandom;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.TestDirectory;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 */
public class SampleSimulatorTest extends AbstractTest {

  private static final String REF = ">ref" + StringUtils.LS
    + "cgtacattac" + "gagcgactag" + "ctagctagta" + "cgtacgtaca"
    + "atggcagcgt" + "attagcggca" + "aattgcgcat" + "tgcgtagcac"
    + "gcgcgattca" + "ttatgcgcgc" + "atcgatcgat" + "cgatcgatca";

  public void testSampleSimulator() throws IOException {
    try (final TestDirectory dir = new TestDirectory("samplesim")) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getDNADir(REF, sdf);
      final SequencesReader sr = SequencesReaderFactory.createMemorySequencesReader(sdf, true, LongRange.NONE);
      final byte[] buffr = new byte[(int) sr.maxLength()];
      final int lenr = sr.read(0, buffr);
      final String sref = DnaUtils.bytesToSequenceIncCG(buffr, 0, lenr);

      // Generate variants
      final int seed = 10;
      final FixedStepPopulationVariantGenerator fixed = new FixedStepPopulationVariantGenerator(sr, 10, new Mutator("X"), new PortableRandom(seed), 0.5);
      final List<PopulationVariantGenerator.PopulationVariant> variants = fixed.generatePopulation();
      final File popVcf = new File(dir, "popVcf.vcf.gz");
      PopulationVariantGenerator.writeAsVcf(popVcf, variants, sr, seed);
      final String popVarStr = FileHelper.gzFileToString(popVcf);
      //System.out.println("-- Population Variants --");
      //System.out.println(popVarStr);
      final String[] popVars = TestUtils.splitLines(StringUtils.grepMinusV(popVarStr, "^#"));
      assertEquals(12, popVars.length);
      for (String line : popVars) {
        assertEquals(8, line.split("\t").length);
      }

      // Generate sample w.r.t variants
      final File vcfOutFile = new File(dir, "sample_foo.vcf.gz");
      final File sdfOutFile = new File(dir, "sample_foo.sdf");
      final MainResult r = MainResult.run(new SampleSimulatorCli(),
        "-t", sdf.getPath(), "-i", popVcf.getPath(), "-o", vcfOutFile.getPath(), "--output-sdf", sdfOutFile.getPath(),
        "--seed", "42", "--sample", "foo"
      );
      assertEquals(r.err(), 0, r.rc());
      assertTrue(sdfOutFile.exists());

      String sampleVcf = FileHelper.gzFileToString(vcfOutFile);
      //System.out.println("-- Including sample foo --");
      //System.out.println(sampleVcf);
      sampleVcf = StringUtils.grepMinusV(sampleVcf, "^#");
      final String[] sampleVars = TestUtils.splitLines(sampleVcf);
      assertEquals(12, sampleVars.length);
      for (String line : sampleVars) {
        final String[] cols = line.split("\t");
        assertEquals(10, cols.length);
        assertTrue(cols[cols.length - 1].contains("|")); // Generated genotypes are phased
      }

      // Generate SDF corresponding to the sample
      final File outsdf = new File(dir, "outsdf");
      final SampleReplayer vr = new SampleReplayer(sr);
      vr.replaySample(vcfOutFile, outsdf, "foo");

      final SequencesReader srOut = SequencesReaderFactory.createMemorySequencesReader(outsdf, true, LongRange.NONE);
      final byte[] buff = new byte[(int) srOut.maxLength()];
      /*
      System.out.println("-- Chromosomes for sample foo --");
      for (int i = 0; i < srOut.numberSequences(); ++i) {
        int len = srOut.read(i, buff);
        System.out.println("seq: " + srOut.name(i));
        System.out.println(DnaUtils.bytesToSequenceIncCG(buff, 0, len));
      }
      */
      assertEquals(2, srOut.numberSequences());
      int len = srOut.read(0, buff);
      final String s1 = DnaUtils.bytesToSequenceIncCG(buff, 0, len);
      len = srOut.read(1, buff);
      final String s2 = DnaUtils.bytesToSequenceIncCG(buff, 0, len);
      assertFalse(sref.equals(s1));
      assertFalse(sref.equals(s2));
      assertFalse(s1.equals(s2));

    }
  }

  public void testGetDistribution() throws IOException {
    try (final TestDirectory dir = new TestDirectory("samplesim")) {
      final File sdf = new File(dir, "sdf");
      ReaderTestUtils.getDNADir(REF, sdf);
      final SequencesReader sr = SequencesReaderFactory.createMemorySequencesReader(sdf, true, LongRange.NONE);
      SampleSimulator ss = new SampleSimulator(sr, new PortableRandom(32), ReferenceGenome.ReferencePloidy.AUTO, false);
      VcfRecord r = new VcfRecord("ref", 10, "a");
      double[] d = ss.getAlleleDistribution(r);
      assertEquals(1, d.length);
      assertEquals(1.0, d[0]);

      r.addAltCall("t");
      d = ss.getAlleleDistribution(r);
      assertEquals(2, d.length);
      assertEquals(0.0, d[0]);
      assertEquals(1.0, d[1]);

      ss = new SampleSimulator(sr, new PortableRandom(32), ReferenceGenome.ReferencePloidy.AUTO, true);
      d = ss.getAlleleDistribution(r);
      assertEquals(0.5, d[0]);
      assertEquals(1.0, d[1]);

      r.addInfo(VcfUtils.INFO_ALLELE_FREQ, "0.2");
      d = ss.getAlleleDistribution(r);
      assertEquals(0.2, d[0]);
      assertEquals(1.0, d[1]);

      r.addAltCall("c").addInfo(VcfUtils.INFO_ALLELE_FREQ, "0.3");
      d = ss.getAlleleDistribution(r);
      assertEquals(3, d.length);
      assertEquals(0.2, d[0]);
      assertEquals(0.5, d[1]);
      assertEquals(1.0, d[2]);
    }
  }
}
