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


import static com.rtg.launcher.CommonFlags.NO_GZIP;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Arrays;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.mode.DnaUtils;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.LineWriter;

/**
 * This class takes format directory and converts to fasta and QUALA format
 */
public final class Sdf2Quala extends AbstractCli {

  private static final String MODULE_NAME = "sdf2quala";
  private static final String INPUT = "input";
  private static final String OUTPUT_FILE = "output";
  private static final String RENAME = "Xrename";
  private static final String DEFAULT_QUALITY = "default-quality";

  static final String START_SEQUENCE = "start-id";
  static final String END_SEQUENCE = "end-id";

  private static final String QUALA_EXT = ".quala";
  private static final String FASTA_EXT = ".fasta";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert SDF to FASTA/QUALA";
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  static void initFlags(final CFlags flags) {
    flags.registerExtendedHelp();
    flags.setDescription("Converts SDF data into FASTA/QUALA files.");
    CommonFlagCategories.setCategories(flags);
    CommonFlags.initNoGzip(flags);
    flags.registerRequired('i', INPUT, File.class, CommonFlags.SDF, "SDF containing sequences").setCategory(INPUT_OUTPUT);
    flags.registerRequired('o', OUTPUT_FILE, File.class, CommonFlags.FILE, "basename for output files (extensions will be added)").setCategory(INPUT_OUTPUT);
    flags.registerOptional('R', RENAME, "rename the reads to their consecutive number; name of first read in file is '0'").setCategory(UTILITY);
    flags.registerOptional('q', DEFAULT_QUALITY, Integer.class, CommonFlags.INT, "default quality value to use if the SDF does not contain quality data (0-63)").setCategory(UTILITY);
    flags.registerOptional(START_SEQUENCE, Long.class, CommonFlags.INT, "inclusive lower bound on sequence id").setCategory(CommonFlagCategories.FILTERING);
    flags.registerOptional(END_SEQUENCE, Long.class, CommonFlags.INT, "exclusive upper bound on sequence id").setCategory(CommonFlagCategories.FILTERING);
    flags.setValidator(VALIDATOR);
  }

  private static final Validator VALIDATOR = new Validator() {
    @Override
    public boolean isValid(final CFlags flags) {
      if (flags.isSet(DEFAULT_QUALITY)) {
        final int qual = (Integer) flags.getValue(DEFAULT_QUALITY);
        if (qual < 0) {
          Diagnostic.error(ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE, DEFAULT_QUALITY, Integer.toString(qual), "0");
          return false;
        }
        if (qual > 63) {
          Diagnostic.error(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, DEFAULT_QUALITY, Integer.toString(qual), "63");
          return false;
        }
      }
      if (!CommonFlags.validateSDF((File) flags.getValue(INPUT))) {
        return false;
      }
      return CommonFlags.validateStartEnd(flags, START_SEQUENCE, END_SEQUENCE);
    }
  };

  /**
   * Convert an SDF to FASTQ
   *
   * @param out where the FASTQ file goes
   * @param err where error messages go
   * @throws IOException should an IO Error occur
   * @return 0 for success, 1 for failure
   */
  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final File preReadDir = (File) mFlags.getValue(INPUT);
    final String basename = mFlags.getValue(OUTPUT_FILE).toString();
    final boolean isPaired = ReaderUtils.isPairedEndDirectory(preReadDir);
    try {
      if (isPaired) {
        final LongRange calculatedRegion = doPrereadToQuala(ReaderUtils.getLeftEnd(preReadDir), basename + "_1", null);
        doPrereadToQuala(ReaderUtils.getRightEnd(preReadDir), basename + "_2", calculatedRegion);
      } else {
        doPrereadToQuala(preReadDir, basename, null);
      }
    } catch (InvalidParamsException e) {
      e.printErrorNoLog();
      return 1;
    }
    return 0;
  }

  //Calculated region for sloppy end
  private LongRange doPrereadToQuala(File preReadDir, String basename, LongRange calculatedRegion) throws IOException, InvalidParamsException {
    final long startId = mFlags.isSet(START_SEQUENCE) ? (Long) mFlags.getValue(START_SEQUENCE) : LongRange.MISSING;
    final long endId = calculatedRegion != null ? calculatedRegion.getEnd() : (mFlags.isSet(END_SEQUENCE) ? (Long) mFlags.getValue(END_SEQUENCE) : LongRange.MISSING);
    final LongRange r = SequencesReaderFactory.resolveRange(preReadDir, new LongRange(startId, endId));
    final boolean rename = mFlags.isSet(RENAME);
    try (SequencesReader read = SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty(preReadDir, r)) {
      final byte def = mFlags.isSet(DEFAULT_QUALITY) ? ((Integer) mFlags.getValue(DEFAULT_QUALITY)).byteValue() : -1;
      if (!read.hasQualityData()) {
        if (def < 0) {
          throw new InvalidParamsException(ErrorType.INFO_ERROR, "The input SDF does not have quality data and no default was provided.");
        }
      }

      final boolean gzip = !mFlags.isSet(NO_GZIP);
      final String seqFileName = basename + FASTA_EXT;
      final String qualFileName = basename + QUALA_EXT;
      final File sequenceOutputFile = gzip ? new File(seqFileName + FileUtils.GZ_SUFFIX) : new File(seqFileName);
      final File qualityOutputFile = gzip ? new File(qualFileName + FileUtils.GZ_SUFFIX) : new File(qualFileName);
      try (LineWriter seqWriter = new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(sequenceOutputFile, gzip, false)))) {
        try (LineWriter qualWriter = new LineWriter(new OutputStreamWriter(FileUtils.createOutputStream(qualityOutputFile, gzip, false)))) {
          process(read, seqWriter, qualWriter, rename, def);
        }
      }
    } catch (final FileNotFoundException e) {
      throw new NoTalkbackSlimException(ErrorType.FILE_NOT_FOUND, preReadDir.toString());
    }
    return r;
  }

  static void process(final SequencesReader read, final LineWriter seqWriter, LineWriter qualWriter, final boolean rename, final byte def) throws IOException {
    for (long seq = 0; seq < read.numberSequences(); ++seq) {
      final String sequenceName = rename || !read.hasNames() ? String.valueOf(seq) : read.fullName(seq);
      final String sequenceFastaName = ">" + sequenceName;
      seqWriter.writeln(sequenceFastaName);
      seqWriter.writeln(DnaUtils.bytesToSequenceIncCG(read.read(seq)));

      qualWriter.writeln(sequenceFastaName);

      final byte[] quality = new byte[read.length(seq)];
      if (read.hasQualityData()) {
        read.readQuality(seq, quality);
      } else {
        Arrays.fill(quality, def);
      }

      final StringBuilder sb = new StringBuilder();
      for (int k = 0; k < quality.length; ++k) {
        if (k > 0) {
          sb.append(" ");
        }
        sb.append(quality[k]);
      }
      qualWriter.writeln(sb.toString());
    }
  }


}
