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
import java.util.List;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.header.VcfHeader;

/**
 * Removes unused ALT alleles recoding the existing genotype references if necessary
 */
public class VcfAltCleaner implements VcfAnnotator {

  @Override
  public void updateHeader(final VcfHeader header) {
  }

  private boolean isAllUsed(final boolean[] used) {
    boolean res = true;
    for (final boolean u : used) {
      res &= u;
    }
    return res;
  }


  @Override
  public void annotate(final VcfRecord rec) {
    final List<String> alts = rec.getAltCalls();
    final boolean[] used = new boolean[alts.size()];
    final int samples = rec.getNumberOfSamples();
    for (int k = 0; k < samples; ++k) {
      final String gt = rec.getSampleString(k, VcfUtils.FORMAT_GENOTYPE);
      final int[] splitGt = VcfUtils.splitGt(gt);
      for (final int alleleCode : splitGt) {
        if (alleleCode > used.length) {
          Diagnostic.warning("Ignoring allele code " + alleleCode + " in " + rec);
        } else if (alleleCode > 0) {
          used[alleleCode - 1] = true;
        }
      }
    }
    if (!isAllUsed(used)) {
      // At least one ALT allele was not used, need to remap them all
      final List<String> originalAlts = new ArrayList<>(alts);
      alts.clear(); // somewhat risky, but we are going to recode all the samples
      final int[] alleleCodeRemap = new int[used.length + 1]; // entry 0 is for the ref and remains as a 0
      for (int k = 0, j = 0; k < used.length; ++k) {
        if (used[k]) {
          alleleCodeRemap[k + 1] = ++j;
          alts.add(originalAlts.get(k));
        }
      }
      for (int k = 0; k < samples; ++k) {
        final String gt = rec.getSampleString(k, VcfUtils.FORMAT_GENOTYPE);
        final int[] splitGt = VcfUtils.splitGt(gt);
        for (int j = 0; j < splitGt.length; ++j) {
          if (splitGt[j] > 0 && splitGt[j] < alleleCodeRemap.length) {
            splitGt[j] = alleleCodeRemap[splitGt[j]];
          }
        }
        rec.setFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.joinGt(VcfUtils.isPhasedGt(gt), splitGt), k);
      }
    }
  }
}
