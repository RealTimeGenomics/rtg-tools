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
import static com.rtg.reader.Sdf2Fasta.END_SEQUENCE;
import static com.rtg.reader.Sdf2Fasta.ID_FILE_FLAG;
import static com.rtg.reader.Sdf2Fasta.INPUT;
import static com.rtg.reader.Sdf2Fasta.INTERLEAVE;
import static com.rtg.reader.Sdf2Fasta.LINE_LENGTH;
import static com.rtg.reader.Sdf2Fasta.NAMES_FLAG;
import static com.rtg.reader.Sdf2Fasta.OUTPUT;
import static com.rtg.reader.Sdf2Fasta.RENAME;
import static com.rtg.reader.Sdf2Fasta.START_SEQUENCE;
import static com.rtg.util.cli.CommonFlagCategories.UTILITY;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.intervals.LongRange;

/**
 * This class takes format directory and converts to FASTQ format
 */
public final class Sdf2Fastq extends AbstractCli {

  static final String MODULE_NAME = "sdf2fastq";

  static final String DEFAULT_QUALITY = "default-quality";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert SDF to FASTQ";
  }

  @Override
  protected void initFlags() {
    mFlags.registerExtendedHelp();
    mFlags.setDescription("Converts SDF data into FASTQ file(s).");
    CommonFlagCategories.setCategories(mFlags);

    Sdf2Fasta.registerTextExtractorFlags(mFlags);

    mFlags.registerOptional('q', DEFAULT_QUALITY, Integer.class, CommonFlags.INT, "default quality value to use if the SDF does not contain quality data (0-63)").setCategory(UTILITY);
    mFlags.registerOptional(INTERLEAVE, "interleave paired data into a single output file. Default is to split to separate output files").setCategory(UTILITY);

    mFlags.setValidator(VALIDATOR);
  }

  private static final Validator VALIDATOR = flags -> {
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

    return Sdf2Fasta.validateTextExtractorFlags(flags);
  };


  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final int lineLength = (Integer) mFlags.getValue(LINE_LENGTH);
      final boolean gzip = !mFlags.isSet(NO_GZIP);
      final boolean rename = mFlags.isSet(RENAME);
      final int def = mFlags.isSet(DEFAULT_QUALITY) ? (Integer) mFlags.getValue(DEFAULT_QUALITY) : -1;

      try (SdfReaderWrapper reader = new SdfReaderWrapper((File) mFlags.getValue(INPUT), false, false)) {
        try (WriterWrapper writer = new FastqWriterWrapper((File) mFlags.getValue(OUTPUT), reader, lineLength, rename, gzip, def, mFlags.isSet(INTERLEAVE))) {
          final WrapperFilter filter;
          if (mFlags.isSet(NAMES_FLAG)) {
            filter = new NameWrapperFilter(reader, writer);
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
      } catch (InvalidParamsException e) {
        e.printErrorNoLog();
        return 1;
      }
      return 0;
    } finally {
      outStream.flush();
    }
  }
}
