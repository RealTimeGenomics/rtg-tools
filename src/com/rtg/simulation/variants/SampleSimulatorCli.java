/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

import static com.rtg.launcher.CommonFlags.NO_GZIP;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.reference.ReferenceGenome.ReferencePloidy;
import com.rtg.reference.Sex;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.intervals.LongRange;
import com.rtg.vcf.VcfUtils;

/**
 * Randomly generate the genotypes for a new sample, given a set of population variants
 * with allele frequencies.
 *
 */
@TestClass(value = {"com.rtg.simulation.variants.SampleSimulatorCliTest", "com.rtg.simulation.variants.SampleSimulatorTest"})
public class SampleSimulatorCli extends AbstractCli {

  private static final String MODULE_NAME = "samplesim";

  private static final String POPULATION_VCF = "input";
  private static final String SAMPLE_NAME = "sample";
  private static final String SEX = "sex";
  private static final String OUTPUT_VCF = "output";
  private static final String OUTPUT_SDF = "output-sdf";
  private static final String REFERENCE_SDF = "reference";
  private static final String SEED = "seed";
  private static final String PLOIDY = "ploidy";


  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate a VCF containing a genotype simulated from a population";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates a VCF containing a genotype simulated from population variants according to allele frequency.");
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initForce(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    mFlags.registerRequired('o', OUTPUT_VCF, File.class, CommonFlags.FILE, "output VCF file name").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_SDF, File.class, CommonFlags.SDF, "if set, output an SDF containing the sample genome").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('i', POPULATION_VCF, File.class, CommonFlags.FILE, "input VCF containing population variants").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('s', SAMPLE_NAME, String.class, CommonFlags.STRING, "name for sample").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(SEX, Sex.class, "SEX", "sex of individual", Sex.EITHER).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(PLOIDY, ReferencePloidy.class, CommonFlags.STRING, "ploidy to use", ReferencePloidy.AUTO).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(CommonFlagCategories.UTILITY);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(flags -> CommonFlags.validateSDF(flags, REFERENCE_SDF)
      && CommonFlags.validateTabixedInputFile(flags, POPULATION_VCF)
      && flags.checkNand(OUTPUT_SDF, NO_GZIP)
      && (!flags.isSet(OUTPUT_SDF) || CommonFlags.validateOutputDirectory(flags, OUTPUT_SDF))
      && CommonFlags.validateNotStdout((File) flags.getValue(OUTPUT_VCF))
      && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_VCF)))
    );
  }


  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final CFlags flags = mFlags;
    final PortableRandom random;
    if (flags.isSet(SEED)) {
      random = new PortableRandom((Integer) flags.getValue(SEED));
    } else {
      random = new PortableRandom();
    }

    final File reference = (File) flags.getValue(REFERENCE_SDF);
    final File popVcf = (File) flags.getValue(POPULATION_VCF);
    final File outputVcf = VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_VCF));
    final String sample = (String) flags.getValue(SAMPLE_NAME);
    final Sex sex = (Sex) flags.getValue(SEX);
    final ReferencePloidy ploidy = (ReferencePloidy) flags.getValue(PLOIDY);
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(reference, true, false, LongRange.NONE)) {
      final SampleSimulator ss = new SampleSimulator(dsr, random, ploidy);
      ss.mutateIndividual(popVcf, outputVcf, sample, sex);
      ss.printStatistics(out);
      if (flags.isSet(OUTPUT_SDF)) {
        final SampleReplayer vr = new SampleReplayer(dsr);
        vr.replaySample(outputVcf, (File) flags.getValue(OUTPUT_SDF), sample);
      }
    }
    return 0;
  }

  /**
   * @param args arguments
   */
  public static void main(String[] args) {
    new SampleSimulatorCli().mainExit(args);
  }
}
