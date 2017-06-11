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
import static com.rtg.reader.Sdf2Fasta.OUTPUT;
import static com.rtg.reader.Sdf2Fasta.START_SEQUENCE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import com.rtg.launcher.AbstractCli;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Validator;
import com.rtg.util.intervals.LongRange;

/**
 * This class takes format directory and converts to FASTQ format
 */
public final class Sdf2Cg extends AbstractCli {

  static final String MODULE_NAME = "sdf2cg";

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String description() {
    return "convert SDF formatted CG data to CG TSV";
  }

  @Override
  protected void initFlags() {
    mFlags.setDescription("Converts SDF formatted data into Complete Genomics TSV file(s).");
    CommonFlagCategories.setCategories(mFlags);

    Sdf2Fasta.registerTextExtractorFlags(mFlags);

    mFlags.setValidator(VALIDATOR);
  }

  private static final Validator VALIDATOR = new Validator() {
    @Override
    public boolean isValid(final CFlags flags) {
      return Sdf2Fasta.validateTextExtractorFlags(flags);
    }
  };


  @Override
  protected int mainExec(final OutputStream out, final PrintStream err) throws IOException {
    final PrintStream outStream = new PrintStream(out);
    try {
      final boolean gzip = !mFlags.isSet(NO_GZIP);

      try (SdfReaderWrapper reader = new SdfReaderWrapper((File) mFlags.getValue(INPUT), false, false)) {
        try (WriterWrapper writer = new TsvWriterWrapper((File) mFlags.getValue(OUTPUT), reader, gzip)) {
          final WrapperFilter filter = new WrapperFilter(reader, writer);

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
