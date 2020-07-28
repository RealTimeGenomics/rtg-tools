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
package com.rtg.vcf.eval;

import java.io.File;
import java.io.IOException;

import com.rtg.bed.BedUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * A ROC filter for variants that overlap user-supplied genomic regions
 */
public class RegionsRocFilter extends RocFilter {

  private final ReferenceRegions mRegions;

  /**
   * Constructor
   * @param name the filter label (used to determine the output file name)
   * @param file source of regions, may be either BED or VCF
   * @throws IOException if the regions could not be read from the supplied file
   */
  public RegionsRocFilter(String name, File file) throws IOException {
    super(name);
    if (VcfUtils.isVcfExtension(file)) {
      mRegions = VcfUtils.regions(file);
    } else if (BedUtils.isBedExtension(file)) {
      mRegions = BedUtils.regions(file);
    } else {
      throw new NoTalkbackSlimException("Unrecognized file type for regions: " + file);
    }
  }

  @Override
  public boolean requiresGt() {
    return false;
  }

  @Override
  public boolean accept(VcfRecord rec, int[] gt) {
    return mRegions.overlapped(rec);
  }
}
