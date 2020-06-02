/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.launcher.CommonFlags;
import com.rtg.sam.SamRangeUtils;
import com.rtg.tabix.BrLineReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.TabixLineReader;
import com.rtg.util.cli.CFlags;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;

/**
 * Finer control over how VcfReaders are created.
 */
@TestClass("com.rtg.vcf.VcfReaderTest")
public class VcfReaderFactory {

  private ReferenceRanges<String> mRegions = null;
  private VcfParser mParser = new VcfParser();

  /**
   * Constructor using defaults to be overridden manually.
   */
  public VcfReaderFactory() { }

  /**
   * Create a reader factory, determining region configuration from a set of command line flags.
   * @param flags contains configuration settings for VCF reading
   * @throws IOException if there was a problem reading a specified regions BED file.
   */
  public VcfReaderFactory(CFlags flags) throws IOException {
    mRegions = CommonFlags.parseRegionOrBedRegions(flags);
  }

  /**
   * Set a custom VCF parser
   * @param parser the parser to use
   * @return this factory, for call chaining
   */
  public VcfReaderFactory parser(VcfParser parser) {
    mParser = parser;
    return this;
  }

  /**
   * Set the regions to load from (will require that the input is indexed)
   * @param regions the regions to load from
   * @return this factory, for call chaining
   */
  public VcfReaderFactory regions(ReferenceRanges<String> regions) {
    mRegions = regions;
    return this;
  }

  /**
   * Set a a single region to load from (will require that the input is indexed)
   * @param region the region to load from
   * @return this factory, for call chaining
   */
  public VcfReaderFactory region(RegionRestriction region) {
    mRegions = region == null ? null : SamRangeUtils.createExplicitReferenceRange(region);
    return this;
  }

  /**
   * Make a reader using the input file specified in the flags
   * @param flags flags containing the input file name
   * @return the VcfReader
   * @throws IOException if there was a problem creating the reader
   */
  public VcfReader make(CFlags flags) throws IOException {
    return make((File) flags.getValue(INPUT_FLAG));
  }

  /**
   * Make a reader using current configuration
   * @param f source file, or '-' for standard in
   * @return the VcfReader
   * @throws IOException if there was a problem creating the reader
   */
  public VcfReader make(File f) throws IOException {
    final VcfReader vcfr;
    if (mRegions == null || mRegions.allAvailable()) {
      vcfr = new VcfReader(mParser, new BrLineReader(new BufferedReader(new InputStreamReader(FileUtils.createInputStream(f, true)))));
    } else {
      if (FileUtils.isStdio(f)) {
        throw new IOException("Cannot apply region restrictions when reading VCF from stdin");
      }
      vcfr = new VcfReader(mParser, new TabixLineReader(f, TabixIndexer.indexFileName(f), mRegions), mParser.parseHeader(f));
    }
    return vcfr;
  }

  /**
   * Make a reader using current configuration on already opened reader.
   * @param r reader, which must be positioned at the start of the header
   * @return the VcfReader
   * @throws IOException if there was a problem creating the reader
   */
  public VcfReader make(BufferedReader r) throws IOException {
    final VcfReader vcfr;
    if (mRegions == null || mRegions.allAvailable()) {
      vcfr = new VcfReader(mParser, new BrLineReader(r));
    } else {
      throw new IOException("Cannot apply region restrictions when reading VCF from supplied reader");
    }
    return vcfr;
  }
}
