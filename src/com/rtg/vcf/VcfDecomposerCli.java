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

package com.rtg.vcf;

import static com.rtg.launcher.CommonFlags.FILE;
import static com.rtg.launcher.CommonFlags.INPUT_FLAG;
import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.launcher.CommonFlags.OUTPUT_FLAG;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.SENSITIVITY_TUNING;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.reader.SdfId;
import com.rtg.reader.SdfUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Decompose complex calls within VCF records into simpler forms.
 */
public final class VcfDecomposerCli extends AbstractCli {

  private static final String BREAK_MNPS = "break-mnps";

  @Override
  public String moduleName() {
    return "vcfdecompose";
  }

  @Override
  public String description() {
    return "decompose complex variants within a VCF file";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Decomposes complex variants within a VCF file into smaller components.");
    CommonFlagCategories.setCategories(mFlags);
    mFlags.registerRequired('i', INPUT_FLAG, File.class, FILE, "VCF file containing variants to decompose. Use '-' to read from standard input").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, FILE, "output VCF file name. Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional('t', CommonFlags.TEMPLATE_FLAG, File.class, CommonFlags.SDF, "SDF of the reference genome the variants are called against").setCategory(INPUT_OUTPUT);
    mFlags.registerOptional(BREAK_MNPS, "if set, break MNPs into individual SNPs").setCategory(SENSITIVITY_TUNING);
    mFlags.registerOptional(CommonFlags.NO_HEADER, "prevent VCF header from being written").setCategory(UTILITY);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    CommonFlags.initForce(mFlags);
    mFlags.setValidator(flags -> CommonFlags.validateInputFile(flags, INPUT_FLAG)
      && CommonFlags.validateOutputFile(flags, VcfUtils.getZippedVcfFileName(!flags.isSet(NO_GZIP), (File) flags.getValue(OUTPUT_FLAG))));
  }

  private static void checkHeader(final VcfHeader header, final SdfId referenceSdfId) {
    final SdfId vcfSdfId = header.getSdfId();
    if (!vcfSdfId.check(referenceSdfId)) {
      Diagnostic.warning("Reference template ID mismatch, VCF variants were not created from the given reference");
    }
  }

  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final File inputFile = (File) mFlags.getValue(INPUT_FLAG);
    final File output = (File) mFlags.getValue(OUTPUT_FLAG);
    final boolean gzip = !mFlags.isSet(NO_GZIP);
    final boolean stdout = FileUtils.isStdio(output);
    try (final SequencesReader templateSequences = getReference()) {
      try (VcfIterator reader = VcfReader.openVcfReader(inputFile)) {
        final VcfHeader header = reader.getHeader();
        if (templateSequences != null) {
          SdfUtils.validateNoDuplicates(templateSequences, false);
          checkHeader(header, templateSequences.getSdfId());
        }
        final File vcfFile = stdout ? null : VcfUtils.getZippedVcfFileName(gzip, output);
        try (DecomposingVcfWriter writer = new DecomposingVcfWriter(new VcfWriterFactory(mFlags).addRunInfo(true).make(header, vcfFile, out), templateSequences, mFlags.isSet(BREAK_MNPS))) {
          while (reader.hasNext()) {
            writer.write(reader.next());
          }
          if (!stdout) {
            writer.printStatistics(out);
          }
        }
      }
    }
    return 0;
  }

  protected SequencesReader getReference() throws IOException {
    if (mFlags.isSet(CommonFlags.TEMPLATE_FLAG)) {
      final File templateFile = (File) mFlags.getValue(CommonFlags.TEMPLATE_FLAG);
      SdfUtils.validateHasNames(templateFile);
      return SequencesReaderFactory.createDefaultSequencesReader(templateFile, LongRange.NONE);
    } else {
      return null;
    }
  }
}
