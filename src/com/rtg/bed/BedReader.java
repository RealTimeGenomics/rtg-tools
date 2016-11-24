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

package com.rtg.bed;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rtg.tabix.BrLineReader;
import com.rtg.tabix.LineReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.TabixLineReader;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.IOIterator;

import htsjdk.samtools.util.BlockCompressedInputStream;

/**
 * BED file reader class.
 */
public class BedReader implements IOIterator<BedRecord> {

  private final LineReader mIn;
  private final BedHeader mHeader;
  private BedRecord mCurrent;

  private final int mMinAnnotations;

  /**
   * create a new VCF reader.
   *
   * @param in where to read from
   * @throws IOException when I/O or format errors occur.
   */
  public BedReader(BufferedReader in) throws IOException {
    this(in, 0);
  }

  /**
   * create a new BED reader with a minimum number of annotations
   *
   * @param in where to read from
   * @param minAnnotations the minimum number of annotations to require on each line of a file
   * @throws IOException when I/O or format errors occur.
   */
  public BedReader(BufferedReader in, int minAnnotations) throws IOException {
    mMinAnnotations = minAnnotations;
    mIn = new BrLineReader(in);
    mHeader = parseHeader(mIn);
  }

  private BedReader(TabixLineReader reader, File bedFile, int minAnnotations) throws IOException {
    mMinAnnotations = minAnnotations;
    mIn = reader;
    try (BrLineReader headerReader = new BrLineReader(new BufferedReader(new InputStreamReader(new BlockCompressedInputStream(bedFile))))) {
      mHeader = parseHeader(headerReader);
    }
    setNext();
  }

  /**
   * Open a <code>bed</code> reader, optionally with a region restriction
   * @param region region to restrict to, null if whole file should be used
   * @param f <code>BED</code> file, optionally gzipped
   * @param minAnnotations minimum number of annotation columns needed in BED input
   * @return the reader
   * @throws IOException if an I/O error occurs
   */
  public static BedReader openBedReader(RegionRestriction region, File f, int minAnnotations) throws IOException {
    final boolean stdin = FileUtils.isStdio(f);
    final BedReader bedr;
    if (region != null) {
      if (stdin) {
        throw new IOException("Cannot apply region restriction when reading BED from stdin");
      }
      bedr = new BedReader(new TabixLineReader(f, TabixIndexer.indexFileName(f), region), f, minAnnotations);
    } else {
      bedr = new BedReader(new BufferedReader(new InputStreamReader(stdin ? System.in : FileUtils.createInputStream(f, true))), minAnnotations);
    }
    return bedr;
  }

  private BedHeader parseHeader(LineReader in) throws IOException {
    final BedHeader header = new BedHeader();
    String line;
    while ((line = in.readLine()) != null) {
      try {
        if (line.startsWith("#") || line.startsWith("track") || line.startsWith("browser") || line.length() == 0) {
          header.addHeaderLine(line);
        } else {
          setCurrent(line);
          break;
        }
      } catch (final IllegalArgumentException e) {
        //illegal argument here means badly formed header
        throw new IOException(e.getMessage(), e);
      }
    }
    return header;
  }

  @Override
  public boolean hasNext() {
    return mCurrent != null;
  }

  @Override
  public BedRecord next() throws IOException {
    if (mCurrent == null) {
      throw new IllegalStateException("No more records");
    }
    final BedRecord result = mCurrent;
    setNext();
    return result;
  }

  /**
   * Read the next record, if any.
   * @return true if there is a valid next record.
   * @throws IOException when IO or format errors occur.
   */
  private boolean setNext() throws IOException {
    String line;
    //TODO this would be problematic if there was ever a reference sequence named track
    while ((line = mIn.readLine()) != null && (line.length() == 0 || line.startsWith("track\t"))) { }
    if (line == null) {
      mCurrent = null;
      return false;
    }
    setCurrent(line);
    return true;
  }

  private void setCurrent(String line) throws IOException {
    try {
      mCurrent = BedRecord.fromString(line);
      if (mCurrent.getAnnotations().length < mMinAnnotations) {
        throw new IllegalArgumentException("Must have at least " + mMinAnnotations + " annotations");
      }
    } catch (final NumberFormatException e) {
      throw new IOException("Invalid BED line: Could not parse coordinates, line:" + line);
    } catch (final IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      //illegal argument == badly formed record
      throw new IOException("Invalid BED line: " + e.getMessage() + ", line:" + line, e);
    }
  }

  /**
   * Get the header.
   * @return the header.
   */
  public BedHeader getHeader() {
    return mHeader;
  }

  @Override
  public void close() throws IOException {
    mIn.close();
  }
}
