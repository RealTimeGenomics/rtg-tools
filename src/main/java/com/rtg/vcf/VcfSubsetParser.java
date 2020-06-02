/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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

import java.io.IOException;
import java.util.ArrayList;

import com.rtg.tabix.LineReader;
import com.rtg.vcf.header.VcfHeader;

/**
 * Provides low level sample subsetting prior to full field parsing
 */
public class VcfSubsetParser extends VcfParser {

  private static final int[] SENTINEL = {-1};

  private final VcfSubset.VcfSampleStripperFactory mFactory;
  private int[] mSamplesToKeep = null;


  VcfSubsetParser(VcfSubset.VcfSampleStripperFactory factory) {
    mFactory = factory;
  }

  @Override
  public VcfHeader parseHeader(LineReader in) throws IOException {
    try {
      if (mSamplesToKeep != null) {
        throw new IllegalStateException("parseHeader() has already been called!");
      }
      final VcfHeader header = super.parseHeader(in);
      final VcfSampleStripper s = mFactory.make(header);
      if (s == null) {
        mSamplesToKeep = SENTINEL;
      } else {
        // Remove any configured samples, make a note of old->new sample positions
        final ArrayList<String> before = new ArrayList<>(header.getSampleNames());
        mFactory.make(header).updateHeader(header);
        final ArrayList<String> after = new ArrayList<>(header.getSampleNames());
        mSamplesToKeep = updateSampleToKeep(before, after);
      }
      return header;
    } catch (IOException | RuntimeException e) {
      in.close();
      throw e;
    }
  }

  private int[] updateSampleToKeep(ArrayList<String> before, ArrayList<String> after) {
    final int[] samplesToKeep = new int[after.size()];
    for (int i = 0, j = 0; i < samplesToKeep.length; ++i, ++j) {
      final String sample = after.get(i);
      while (j < before.size() && !before.get(j).equals(sample)) {
        j++;
      }
      if (j == before.size()) {
        throw new IllegalStateException("Sample " + sample + " wasn't found in original VCF");
      }
      samplesToKeep[i] = j;
    }
    return samplesToKeep;
  }

  String[] split(String src) {
    return split(src, mSamplesToKeep);
  }

  static String[] split(String src, int[] samplesToKeep) {
    final int outcols = samplesToKeep.length == 0 ? 8 : (9 + samplesToKeep.length);
    final String[] output = new String[outcols];
    int outcol = 0;
    int index;
    int lindex = 0;
    while ((index = src.indexOf('\t', lindex)) != -1 && outcol < 8) { // Get up to INFO
      output[outcol++] = src.substring(lindex, index);
      lindex = index + 1;
    }
    if (outcol < 7) {
      throw new VcfFormatException("Expected at least 8 fields");
    }

    if (index == -1) { // 8th column is last
      if (samplesToKeep.length > 0) {
        throw new VcfFormatException("Expected FORMAT field and samples");
      }
      output[outcol] = src.substring(lindex);
      return output; // Done, we were not expecting further samples
    } else if (samplesToKeep.length == 0) {
      return output;
    }

    index = src.indexOf('\t', lindex);
    if (index == -1) { // 8th column is last
      throw new VcfFormatException("Format field exists without sample fields");
    }
    output[outcol++] = src.substring(lindex, index); // This is the FORMAT field
    lindex = index + 1;

    // Collect sample columns
    int sidx = 0;
    while ((index = src.indexOf('\t', lindex)) != -1 && outcol < output.length) { // Get up to INFO
      if (sidx == samplesToKeep[outcol - 9]) {
        output[outcol++] = src.substring(lindex, index);
        if (outcol == output.length) { // Got all we wanted
          return output;
        }
      }
      sidx++;
      lindex = index + 1;
    }
    if (index == -1 && sidx == samplesToKeep[outcol - 9]) {
      output[outcol] = src.substring(lindex); // we wanted that last sample
      return output;
    }
    throw new VcfFormatException("Expected at least 8 fields");
  }

  @Override
  public VcfRecord parseLine(String line) {
    if (mSamplesToKeep == null) {
      throw new IllegalStateException("parseHeader() has not been called!");
    } else if (mSamplesToKeep == SENTINEL) {
      return super.parseLine(line);
    } else {
      return parseFields(split(line));
    }
  }
}
