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
import java.io.Closeable;
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
import com.rtg.vcf.header.VcfHeader;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * Reads a <code>.vcf</code> input stream and converts it into VCF records.
 *
 */
public class VcfReader implements Closeable {

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
   * @param vcfFile file reader was generated from (will be read to obtain the VCF header information)
   * @throws IOException if an IO error occurs
   */
  VcfReader(TabixLineReader reader, File vcfFile) throws IOException {
    mIn = reader;
    try (LineReader headerReader = new BrLineReader(new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(vcfFile))))) {
      mHeader = parseHeader(headerReader);
    }
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
        throw new IOException("Cannot apply region restrictions when reading from stdin");
      }
      vcfr = new VcfReader(new TabixLineReader(f, TabixIndexer.indexFileName(f), ranges), f);
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
        throw new IOException("Cannot apply region restriction when reading from stdin");
      }
      vcfr = new VcfReader(new TabixLineReader(f, TabixIndexer.indexFileName(f), region), f);
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
      } catch (final IllegalArgumentException e) {
        //illegal argument here means badly formed header
        throw new IOException(e.getMessage(), e);
      }
    }
    if (header.getVersionLine() == null) {
      throw new IOException("No file format line found");
    }
    return header;
  }

  /**
   * Turn a line of <code>VCF</code> output into a {@link VcfRecord}
   * @param line line of file
   * @return the corresponding record
   */
  public static VcfRecord vcfLineToRecord(String line) {
    final VcfRecord rec = new VcfRecord();
    final String[] field = StringUtils.split(line, '\t');
    if (field.length < 8) {
      throw new IllegalArgumentException("Expected at least 8 fields");
    }
    rec.setSequence(StringUtils.deepCopy(field[CHROM_FIELD]));
    rec.setStart(Integer.parseInt(field[POS_FIELD]) - 1);
    rec.setId(StringUtils.deepCopy(field[ID_FIELD]));
    rec.setRefCall(StringUtils.deepCopy(field[REF_FIELD]));
    final String[] altSplit = StringUtils.split(field[ALT_FIELD], ',');
    for (final String anAltSplit : altSplit) {
      if (!anAltSplit.equals(".")) {
        rec.addAltCall(StringUtils.deepCopy(anAltSplit));
      }
    }
    rec.setQuality(field[QUAL_FIELD]);  // "." or float.
    final String[] filterSplit = StringUtils.split(field[FILTER_FIELD], ';');
    for (final String aFilterSplit : filterSplit) {
      if (!aFilterSplit.equals(".")) {
        rec.addFilter(StringUtils.deepCopy(aFilterSplit));
      }
    }
    if (!field[INFO_FIELD].equals(VcfRecord.MISSING)) {
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
        throw new IllegalArgumentException("Format field exists without sample fields");
      }
      final String[] genotypes = StringUtils.split(field[8], ':');
      rec.setNumberOfSamples(field.length - 9);
      for (int sample = 9; sample < field.length; sample++) {
        final String[] svalues = StringUtils.split(field[sample], ':');
        if (svalues.length > genotypes.length) {
          throw new IllegalArgumentException("Column " + (sample + 1) + " does not have the same number of values as specified in the format column. Field=" + field[sample]);
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


  /**
   * Check if there is another record to get.
   * @return boolean true if there is another record to get
   */
  public boolean hasNext() {
    return mCurrent != null;
  }

  /**
   * Get the current VCF record and advance the reader
   *
   * @return the current VCF record
   * @throws IOException when IO or format errors occur.
   */
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
        throw new IOException("Invalid VCF record. Expected " + mNumSamples + " samples, but there were " + mCurrent.getNumberOfSamples() + " on line:" + line);
      }
    } catch (final IllegalArgumentException e) {
      //illegal argument == badly formed record
      throw new IOException("Invalid VCF record. " + e.getMessage() + " on line:" + line, e);
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
