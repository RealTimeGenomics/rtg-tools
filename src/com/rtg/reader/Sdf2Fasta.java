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
import static com.rtg.util.cli.CommonFlagCategories.FILTERING;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.intervals.LongRange;

/**
 * Convert a preread dataset into FASTA format.
 *
 */
public class Sdf2Fasta extends AbstractCli {

  static final String MODULE_NAME = "sdf2fasta";

  static final String INPUT = "input";
  static final String OUTPUT = "output";
  static final String LINE_LENGTH = "line-length";
  static final String INTERLEAVE = "interleave";
  static final String RENAME = "Xrename";

  static final String ID_FILE_FLAG = "id-file";
  static final String NAMES_FLAG = "names";
  static final String TAXID_FLAG = "taxons";
  static final String START_SEQUENCE = "start-id";
  static final String END_SEQUENCE = "end-id";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert SDF to FASTA";
  }

  @Override
  protected void initFlags() {
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Converts SDF data into FASTA file(s).");
    CommonFlagCategories.setCategories(mFlags);

    registerTextExtractorFlags(mFlags);
    mFlags.registerOptional(TAXID_FLAG, "interpret supplied sequence as taxon ids instead of numeric sequence ids").setCategory(FILTERING);
    mFlags.registerOptional(INTERLEAVE, "interleave paired data into a single output file. Default is to split to separate output files").setCategory(UTILITY);

    mFlags.setValidator(VALIDATOR);
  }

  private static final Validator VALIDATOR = new Validator() {
    @Override
    public boolean isValid(final CFlags flags) {
      if (!flags.checkNand(TAXID_FLAG, START_SEQUENCE) || !flags.checkNand(TAXID_FLAG, END_SEQUENCE) || !flags.checkNand(TAXID_FLAG, NAMES_FLAG)) {
        return false;
      }
      if (flags.isSet(TAXID_FLAG) && !(flags.isSet(ID_FILE_FLAG) || flags.getAnonymousFlag(0).isSet())) {
        flags.setParseMessage("When using --" + TAXID_FLAG + ", sequences to extract must be specified, either explicitly, or using --" + ID_FILE_FLAG);
        return false;
      }
      return validateTextExtractorFlags(flags);
    }
  };


  // FASTA/FASTQ
  static void registerTextExtractorFlags(CFlags flags) {
    registerExtractorFlags(flags);
    flags.registerRequired('o', OUTPUT, File.class, CommonFlags.FILE, "output filename (extension added if not present). Use '-' to write to standard output").setCategory(INPUT_OUTPUT);
    flags.registerOptional('l', LINE_LENGTH, Integer.class, CommonFlags.INT, "maximum number of nucleotides to print on a line of output. A value of 0 indicates no limit", 0).setCategory(UTILITY);
    flags.registerOptional('R', RENAME, "rename the reads to their consecutive number; name of first read in file is '0'").setCategory(UTILITY);
    CommonFlags.initNoGzip(flags);
  }

  // FASTA/FASTQ/SDF
  static void registerExtractorFlags(CFlags flags) {
    flags.registerRequired('i', INPUT, File.class, CommonFlags.SDF, "SDF containing sequences").setCategory(INPUT_OUTPUT);
    flags.registerOptional('n', NAMES_FLAG, "interpret supplied sequence as names instead of numeric ids").setCategory(FILTERING);
    final Flag<File> listFlag = flags.registerOptional('I', ID_FILE_FLAG, File.class, CommonFlags.FILE, "file containing sequence ids, or sequence names if --" + NAMES_FLAG + " flag is set, one per line").setCategory(FILTERING);
    final Flag<String> idFlag = flags.registerRequired(String.class, CommonFlags.STRING, "id of sequence to extract, or sequence name if --" + NAMES_FLAG + " flag is set").setMinCount(0).setMaxCount(Integer.MAX_VALUE).setCategory(FILTERING);
    final Flag<Long> startFlag = flags.registerOptional(START_SEQUENCE, Long.class, CommonFlags.INT, "inclusive lower bound on sequence id").setCategory(FILTERING);
    final Flag<Long> endFlag = flags.registerOptional(END_SEQUENCE, Long.class, CommonFlags.INT, "exclusive upper bound on sequence id").setCategory(FILTERING);
    flags.addRequiredSet(idFlag);
    flags.addRequiredSet(listFlag);
    flags.addRequiredSet(startFlag, endFlag);
  }

  static boolean validateTextExtractorFlags(CFlags flags) {
    if ((Integer) flags.getValue(LINE_LENGTH) < 0) {
      Diagnostic.error(ErrorType.EXPECTED_NONNEGATIVE, LINE_LENGTH);
      return false;
    }
    return validateExtractorFlags(flags);
  }

  static boolean validateExtractorFlags(CFlags flags) {
    if (!CommonFlags.validateSDF((File) flags.getValue(INPUT))) {
      return false;
    }
    if (!flags.checkNand(NAMES_FLAG, START_SEQUENCE) || !flags.checkNand(NAMES_FLAG, END_SEQUENCE)) {
      return false;
    }
    return CommonFlags.validateStartEnd(flags, START_SEQUENCE, END_SEQUENCE);
  }


  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final int lineLength = (Integer) mFlags.getValue(LINE_LENGTH);
      final boolean gzip = !mFlags.isSet(NO_GZIP);
      final boolean rename = mFlags.isSet(RENAME);

      try (SdfReaderWrapper reader = new SdfReaderWrapper((File) mFlags.getValue(INPUT), false, false)) {
        try (WriterWrapper writer = new FastaWriterWrapper((File) mFlags.getValue(OUTPUT), reader, lineLength, rename, gzip, mFlags.isSet(INTERLEAVE))) {
          final WrapperFilter filter;
          if (mFlags.isSet(NAMES_FLAG)) {
            filter = new NameWrapperFilter(reader, writer);
          } else if (mFlags.isSet(TAXID_FLAG)) {
            filter = new TaxidWrapperFilter(reader, writer);
          } else {
            filter = new WrapperFilter(reader, writer);
          }

          boolean doAll = true;
          if (mFlags.getAnonymousFlag(0).isSet()) {
            doAll = false;
            for (final Object oi : mFlags.getAnonymousValues(0)) {
              filter.transfer((String) oi);
            }
          }
          if (mFlags.isSet(ID_FILE_FLAG)) {
            doAll = false;
            filter.transferFromFile((File) mFlags.getValue(ID_FILE_FLAG));
          }
          if (doAll || mFlags.isSet(START_SEQUENCE) || mFlags.isSet(END_SEQUENCE)) {
            final long startId = mFlags.isSet(START_SEQUENCE) ? (Long) mFlags.getValue(START_SEQUENCE) : LongRange.MISSING;
            final long endId = mFlags.isSet(END_SEQUENCE) ? (Long) mFlags.getValue(END_SEQUENCE) : LongRange.MISSING;
            filter.transfer(new LongRange(startId, endId));
          }
        }
      }
      Diagnostic.deleteLog(); // was successful execution
      return 0;
    } finally {
      outStream.flush();
    }
  }
}

