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
 * Generate the genotypes for a new sample which is the child of two existing samples.
 *
 */
@TestClass(value = {"com.rtg.simulation.variants.ChildSampleSimulatorCliTest", "com.rtg.simulation.variants.ChildSampleSimulatorTest"})
public class ChildSampleSimulatorCli extends AbstractCli {

  private static final String MODULE_NAME = "childsim";

  private static final String INPUT_VCF = "input";
  private static final String SAMPLE_FLAG = "sample";
  private static final String FATHER_FLAG = "father";
  private static final String MOTHER_FLAG = "mother";
  private static final String SEX = "sex";
  private static final String OUTPUT_VCF = "output";
  private static final String OUTPUT_SDF = "output-sdf";
  private static final String EXTRA_CROSSOVERS = "num-crossovers";
  private static final String SHOW_CROSSOVERS = "show-crossovers";
  private static final String REFERENCE_SDF = "reference";
  private static final String SEED = "seed";
  private static final String PLOIDY = "ploidy";

  // We require one crossover, but there is a small probability of an extra crossover.
  private static final double EXTRA_CROSSOVERS_PER_CHROMOSOME = 0.01;


  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate a VCF containing a genotype simulated as a child of two parents";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates a VCF containing a genotype simulated as a child of two parents.");
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initForce(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    mFlags.registerRequired('o', OUTPUT_VCF, File.class, CommonFlags.FILE, "output VCF file name").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_SDF, File.class, CommonFlags.SDF, "if set, output an SDF containing the sample genome").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('i', INPUT_VCF, File.class, CommonFlags.FILE, "input VCF containing parent variants").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('s', SAMPLE_FLAG, String.class, CommonFlags.STRING, "name for new child sample").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired(FATHER_FLAG, String.class, CommonFlags.STRING, "name of the existing sample to use as the father").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired(MOTHER_FLAG, String.class, CommonFlags.STRING, "name of the existing sample to use as the mother").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(SEX, Sex.class, "SEX", "sex of individual", Sex.EITHER).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(PLOIDY, ReferencePloidy.class, CommonFlags.STRING, "ploidy to use", ReferencePloidy.AUTO).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(EXTRA_CROSSOVERS, Double.class, CommonFlags.FLOAT, "likelihood of extra crossovers per chromosome", EXTRA_CROSSOVERS_PER_CHROMOSOME).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SHOW_CROSSOVERS, "if set, display information regarding haplotype selection and crossover points").setCategory(CommonFlagCategories.UTILITY);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(flags -> CommonFlags.validateSDF(flags, REFERENCE_SDF)
      && CommonFlags.validateTabixedInputFile(flags, INPUT_VCF)
      && flags.checkNand(OUTPUT_SDF, NO_GZIP)
      && (!flags.isSet(OUTPUT_SDF) || CommonFlags.validateOutputDirectory(flags, OUTPUT_SDF))
      && CommonFlags.validateNotStdout((File) flags.getValue(OUTPUT_VCF))
      && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_VCF)))
      && flags.checkInRange(EXTRA_CROSSOVERS, 0.0, 1.0));
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
    final File popVcf = (File) flags.getValue(INPUT_VCF);
    final File outputVcf = VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_VCF));
    final String sample = (String) flags.getValue(SAMPLE_FLAG);
    final String father = (String) flags.getValue(FATHER_FLAG);
    final String mother = (String) flags.getValue(MOTHER_FLAG);
    final Sex sex = (Sex) flags.getValue(SEX);
    final ReferencePloidy ploidy = (ReferencePloidy) flags.getValue(PLOIDY);
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(reference, true, false, LongRange.NONE)) {
      final ChildSampleSimulator ss = new ChildSampleSimulator(dsr, random, ploidy, (Double) flags.getValue(EXTRA_CROSSOVERS), flags.isSet(SHOW_CROSSOVERS));
      ss.mutateIndividual(popVcf, outputVcf, sample, sex, father, mother);
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
    new ChildSampleSimulatorCli().mainExit(args);
  }
}
