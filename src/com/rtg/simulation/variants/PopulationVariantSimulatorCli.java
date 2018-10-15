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
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.intervals.LongRange;
import com.rtg.variant.GenomePriorParams;
import com.rtg.vcf.VcfUtils;

/**
 * Command line wrapper for prior-based population variant creation
 */
public class PopulationVariantSimulatorCli extends AbstractCli {

  private static final String OUTPUT_VCF = "output";
  private static final String REFERENCE_SDF = "reference";
  private static final String SEED = "seed";
  private static final String PRIORS_FLAG = "Xpriors";
  private static final String BIAS_FLAG = "Xbias";
  private static final String RATE_FLAG = "Xrate";

  @Override
  public String moduleName() {
    return "popsim";
  }

  @Override
  public String description() {
    return "generate a VCF containing simulated population variants";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates a VCF containing simulated population variants.");
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initForce(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    mFlags.registerRequired('o', OUTPUT_VCF, File.class, CommonFlags.FILE, "output VCF file name").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('p', PRIORS_FLAG, String.class, CommonFlags.STRING, "selects a properties file specifying the priors. Either a file name or one of [human]", "human").setCategory(UTILITY);
    mFlags.registerOptional(BIAS_FLAG, Double.class, CommonFlags.FLOAT, "bias frequency of variants towards alt alleles.", 0.0).setCategory(UTILITY);
    mFlags.registerOptional(RATE_FLAG, Double.class, CommonFlags.FLOAT, "per base rate of variant generation (overrides that loaded from priors).").setCategory(UTILITY);
    mFlags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(UTILITY);
    CommonFlags.initNoGzip(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateSDF(flags, REFERENCE_SDF)
      && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_VCF)))
    );
  }

  @Override
  protected int mainExec(OutputStream output, PrintStream err) throws IOException {
    final CFlags flags = mFlags;
    final PortableRandom random;
    if (flags.isSet(SEED)) {
      random = new PortableRandom((Integer) flags.getValue(SEED));
    } else {
      random = new PortableRandom();
    }
    final long seed = random.getSeed();
    final PopulationMutatorPriors priors;
    try {
      priors = new PopulationMutatorPriors(GenomePriorParams.builder().genomePriors((String) mFlags.getValue(PRIORS_FLAG)).create());
    } catch (final InvalidParamsException e) {
      return 1;
    }
    final File reference = (File) flags.getValue(REFERENCE_SDF);
    final File out = (File) flags.getValue(OUTPUT_VCF);
    final boolean gzip = !flags.isSet(NO_GZIP);
    final File vcfFile = VcfUtils.getZippedVcfFileName(gzip, out);
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(reference, true, false, LongRange.NONE)) {
      final int targetVariants;
      if (flags.isSet(RATE_FLAG)) {
        targetVariants = (int) (dsr.totalLength() * (Double) flags.getValue(RATE_FLAG));
      } else {
        targetVariants = (int) (dsr.totalLength() * priors.rate());
      }
      final PriorPopulationVariantGenerator fs = new PriorPopulationVariantGenerator(dsr, priors, random, (Double) flags.getValue(BIAS_FLAG), targetVariants);
      PopulationVariantGenerator.writeAsVcf(vcfFile, fs.generatePopulation(), dsr, seed);
    }
    return 0;
  }
}
