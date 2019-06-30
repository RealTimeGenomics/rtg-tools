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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.rtg.util.MultiSet;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.header.VcfHeader;

/**
 * Provides GT merging that takes a majority vote at sample variants are genotyped multiple times.
 */
public class VcfGtMajorityMerger extends VcfRecordMerger {

  private static final String DIP_MISSING = "./.";

  /**
   * Constructor
   */
  public VcfGtMajorityMerger() {
    super(VcfUtils.FORMAT_GENOTYPE, false);
  }


  @Override
  protected boolean mergeSamples(VcfRecord[] records, VcfHeader[] headers, VcfRecord dest, VcfHeader destHeader, AlleleMap map, Set<String> unmergeableFormatFields, boolean dropUnmergeable) {
    final ArrayList<String> sampleGts = new ArrayList<>();
    dest.getFormatAndSample().put(VcfUtils.FORMAT_GENOTYPE, sampleGts);
    dest.setNumberOfSamples(destHeader.getNumberOfSamples());
    final List<String> names = destHeader.getSampleNames();
    if (map.mAltsChanged) {
      // This is interesting, dump the situation:
      Diagnostic.developerLog("INTERESTING (alts changed)");
      for (VcfRecord r : records) {
        Diagnostic.developerLog("REC: " + r.toString());
      }
    }
    // For each sample we are going to output
    for (int destSampleIndex = 0; destSampleIndex < names.size(); ++destSampleIndex) {
      final MultiSet<String> counts = new MultiSet<>();
      // Count the genotypes from the input records
      for (int i = 0; i < headers.length; ++i) {
        final int sampleIndex = headers[i].getSampleIndex(names.get(destSampleIndex));
        if (sampleIndex > -1 && records[i].hasFormat(VcfUtils.FORMAT_GENOTYPE)) {

          // Get the GT, remap alleles, and normalize allele ordering
          final VcfRecord record = records[i];
          final String gtStr = record.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sampleIndex);
          int[] splitGt = map.splitRemapGt(gtStr, record, i);

          if (!VcfUtils.isMissingGt(splitGt)) {
            Arrays.sort(splitGt); //  Ensure that het 0/1 vs 1/0 don't cause apparent discordance
            counts.add(VcfUtils.joinGt(false, splitGt));
          }
        }
      }
      if (counts.keySet().isEmpty()) {
        sampleGts.add(DIP_MISSING);
      } else {
        if (counts.keySet().size() > 1) {
          Diagnostic.developerLog("Discordant genotypes found at " + new SequenceNameLocusSimple(dest) + " for sample " + names.get(destSampleIndex) + " " + counts);// Log the distribution?
        }
        String maxElement = null;
        int maxCount = -1;
        for (String genotype : counts.keySet()) {
          int c = counts.get(genotype);
          if (c > maxCount) {
            maxElement = genotype;
            maxCount = c;
          }
        }
        if ((double) maxCount / counts.totalCount() > 0.75) {
          sampleGts.add(maxElement);
        } else {
          sampleGts.add(DIP_MISSING);
        }
      }
    }
    return true;
  }
}
