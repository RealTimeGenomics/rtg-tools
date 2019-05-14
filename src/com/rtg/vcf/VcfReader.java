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

import static com.rtg.launcher.CommonFlags.INPUT_FLAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rtg.launcher.CommonFlags;
import com.rtg.tabix.BrLineReader;
import com.rtg.tabix.LineReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.TabixLineReader;
import com.rtg.util.cli.CFlags;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Reads a <code>.vcf</code> input stream and converts it into VCF records.
 */
public class VcfReader implements VcfIterator {

  private final VcfParser mParser;
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
    this(new VcfParser(), new BrLineReader(in));
  }
  
  private VcfReader(VcfParser parser, BrLineReader in) throws IOException {
    this(parser, in, parser.parseHeader(in));
  }

  /**
   * Read VcfRecords from a region of a block-compressed file, pre-parsed header
   * @param reader source of record lines
   * @param header header for file currently being read (callers responsibility)
   * @throws IOException if an IO error occurs
   */
  VcfReader(LineReader reader, VcfHeader header) throws IOException {
    this(new VcfParser(), reader, header);
  }

  /**
   * Read VcfRecords from a region of a block-compressed file
   * @param reader source of record lines
   * @param header header for file currently being read (callers responsibility)
   * @throws IOException if an IO error occurs
   */
  private VcfReader(VcfParser parser, LineReader reader, VcfHeader header) throws IOException {
    mParser = parser;
    mIn = reader;
    mHeader = header;
    mNumSamples = mHeader.getNumberOfSamples();
    setNext();
  }

  /**
   * Open a <code>VCF</code> reader from commonly used flag arguments
   * @param flags the flags parser with all arguments set
   * @return the reader
   * @throws IOException if an IO Error occurs
   */
  static VcfReader openVcfReader(CFlags flags) throws IOException {
    final ReferenceRanges<String> regions = CommonFlags.parseRegionOrBedRegions(flags);
    return openVcfReader((File) flags.getValue(INPUT_FLAG), regions);
  }

  /**
   * Open a <code>VCF</code> reader
   * @param f <code>VCF</code> file, optionally gzipped. If f is '-', input will be read from System.in instead
   * @return the reader
   * @throws IOException if an IO Error occurs
   */
  public static VcfReader openVcfReader(File f) throws IOException {
    return new VcfReader(new BufferedReader(new InputStreamReader(FileUtils.createInputStream(f, true))));
  }

  /**
   * Open a <code>VCF</code> reader, optionally with a region restriction
   * @param f <code>VCF</code> file, optionally gzipped. If f is '-', input will be read from System.in instead
   * @param ranges regions to restrict to, null if whole file should be used
   * @return the reader
   * @throws IOException if an IO Error occurs or if trying to apply a region restriction when reading from System.in
   */
  public static VcfReader openVcfReader(File f, ReferenceRanges<String> ranges) throws IOException {
    final VcfReader vcfr;
    if (ranges == null || ranges.allAvailable()) {
      vcfr = new VcfReader(new BufferedReader(new InputStreamReader(FileUtils.createInputStream(f, true))));
    } else {
      if (FileUtils.isStdio(f)) {
        throw new IOException("Cannot apply region restrictions when reading VCF from stdin");
      }
      vcfr = new VcfReader(new VcfParser(), new TabixLineReader(f, TabixIndexer.indexFileName(f), ranges), VcfUtils.getHeader(f));
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
    final VcfReader vcfr;
    if (region == null) {
      vcfr = new VcfReader(new BufferedReader(new InputStreamReader(FileUtils.createInputStream(f, true))));
    } else {
      if (FileUtils.isStdio(f)) {
        throw new IOException("Cannot apply region restriction when reading VCF from stdin");
      }
      vcfr = new VcfReader(new VcfParser(), new TabixLineReader(f, TabixIndexer.indexFileName(f), region), VcfUtils.getHeader(f));
    }
    return vcfr;
  }

  /**
   * Turn a line of <code>VCF</code> output into a {@link VcfRecord}.
   * XXX This is method is now for use in tests, and should be moved
   * @param line line of file
   * @return the corresponding record
   */
  public static VcfRecord vcfLineToRecord(String line) {
    return new VcfParser().parseLine(line);
  }

  @Override
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
      mCurrent = mParser.parseLine(line);
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
