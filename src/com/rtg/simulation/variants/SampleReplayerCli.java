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

import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.LogStream;
import com.rtg.vcf.VcfUtils;

/**
 * Generates a genome SDF corresponding to the sample genotype described in a VCF file.
 */
public class SampleReplayerCli extends LoggedCli {

  private static final String MODULE_NAME = "samplereplay";

  private static final String SAMPLE_VCF = "input";
  private static final String SAMPLE_NAME = "sample";
  private static final String REFERENCE_SDF = "reference";


  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "generate the genome corresponding to a sample genotype";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Generates the genome corresponding to a sample genotype.");
    mFlags.registerExtendedHelp();
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initReferenceTemplate(mFlags, REFERENCE_SDF, true, "");
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, "SDF", "name for output SDF").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('i', SAMPLE_VCF, File.class, "FILE", "input VCF containing the sample genotype").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('s', SAMPLE_NAME, String.class, "STRING", "name of the sample to select from the VCF").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        return CommonFlags.validateOutputDirectory(flags);
      }
    });

  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(OUTPUT_FLAG);
  }

  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    final CFlags flags = mFlags;
    final File input = (File) flags.getValue(REFERENCE_SDF);
    final File sampleVcf = VcfUtils.getZippedVcfFileName(true, (File) flags.getValue(SAMPLE_VCF));
    final String sample = (String) flags.getValue(SAMPLE_NAME);
    try (SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReader(input, true, LongRange.NONE)) {
      final SampleReplayer vr = new SampleReplayer(dsr);
      vr.replaySample(sampleVcf, outputDirectory(), sample);
      return 0;
    }
  }

  /**
   * @param args arguments
   */
  public static void main(String[] args) {
    new SampleReplayerCli().mainExit(args);
  }
}
