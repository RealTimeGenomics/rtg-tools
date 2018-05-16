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

import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.intervals.LongRange;
import com.rtg.vcf.VcfUtils;

/**
 * Command line wrapper for fixed step variant generator
 */
public class FixedStepPopulationVariantGeneratorCli extends AbstractCli {

  private static final String MODULE_NAME = "fixedstepsnpsim";
  private static final String OUTPUT_VCF = "output";
  private static final String REFERENCE_SDF = "reference";
  private static final String DISTANCE = "distance";
  private static final String SEED = "seed";
  private static final String SNP_SPECIFICATION = "spec";
  private static final String FREQUENCY = "allele-frequency";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  /**
   * Set up a flags object for this module
   * @param flags the flags to set up
   */
  public void initFlags(CFlags flags) {
    CommonFlagCategories.setCategories(flags);
    flags.registerRequired('i', REFERENCE_SDF, File.class, CommonFlags.SDF, "SDF containing input genome").setCategory(INPUT_OUTPUT);
    flags.registerRequired('o', OUTPUT_VCF, File.class, CommonFlags.FILE, "name for population output VCF").setCategory(INPUT_OUTPUT);
    flags.registerRequired(SNP_SPECIFICATION, String.class, CommonFlags.STRING, "generated mutation format").setCategory(INPUT_OUTPUT);
    flags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(UTILITY);
    flags.registerRequired('d', DISTANCE, Integer.class, CommonFlags.INT, "distance between mutations").setCategory(INPUT_OUTPUT);
    flags.registerOptional('a', FREQUENCY, Double.class, CommonFlags.FLOAT, "allele frequency", 0.5).setCategory(UTILITY);
    flags.setValidator(flags1 -> {
      final Double af = (Double) flags1.getValue(FREQUENCY);
      return af != null && af >= 0 && af <= 1 && !af.isNaN();
    });
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
    final long seed = random.getSeed();
    final int distance = (Integer) flags.getValue(DISTANCE);
    final File input = (File) flags.getValue(REFERENCE_SDF);
    final Mutator mutator = new Mutator((String) flags.getValue(SNP_SPECIFICATION));
    final File outputVcf = VcfUtils.getZippedVcfFileName(true, (File) flags.getValue(OUTPUT_VCF));
    final double af = (Double) flags.getValue(FREQUENCY);
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReader(input, true, LongRange.NONE)) {
      final FixedStepPopulationVariantGenerator fs = new FixedStepPopulationVariantGenerator(dsr, distance, mutator, random, af);
      PopulationVariantGenerator.writeAsVcf(outputVcf, fs.generatePopulation(), dsr, seed);
      return 0;
    }
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    new FixedStepPopulationVariantGeneratorCli().mainExit(args);
  }

}
