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
package com.rtg.sam;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.rtg.reader.FormatCli;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SamReader;

/**
 * Contains methods for helping with command line flags and params related to SAM
 */
public final class SamCommandHelper {

  private SamCommandHelper() {
    super();
  }

  /** flag to capture SAM read group information */
  public static final String SAM_RG = "sam-rg";
  static final String STRING_OR_FILE = "STRING|FILE";

  /**
   * Method adds SAM read group flag to given input
   * @param flags flags to add <code>sam-rg</code> flag.
   */
  public static void initSamRg(CFlags flags) {
    initSamRg(flags, "ILLUMINA", CommonFlagCategories.REPORTING);
  }

  /**
   * Method adds SAM read group flag to given input
   * @param flags flags to add <code>sam-rg</code> flag.
   * @param examplePlatform the example platform to use.
   * @param category the category to register the flag under.
   */
  public static void initSamRg(CFlags flags, String examplePlatform, String category) {
    flags.registerOptional(SAM_RG, String.class, STRING_OR_FILE, "file containing a single valid read group SAM header line or a string in the form \"@RG\\tID:READGROUP1\\tSM:BACT_SAMPLE\\tPL:" + examplePlatform + "\"").setCategory(category);
  }

  /**
   * Validate SAM read group flag
   * @param flags flags to validate from
   * @return true if all OK, false otherwise
   */
  public static boolean validateSamRg(CFlags flags) {
    final String rg = (String) flags.getValue(SAM_RG);
    final File in = new File(rg);
    if (!in.exists() && rg.indexOf('\t') != -1) {
      flags.setParseMessage("given string \"" + rg + "\" for --" + SAM_RG + " contains literal tab characters, please use \\t instead");
      return false;
    } else if (in.isDirectory()) {
      flags.setParseMessage("given input file \"" + in.getPath() + "\" for --" + SAM_RG + " is a directory, must be a file");
      return false;
    }
    return true;
  }

  /**
   * Validate and create SAM read group record.
   * @param value the name of the file or the read group string to be parsed.
   * @param strict if true, throw exceptions when read group information is Invalid, if false return null instead.
   * @return the read group record or null when not in strict mode and input is invalid.
   * @throws InvalidParamsException if there is no read group or more than one read groups in the input.
   * @throws java.io.IOException if there is an IO problem.
   */
  public static SAMReadGroupRecord validateAndCreateSamRG(String value, ReadGroupStrictness strict) throws InvalidParamsException, IOException {
    final File rgFile = new File(value);
    final boolean fileMode = rgFile.exists();
    final BufferedInputStream bis;
    final String errorSubString;
    if (fileMode) {
      errorSubString = "file \"" + rgFile.getPath() + "\", please provide a file";
      bis = FileUtils.createInputStream(rgFile, false);
    } else {
      errorSubString = "string \"" + value + "\", please provide a string";
      final String convertedValue = value.replaceAll("\\\\t", "\t");
      bis = new BufferedInputStream(new ByteArrayInputStream(convertedValue.getBytes()));
    }
    try {
      final SamReader sfr = SamUtils.makeSamReader(bis);
      final List<SAMReadGroupRecord> readGroups = sfr.getFileHeader().getReadGroups();
      final int readGroupCount = readGroups.size();
      if (readGroupCount == 0) {
        if (strict == ReadGroupStrictness.REQUIRED) {
            throw new InvalidParamsException("No read group information present in the input " + errorSubString + ". A single read group is required");
        } else {
          return null;
        }
      }
      if (readGroupCount > 1) {
        if (strict == ReadGroupStrictness.REQUIRED || strict == ReadGroupStrictness.AT_MOST_ONE) {
          throw new InvalidParamsException("Multiple read groups present in the input " + errorSubString + ". A single read group is required");
        } else {
          return null;
        }
      }
      final SAMReadGroupRecord samReadGroupRecord = readGroups.get(0);
      if (samReadGroupRecord.getSample() == null) {
        Diagnostic.warning("Sample not specified in read group, it is recommended that the sample be set.");
      }
      return samReadGroupRecord;
    } finally {
      bis.close();
    }
  }

  /**
   * @param format the input format name
   * @return true if input is sam format
   */
  public static boolean isSamInput(String format) {
    return FormatCli.SAM_PE_FORMAT.equals(format) || FormatCli.SAM_SE_FORMAT.equals(format) || FormatCli.CGSAM_FORMAT.equals(format);
  }

  /**
   * This validates an retrieves from a sam file the read group matching the {@code selectReadGroup} parameter
   * @param rgFile sam file containing the read group
   * @param selectReadGroup the read group ID to locate
   * @return a {@code SAM ReadGroupRecord} that corresponds to the requested id
   * @throws java.io.IOException if IO falls over when reading the SAM file
   */
  public static SAMReadGroupRecord validateSelectedSamRG(File rgFile, String selectReadGroup) throws IOException {
    try (BufferedInputStream bis = FileUtils.createInputStream(rgFile, false)) {
      final SamReader sfr = SamUtils.makeSamReader(bis);
      final List<SAMReadGroupRecord> readGroups = sfr.getFileHeader().getReadGroups();
      final int readGroupCount = readGroups.size();
      if (readGroupCount == 0) {
        throw new InvalidParamsException("No read group information matching \"" + selectReadGroup + "\" present in the input file \"" + rgFile.getPath() + "\"");
      }
      for (SAMReadGroupRecord r : readGroups) {
        if (selectReadGroup.equals(r.getId())) {
          if (r.getSample() == null) {
            Diagnostic.warning("Sample not specified in read group, it is recommended to set the sample tag.");
          }
          return r;
        }
      }
      throw new InvalidParamsException("No read group information matching \"" + selectReadGroup + "\" present in the input file \"" + rgFile.getPath() + "\"");
    }

  }

  /**
   * How strict would you like read group validation to be
   */
  public enum ReadGroupStrictness {
    /** A single read group must be present */
    REQUIRED
    /** Either a single read group will be present or null will be returned */
    , OPTIONAL
    /** Null if none, the read group if single, an exception on more than one */
    , AT_MOST_ONE
  }
}
