/*
 * Copyright (c) 2014. Real Time Genomics Limited.
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
package com.rtg.reader;

import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.sam.SamCommandHelper;
import com.rtg.util.Constants;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.LogStream;
import com.rtg.util.machine.MachineType;

import htsjdk.samtools.SAMReadGroupRecord;

/**
 * Perform the prereading of CG sequence data, into a format that is understood
 * by RTG.
 */
public final class Cg2Sdf extends LoggedCli {

  static final String MODULE_NAME = "cg2sdf";
  static final String MAXIMUM_NS = "max-unknowns";
  static final String NO_QUALITY = "no-quality";

  static final String COMPRESS_FLAG = "Xcompress";
  private static final String KEEP_NAMES = "Xkeep-names";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert Complete Genomics reads to SDF";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Converts Complete Genomics sequencing system reads to RTG SDF format.");
    CommonFlagCategories.setCategories(mFlags);

    final Flag<File> inFlag = mFlags.registerRequired(File.class, CommonFlags.FILE, "file in Complete Genomics TSV format");
    inFlag.setMinCount(0);
    inFlag.setMaxCount(Integer.MAX_VALUE);
    inFlag.setCategory(INPUT_OUTPUT);
    final Flag<File> list = mFlags.registerOptional('I', CommonFlags.INPUT_LIST_FLAG, File.class, CommonFlags.FILE, "file containing a list of Complete Genomics TSV files (1 per line)").setCategory(INPUT_OUTPUT);
    mFlags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, CommonFlags.SDF, "name of output SDF").setCategory(INPUT_OUTPUT);

    mFlags.registerOptional(MAXIMUM_NS, Integer.class, CommonFlags.INT, "maximum number of Ns allowed in either side for a read", 5).setCategory(FILTERING);
    mFlags.registerOptional(NO_QUALITY, "does not include quality data in the resulting SDF").setCategory(UTILITY);
    mFlags.registerOptional(COMPRESS_FLAG, Boolean.class, "BOOL", "compress sdf", Boolean.TRUE).setCategory(UTILITY);
    mFlags.registerOptional(KEEP_NAMES, "add name data to the resulting SDF").setCategory(UTILITY);
    SamCommandHelper.initSamRg(mFlags, "COMPLETE", UTILITY);
    mFlags.addRequiredSet(inFlag);
    mFlags.addRequiredSet(list);
    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && CommonFlags.checkFileList(flags, CommonFlags.INPUT_LIST_FLAG, null, Integer.MAX_VALUE)
      && flags.checkInRange(MAXIMUM_NS, 0, Integer.MAX_VALUE)
      && SamCommandHelper.validateSamRg(flags));
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }

  static String getBaseInputPath(final File inputFile) {
    String name = inputFile.getPath();
    final int lastDot = name.lastIndexOf('.');
    if (lastDot > -1) {
      name = name.substring(0, lastDot);
    }
    return name;
  }

  private static void performPreread(final Collection<File> inputFiles, final File output, final Integer maximumNs, boolean useQuality, PrintStream ps, boolean compress, boolean includeNames, SAMReadGroupRecord samReadGroupRecord) throws IOException {

    final SdfId sdfId = new SdfId();

    final List<CgSequenceDataSource> sources = new ArrayList<>(inputFiles.size());
    final List<String> names = new ArrayList<>(inputFiles.size());

    for (final File f : inputFiles) {
      final String path = getBaseInputPath(f);
      sources.add(new TsvSequenceDataSource(f, maximumNs));
      names.add(path);
    }

    int skippedNReads = 0;
    long skippedResidues = 0;
    try (ConcatSequenceDataSource<CgSequenceDataSource> dsl = new ConcatSequenceDataSource<>(sources, names)) {
      final AlternatingSequencesWriter writer = new AlternatingSequencesWriter(dsl, output, Constants.MAX_FILE_SIZE, null, PrereadType.CG, compress);
      writer.setSdfId(sdfId);
      writer.setReadGroup(samReadGroupRecord == null ? null : samReadGroupRecord.getSAMString());

      // perform the actual work
      writer.processSequences(useQuality, includeNames);
      long minInputLength = Long.MAX_VALUE;
      long maxInputLength = 0;
      for (final CgSequenceDataSource s : sources) {
        skippedResidues += s.getSkippedResidues();
        skippedNReads += s.getSkippedReads();
        if (s.getMaxLength() > maxInputLength) {
          maxInputLength = s.getMaxLength();
        }
        if (s.getMinLength() < minInputLength) {
          minInputLength = s.getMinLength();
        }
      }
      if (minInputLength != maxInputLength) {
        Diagnostic.warning("Complete Genomics reads should be fixed length, but variable length reads were encountered");
      } else if (samReadGroupRecord != null && samReadGroupRecord.getPlatform() != null) {
        final String pl = samReadGroupRecord.getPlatform();
        if (minInputLength == CgUtils.CG_RAW_READ_LENGTH && !MachineType.COMPLETE_GENOMICS.compatiblePlatform(pl)) {
          Diagnostic.warning("For Complete Genomics v1 read structure the SAM read group platform should be set to: " + MachineType.COMPLETE_GENOMICS.platform() + ", not " + pl);
        } else if (minInputLength == CgUtils.CG2_RAW_READ_LENGTH && !MachineType.COMPLETE_GENOMICS_2.compatiblePlatform(pl)) {
          Diagnostic.warning("For Complete Genomics v2 read structure the SAM read group platform should be set to: " + MachineType.COMPLETE_GENOMICS_2.platform() + ", not " + pl);
        }
      }

      final long inResidues = writer.getTotalLength() + skippedResidues;
      final long inSeqs = writer.getNumberOfSequences() + skippedNReads * 2;
      final FormatCli.PrereadExecutor.Counts inputCounts = new FormatCli.PrereadExecutor.Counts(inSeqs, inResidues, maxInputLength, minInputLength);
      final FormatCli.PrereadExecutor.Counts outputCounts = new FormatCli.PrereadExecutor.Counts(writer.getNumberOfSequences(), writer.getTotalLength(), writer.getMaxLength(), writer.getMinLength());
      FormatCli.PrereadExecutor.writeStats(inputFiles, PrereadType.CG.toString(), "DNA", true, 0, inputCounts, outputCounts, writer.getSdfId(), ps);
    }
  }

  @Override
  protected int mainExec(OutputStream out, LogStream initLog) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final File output = (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
      try (PrintStream summaryStream = new PrintStream(new FileOutputStream(new File(output, CommonFlags.SUMMARY_FILE)))) {
        final Collection<File> inputFiles = CommonFlags.getFileList(mFlags, CommonFlags.INPUT_LIST_FLAG, null, false);
        final Integer maximumNs = (Integer) mFlags.getValue(MAXIMUM_NS);

        final boolean useQuality = !mFlags.isSet(NO_QUALITY);
        final SAMReadGroupRecord samReadGroupRecord;
        if (mFlags.isSet(SamCommandHelper.SAM_RG)) {
          samReadGroupRecord = SamCommandHelper.validateAndCreateSamRG((String) mFlags.getValue(SamCommandHelper.SAM_RG), SamCommandHelper.ReadGroupStrictness.REQUIRED);
          final String platform = samReadGroupRecord.getPlatform();
          final MachineType mt = MachineType.COMPLETE_GENOMICS_2; // Default to new read structure
          if (!mt.compatiblePlatform(platform)) {
            if (platform == null || platform.length() == 0) {
              Diagnostic.warning("Read group platform not set, defaulting to \"" + mt.platform() + "\"");
              samReadGroupRecord.setPlatform(mt.platform());
            }
          }
        } else {
          samReadGroupRecord = null;
        }
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final PrintStream ps = new PrintStream(bos)) {
          performPreread(inputFiles, output, maximumNs, useQuality, ps, (Boolean) mFlags.getValue(COMPRESS_FLAG), mFlags.isSet(KEEP_NAMES), samReadGroupRecord);
          ps.flush();
          outStream.print(bos.toString());
          summaryStream.print(bos.toString());
        } catch (final IOException e) {
          if (output.getUsableSpace() == 0) {
            throw new NoTalkbackSlimException(e, ErrorType.DISK_SPACE, output.getPath());
          } else {
            throw e;
          }
        }
        return 0;
      }
    } finally {
      outStream.flush();
    }
  }

  static void printLine(String line, PrintStream[] streams) {
    for (final PrintStream stream : streams) {
      stream.println(line);
    }
  }
}
