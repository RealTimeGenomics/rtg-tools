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

package com.rtg.vcf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rtg.launcher.CommonFlags;
import com.rtg.tabix.BrLineReader;
import com.rtg.tabix.LineReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.TabixLineReader;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOIterator;
import com.rtg.vcf.header.VcfHeader;

/**
 * Reads a <code>.vcf</code> input stream and converts it into VCF records.
 */
public class VcfReader implements IOIterator<VcfRecord> {

  private static final int CHROM_FIELD = 0;
  private static final int POS_FIELD = 1;
  private static final int ID_FIELD = 2;
  private static final int REF_FIELD = 3;
  private static final int ALT_FIELD = 4;
  private static final int QUAL_FIELD = 5;
  private static final int FILTER_FIELD = 6;
  private static final int INFO_FIELD = 7;

  private final LineReader mIn;
  private final VcfHeader mHeader;
  private final int mNumSamples;
  private VcfRecord mCurrent;

  /**
   * Create a new VCF reader. In general you should use <code>VcfReader.openVcfReader</code> instead.
   *
   * @param in where to read from
   * @throws IOException when IO or format errors occur.
   */
  public VcfReader(BufferedReader in) throws IOException {
    mIn = new BrLineReader(in);
    try {
      mHeader = parseHeader(mIn);
    } catch (IOException | RuntimeException e) {
      mIn.close();
      throw e;
    }
    mNumSamples = mHeader.getNumberOfSamples();
    setNext();
  }

  /**
   * Read VcfRecords from a region of a block-compressed file
   * @param reader source of record lines
   * @param header header for file currently being read (callers responsibility)
   * @throws IOException if an IO error occurs
   */
  VcfReader(TabixLineReader reader, VcfHeader header) throws IOException {
    mIn = reader;
    mHeader = header;
    mNumSamples = mHeader.getNumberOfSamples();
    setNext();
  }

  /**
   * Open a <code>VCF</code> reader
   * @param f <code>VCF</code> file, optionally gzipped. If f is '-', input will be read from System.in instead
   * @return the reader
   * @throws IOException if an IO Error occurs
   */
  public static VcfReader openVcfReader(File f) throws IOException {
    final boolean stdin = CommonFlags.isStdio(f);
    return new VcfReader(new BufferedReader(new InputStreamReader(stdin ? System.in : FileUtils.createInputStream(f, true))));
  }

  /**
   * Open a <code>VCF</code> reader, optionally with a region restriction
   * @param f <code>VCF</code> file, optionally gzipped. If f is '-', input will be read from System.in instead
   * @param ranges regions to restrict to, null if whole file should be used
   * @return the reader
   * @throws IOException if an IO Error occurs or if trying to apply a region restriction when reading from System.in
   */
  public static VcfReader openVcfReader(File f, ReferenceRanges<String> ranges) throws IOException {
    final boolean stdin = CommonFlags.isStdio(f);
    final VcfReader vcfr;
    if (ranges == null || ranges.allAvailable()) {
      vcfr = new VcfReader(new BufferedReader(new InputStreamReader(stdin ? System.in : FileUtils.createInputStream(f, true))));
    } else {
      if (stdin) {
        throw new IOException("Cannot apply region restrictions when reading VCF from stdin");
      }
      vcfr = new VcfReader(new TabixLineReader(f, TabixIndexer.indexFileName(f), ranges), VcfUtils.getHeader(f));
    }
    return vcfr;
  }

  /**
   * Open a <code>VCF</code> reader, optionally with a region restriction
   * @param f <code>VCF</code> file, optionally gzipped. If f is '-', input will be read from System.in instead
   * @param region region to restrict to, null if whole file should be used
   * @return the reader
   * @throws IOException if an IO Error occurs or if trying to apply a region restriction when reading from System.in
   */
  public static VcfReader openVcfReader(File f, RegionRestriction region) throws IOException {
    final boolean stdin = CommonFlags.isStdio(f);
    final VcfReader vcfr;
    if (region == null) {
      vcfr = new VcfReader(new BufferedReader(new InputStreamReader(stdin ? System.in : FileUtils.createInputStream(f, true))));
    } else {
      if (stdin) {
        throw new IOException("Cannot apply region restriction when reading VCF from stdin");
      }
      vcfr = new VcfReader(new TabixLineReader(f, TabixIndexer.indexFileName(f), region), VcfUtils.getHeader(f));
    }
    return vcfr;
  }

  /**
   * Called from within the constructor. After successful header parse, the input reader is positioned at the
   * first variant line of the VCF.
   */
  private static VcfHeader parseHeader(LineReader in) throws IOException {
    final VcfHeader header = new VcfHeader();
    String line;
    while ((line = in.readLine()) != null) {
      try {
        if (line.startsWith("##")) {
          header.addMetaInformationLine(line);
        } else if (line.startsWith("#")) {
          //should always be last header line
          header.addColumnHeaderLine(line);
          break;
        }
      } catch (final VcfFormatException e) {
        throw new VcfFormatException("Invalid VCF header. " + e.getMessage() + " on line:" + line); // Add context information
      }
    }
    if (header.getVersionLine() == null) {
      throw new VcfFormatException("No VCF file format version header line found");
    }
    return header;
  }

