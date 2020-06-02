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

package com.rtg.util.intervals;

import java.io.IOException;

import com.rtg.bed.BedUtils;
import com.rtg.vcf.VcfUtils;

/**
 * A simple adaptor for ReferenceRegions that uses one-based addressing.
 * This class is intended to be used by the JavaScript filtering API.
 */
public class JSReferenceRegions {

  final ReferenceRegions mRegions;

  /**
   * Create a JSReferenceRegions from the specified BED file
   * @param filename the name of the BED file
   * @return a new <code>JSReferenceRegions</code>
   * @throws java.io.IOException when reading the file fails
   */
  public static JSReferenceRegions fromBed(String filename) throws IOException {
    return new JSReferenceRegions(BedUtils.regions(filename));
  }

  /**
   * Create a JSReferenceRegions from the specified VCF file
   * @param filename the name of the VCF file
   * @return a new <code>JSReferenceRegions</code>
   * @throws java.io.IOException when reading the file fails
   */
  public static JSReferenceRegions fromVcf(String filename) throws IOException {
    return new JSReferenceRegions(VcfUtils.regions(filename));
  }

  /**
   * Construct the adaptor
   * @param regions the wrapped reference regions
   */
  JSReferenceRegions(ReferenceRegions regions) {
    mRegions = regions;
  }

  /**
   * @param sequence name of the sequence
   * @param pos one based position within the sequence
   * @return true if these regions enclose the provided position
   */
  public boolean encloses(String sequence, int pos) {
    return mRegions.enclosed(sequence, pos - 1);
  }

  /**
   * @param sequence name of the sequence
   * @param start start position, one based
   * @param end end position, one based exclusive
   * @return true if these regions entirely encloses the range provided
   */
  public boolean encloses(String sequence, int start, int end) {
    return mRegions.enclosed(sequence, start - 1, end - 1);
  }

  /**
   * @param sequence name of the sequence
   * @param start start position, one based
   * @param end end position, one based exclusive
   * @return true if these regions overlap the provided range
   */
  public boolean overlaps(String sequence, int start, int end) {
    return mRegions.overlapped(sequence, start - 1, end - 1);
  }
}
