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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.SimpleRangeMeta;
import com.rtg.util.intervals.ReferenceRanges;

/**
 * Adds VCF ID column based on VCF IDs from another VCF file.
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public class VcfIdAnnotator extends NamedRangesVcfAnnotator {

  /**
   * Constructor
   * @param vcfFiles VCF files containing variant IDs to be added to VCF id column.
   * @param fullSpan if true, full reference span each VCF record will be considered, otherwise just the start position.
   * @throws IOException if the BED file could not be loaded.
   */
  public VcfIdAnnotator(Collection<File> vcfFiles, boolean fullSpan) throws IOException {
    super(null, null, loadVcfIdRanges(vcfFiles), fullSpan);
  }

  private static ReferenceRanges<List<String>> loadVcfIdRanges(Collection<File> vcfFiles) throws IOException {
    final ReferenceRanges.Accumulator<List<String>> rangeData = new ReferenceRanges.Accumulator<>();
    for (final File vcfFile : vcfFiles) {
      try (final VcfReader reader = VcfReader.openVcfReader(vcfFile)) {
        while (reader.hasNext()) {
          final VcfRecord record = reader.next();
          final String id = record.getId();
          if (!VcfRecord.MISSING.equals(id)) {
            rangeData.addRangeData(record.getSequenceName(), new SimpleRangeMeta<>(record.getStart(), record.getEnd(), Arrays.asList(StringUtils.split(id, ';'))));
          }
        }
      }
    }
    return rangeData.getReferenceRanges();
  }
}