  /**
   * Turn a line of <code>VCF</code> output into a {@link VcfRecord}
   * @param line line of file
   * @return the corresponding record
   */
  public static VcfRecord vcfLineToRecord(String line) {
    final String[] field = StringUtils.split(line, '\t');
    if (field.length < 8) {
      throw new VcfFormatException("Expected at least 8 fields");
    }
    final int pos;
    try {
      pos = Integer.parseInt(field[POS_FIELD]) - 1;
    } catch (NumberFormatException e) {
      throw new VcfFormatException(e.getMessage());
    }
    final VcfRecord rec = new VcfRecord(StringUtils.deepCopy(field[CHROM_FIELD]), pos, StringUtils.deepCopy(field[REF_FIELD]));
    rec.setId(StringUtils.deepCopy(field[ID_FIELD]));
    if (!VcfRecord.MISSING.equals(field[ALT_FIELD])) {
      final String[] altSplit = StringUtils.split(field[ALT_FIELD], ',');
      for (final String anAltSplit : altSplit) {
        rec.addAltCall(StringUtils.deepCopy(anAltSplit));
      }
    }
    rec.setQuality(field[QUAL_FIELD]);  // "." or float.
    final String[] filterSplit = StringUtils.split(field[FILTER_FIELD], ';');
    for (final String aFilterSplit : filterSplit) {
      if (!VcfRecord.MISSING.equals(aFilterSplit)) {
        rec.addFilter(StringUtils.deepCopy(aFilterSplit));
      }
    }
    if (!VcfRecord.MISSING.equals(field[INFO_FIELD])) {
      final String[] infoSplit = StringUtils.split(field[INFO_FIELD], ';');
      for (final String anInfoSplit : infoSplit) {
        final String info = StringUtils.deepCopy(anInfoSplit);
        final int eq = info.indexOf('=');
        if (eq < 1) {
          rec.addInfo(info);
        } else {
          final String key = info.substring(0, eq);
          final String[] vals = StringUtils.split(info.substring(eq + 1), ',');
          rec.addInfo(key, vals);
        }
      }
    }
    rec.setNumberOfSamples(0);
    // now parse each sample field.
    if (field.length > 8) {
      if (field.length == 9) {
        throw new VcfFormatException("Format field exists without sample fields");
      }
      final String[] genotypes = StringUtils.split(field[8], ':');
      rec.setNumberOfSamples(field.length - 9);
      for (int sample = 9; sample < field.length; sample++) {
        final String[] svalues = StringUtils.split(field[sample], ':');
        if (svalues.length > genotypes.length) {
          throw new VcfFormatException("Column " + (sample + 1) + " does not have the same number of values as specified in the format column. Field=" + field[sample]);
        }
        for (int i = 0; i < svalues.length; i++) {
          rec.addFormatAndSample(genotypes[i], svalues[i]);
        }
        for (int i = svalues.length; i < genotypes.length; i++) {
          rec.addFormatAndSample(genotypes[i], VcfRecord.MISSING);
        }
      }
    }
    return rec;
  }

  /**
   * @return the header
   */
  public VcfHeader getHeader() {
    return mHeader;
  }

  @Override
  public boolean hasNext() {
    return mCurrent != null;
  }

  @Override
  public VcfRecord next() throws IOException {
    final VcfRecord result = peek();
    setNext();
    return result;
  }

  /**
   * Get the current VCF record without advancing the reader
   *
   * @return the current VCF record, or null if none.
   */
  public VcfRecord peek() {
    if (mCurrent == null) {
      throw new IllegalStateException("No more records");
    }
    return mCurrent;
  }

  /**
   * Read the next record, if any.
   * @return true if there is a valid next record.
   * @throws IOException when IO or format errors occur.
   */
  private boolean setNext() throws IOException {
    final String line = mIn.readLine();
    if (line == null) {
      mCurrent = null;
      return false;
    }
    try {
      mCurrent = vcfLineToRecord(line);
      if (mCurrent.getNumberOfSamples() != mNumSamples) {
        throw new VcfFormatException("Expected " + mNumSamples + " samples, but there were " + mCurrent.getNumberOfSamples());
      }
    } catch (final VcfFormatException e) {
      throw new VcfFormatException("Invalid VCF record. " + e.getMessage() + " on line:" + line); // Add context information
    }
    return true;
  }

  /**
   * closes internal streams
   * @throws IOException if an IO error occurs
   */
  @Override
  public void close() throws IOException {
    mIn.close();
  }

}
