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

import static com.rtg.reader.Sdf2Fasta.END_SEQUENCE;
import static com.rtg.reader.Sdf2Fasta.ID_FILE_FLAG;
import static com.rtg.reader.Sdf2Fasta.INPUT;
import static com.rtg.reader.Sdf2Fasta.NAMES_FLAG;
import static com.rtg.reader.Sdf2Fasta.START_SEQUENCE;
import static com.rtg.util.cli.CommonFlagCategories.INPUT_OUTPUT;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.LoggedCli;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.LogStream;

/**
 * Pulls out a subset of sequences from one SDF into another SDF.
 *
 */
public final class SdfSubset extends LoggedCli {

  private static boolean checkSequenceIds(CFlags flags) {
    if (!flags.isSet(ID_FILE_FLAG) && !flags.getAnonymousFlag(0).isSet() && !(flags.isSet(START_SEQUENCE) || flags.isSet(END_SEQUENCE))) {
      flags.setParseMessage("Sequences to extract must be specified, either explicitly, or using --" + ID_FILE_FLAG + ", or via --" + START_SEQUENCE + "/--" + END_SEQUENCE);
      return false;
    }
    return true;
  }

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    mFlags.setDescription("Extracts a subset of sequences from one SDF and outputs them to another SDF.");
    CommonFlags.initForce(mFlags);
    Sdf2Fasta.registerExtractorFlags(mFlags);
    mFlags.registerRequired('o', CommonFlags.OUTPUT_FLAG, File.class, CommonFlags.SDF, "output SDF").setCategory(INPUT_OUTPUT);

    mFlags.setValidator(flags -> CommonFlags.validateOutputDirectory(flags)
      && checkSequenceIds(flags)
      && Sdf2Fasta.validateExtractorFlags(flags));
  }

  @Override
  public String moduleName() {
    return "sdfsubset";
  }

  @Override
  public String description() {
    return "extract a subset of an SDF into a new SDF";
  }

  @Override
  protected File outputDirectory() {
    return (File) mFlags.getValue(CommonFlags.OUTPUT_FLAG);
  }


  @Override
  protected int mainExec(OutputStream out, LogStream log) throws IOException {
    try (final SdfReaderWrapper reader = new SdfReaderWrapper((File) mFlags.getValue(INPUT), false, false)) {
      try (final SdfWriterWrapper writer = new SdfWriterWrapper((File) mFlags.getValue(CommonFlags.OUTPUT_FLAG), reader, false)) {
        final WrapperFilter filter;
        if (mFlags.isSet(NAMES_FLAG)) {
          filter = new NameWrapperFilter(reader, writer);
        } else {
          filter = new WrapperFilter(reader, writer);
        }

        if (mFlags.getAnonymousFlag(0).isSet()) {
          final Collection<?> seqs = mFlags.getAnonymousValues(0);
          for (final Object oi : seqs) {
            filter.transfer((String) oi);
          }
        }
        if (mFlags.isSet(ID_FILE_FLAG)) {
          filter.transferFromFile((File) mFlags.getValue(ID_FILE_FLAG));
        }
        if (mFlags.isSet(START_SEQUENCE) || mFlags.isSet(END_SEQUENCE)) {
          final long startId = mFlags.isSet(START_SEQUENCE) ? (Long) mFlags.getValue(START_SEQUENCE) : LongRange.MISSING;
          final long endId = mFlags.isSet(END_SEQUENCE) ? (Long) mFlags.getValue(END_SEQUENCE) : LongRange.MISSING;
          filter.transfer(new LongRange(startId, endId));
        }

        Diagnostic.progress("Extracted " + filter.getWritten() + " sequences");
      }
    }
    return 0;
  }
}
