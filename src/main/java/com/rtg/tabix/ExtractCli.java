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
package com.rtg.tabix;

import static com.rtg.launcher.CommonFlags.REGION_SPEC;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.sam.RecordIterator;
import com.rtg.sam.SamClosedFileReader;
import com.rtg.sam.SamRangeUtils;
import com.rtg.sam.SamRegionRestriction;
import com.rtg.sam.SamUtils;
import com.rtg.sam.SkipInvalidRecordsIterator;
import com.rtg.util.StringUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * command class for extract module
 */
public class ExtractCli extends AbstractCli {

  private static final String HEADER_ONLY_FLAG = "header-only";
  private static final String HEADER_FLAG = "header";

  /**
   * extract records in given range to given output
   *
   * @param input input text file
   * @param tabix index for input file
   * @param regions the regions to extract
   * @param out destination for output
   * @throws java.io.IOException if an IO error occurs
   */
  public static void extractRecords(File input, File tabix, ReferenceRanges<String> regions, OutputStream out) throws IOException {
    try (TabixLineReader reader = new TabixLineReader(input, tabix, regions)) {
      String line;
      while ((line = reader.readLine()) != null) {
        out.write(line.getBytes());
        out.write(StringUtils.LS.getBytes());
      }
    }
  }

  @Override
  public String moduleName() {
    return "extract";
  }

  @Override
  public String description() {
    return "extract data from a tabix indexed file";
  }

  @Override
  protected void initFlags() {
    initFlags(mFlags);
  }

  private static void initFlags(CFlags flags) {
    flags.setDescription("Extract records from an indexed genomic position data file.");
    CommonFlagCategories.setCategories(flags);
    flags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        for (final Object o : flags.getAnonymousValues(1)) {
          final String region = (String) o;
          if (!SamRegionRestriction.validateRegion(region)) {
            flags.setParseMessage("The value \"" + region + "\" is not a well formed region.");
            return false;
          }
        }
        if (!flags.checkNand(HEADER_FLAG, HEADER_ONLY_FLAG)) {
          return false;
        }
        if (flags.isSet(HEADER_ONLY_FLAG)) {
          if (flags.getAnonymousFlag(1).isSet()) {
            flags.setParseMessage("A region cannot be specified when using --" + HEADER_ONLY_FLAG);
            return false;
          }
        }
        return true;
      }
    });
    flags.registerRequired(File.class, CommonFlags.FILE, "the indexed block compressed genome position data file to extract").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    final Flag<String> region = flags.registerRequired(String.class, CommonFlags.REGION, "the range to display. " + REGION_SPEC).setCategory(CommonFlagCategories.FILTERING);
    region.setMinCount(0);
    region.setMaxCount(Integer.MAX_VALUE);
    flags.registerOptional(HEADER_FLAG, "print out header also").setCategory(CommonFlagCategories.REPORTING);
    flags.registerOptional(HEADER_ONLY_FLAG, "print out header only").setCategory(CommonFlagCategories.REPORTING);
    flags.addRequiredSet(region);
    flags.addRequiredSet();
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File input = (File) mFlags.getAnonymousValue(0);
    if (!TabixIndexer.isBlockCompressed(input)) {
      throw new NoTalkbackSlimException(input.getPath() + " is not in bgzip format. Cannot extract records.");
    }
    final List<?> rStrings = mFlags.getAnonymousValues(1);
    final ReferenceRanges<String> regions;
    if (rStrings.isEmpty()) {
      regions = new ReferenceRanges<>(true);
    } else {
      final RegionRestriction[] r = new RegionRestriction[rStrings.size()];
      for (int i = 0; i < rStrings.size(); ++i) {
        r[i] = new RegionRestriction((String) rStrings.get(i));
      }
      regions = SamRangeUtils.createExplicitReferenceRange(r);
    }
    final boolean headerOnly = mFlags.isSet(HEADER_ONLY_FLAG);
    final boolean printHeader = headerOnly || mFlags.isSet(HEADER_FLAG);
    if (SamUtils.isBAMFile(input)) {
      extractSamBam(input, regions, out, printHeader, headerOnly); // BAM

    } else {
      final File index = TabixIndexer.indexFileName(input);
      if (!index.exists()) {
        throw new NoTalkbackSlimException("Index not found for file: " + input.getPath() + " expected index called: " + index.getPath());
      }
      final TabixIndexReader tir = new TabixIndexReader(index);

      if (tir.getOptions().mFormat == TabixIndexer.TabixOptions.FORMAT_SAM) {
        extractSamBam(input, regions, out, printHeader, headerOnly); // SAM

      } else {
        // Everything else
        if (printHeader) {
          extractHeader(input, (char) tir.getOptions().mMeta, out);
        }
        if (!headerOnly) {
          extractRecords(input, index, regions, out);
        }
      }
    }
    return 0;
  }

  private void extractHeader(File input, char metaChar, OutputStream out) throws IOException {
    try (BlockCompressedLineReader bclr = new BlockCompressedLineReader(new BlockCompressedInputStream(input))) {
      String line;
      while ((line = bclr.readLine()) != null && (line.length() == 0 || line.charAt(0) == metaChar)) {
        out.write((line + StringUtils.LS).getBytes());
      }
    }
  }

  private static void extractSamBam(File f, ReferenceRanges<String> regions, OutputStream out, boolean printHeader, boolean headerOnly) throws IOException {
    final SAMFileHeader header = SamUtils.getSingleHeader(f);
    try (final SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(header, true, out, printHeader)) {
      if (!headerOnly) {
        try (RecordIterator<SAMRecord> samfr = new SkipInvalidRecordsIterator(f.getPath(), new SamClosedFileReader(f, regions, null, header))) {
          while (samfr.hasNext()) {
            writer.addAlignment(samfr.next());
          }
        }
      }
    }
  }

}
