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
import com.rtg.util.InvalidParamsException;
import com.rtg.util.PortableRandom;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.intervals.LongRange;
import com.rtg.variant.GenomePriorParams;
import com.rtg.vcf.VcfUtils;

/**
 * Generate a derived genotype that contains additional de novo variants.
 *
 */
@TestClass(value = {"com.rtg.simulation.variants.DeNovoSampleSimulatorCliTest", "com.rtg.simulation.variants.DeNovoSampleSimulatorTest"})
public class DeNovoSampleSimulatorCli extends AbstractCli {

  private static final String MODULE_NAME = "denovosim";

  private static final String INPUT_VCF = "input";
  private static final String SAMPLE_FLAG = "sample";
  private static final String ORIGINAL_FLAG = "original";
  private static final String OUTPUT_VCF = "output";
  private static final String OUTPUT_SDF = "output-sdf";
  private static final String EXPECTED_MUTATIONS = "num-mutations";
  private static final String SHOW_MUTATIONS = "show-mutations";
  private static final String REFERENCE_SDF = "reference";
  private static final String SEED = "seed";
  private static final String PRIORS_FLAG = "Xpriors";
  private static final String PLOIDY = "ploidy";

  // We inject one mutation per chromosome, but there is a small probability of an extra mutation.
  private static final int DEFAULT_MUTATIONS_PER_GENOME = 70;


  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate a VCF containing a derived genotype containing de novo variants";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates a VCF containing a derived genotype containing de novo variants.");
    mFlags.registerExtendedHelp();
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    mFlags.registerRequired('o', OUTPUT_VCF, File.class, CommonFlags.FILE, "output VCF file name").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(OUTPUT_SDF, File.class, CommonFlags.SDF, "if set, output genome SDF name").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('i', INPUT_VCF, File.class, CommonFlags.FILE, "input VCF containing genotype of original sample").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('s', SAMPLE_FLAG, String.class, CommonFlags.STRING, "name for new derived sample").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional('p', PRIORS_FLAG, String.class, CommonFlags.STRING, "selects a properties file specifying the mutation priors. Either a file name or one of [human]", "human").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerRequired(ORIGINAL_FLAG, String.class, CommonFlags.STRING, "name of the existing sample to use as the original genotype").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerOptional(PLOIDY, ReferencePloidy.class, CommonFlags.STRING, "ploidy to use", ReferencePloidy.AUTO).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(EXPECTED_MUTATIONS, Integer.class, CommonFlags.INT, "expected number of mutations per genome", DEFAULT_MUTATIONS_PER_GENOME).setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SEED, Integer.class, CommonFlags.INT, "seed for the random number generator").setCategory(CommonFlagCategories.UTILITY);
    mFlags.registerOptional(SHOW_MUTATIONS, "if set, display information regarding de novo mutation points").setCategory(CommonFlagCategories.UTILITY);
    CommonFlags.initNoGzip(mFlags);

    mFlags.setValidator(new DeNovoSampleSimulatorFlagValidator());
  }

  private static class DeNovoSampleSimulatorFlagValidator implements Validator {

    @Override
    public boolean isValid(final CFlags cflags) {
      return cflags.checkNand(OUTPUT_SDF, CommonFlags.NO_GZIP)
        && CommonFlags.validateNotStdout((File) cflags.getValue(OUTPUT_VCF))
        && (!cflags.isSet(OUTPUT_SDF) || CommonFlags.validateOutputDirectory(cflags, OUTPUT_SDF));
    }
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
    final File outputVcf = VcfUtils.getZippedVcfFileName(!flags.isSet(CommonFlags.NO_GZIP), (File) flags.getValue(OUTPUT_VCF));
    final String sample = (String) flags.getValue(SAMPLE_FLAG);
    final String original = (String) flags.getValue(ORIGINAL_FLAG);
    final ReferencePloidy ploidy = (ReferencePloidy) flags.getValue(PLOIDY);
    final GenomePriorParams priors;
    try {
      priors = GenomePriorParams.builder().genomePriors((String) mFlags.getValue(PRIORS_FLAG)).create();
    } catch (final InvalidParamsException e) {
      return 1;
    }
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(reference, true, false, LongRange.NONE)) {
      final DeNovoSampleSimulator ss = new DeNovoSampleSimulator(dsr, priors, random, ploidy, (Integer) flags.getValue(EXPECTED_MUTATIONS), flags.isSet(SHOW_MUTATIONS));
      ss.mutateIndividual(popVcf, outputVcf, original, sample);
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
    new DeNovoSampleSimulatorCli().mainExit(args);
  }
}
