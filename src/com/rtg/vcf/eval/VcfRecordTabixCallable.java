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

package com.rtg.vcf.eval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.rtg.launcher.GlobalFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * A callable which loads VcfRecords for given template
 */
public class VcfRecordTabixCallable implements Callable<LoadedVariants> {

  private static final boolean ANY_ALT_BASELINE = GlobalFlags.getBooleanValue(GlobalFlags.VCFEVAL_ANY_ALLELE_BASELINE);

  private final File mInput;
  private final ReferenceRanges<String> mRanges;
  private final String mSampleName;
  private final int mTemplateLength;
  private final VariantSetType mType;
  private final RocSortValueExtractor mExtractor;
  private final boolean mPassOnly;
  private final boolean mSquashPloidy;
  private final int mMaxLength;

  VcfRecordTabixCallable(File file, ReferenceRanges<String> ranges, String templateName, Integer templateLength, VariantSetType type, String sample, RocSortValueExtractor extractor, boolean passOnly, boolean squashPloidy, int maxLength) {
    if (!ranges.containsSequence(templateName)) {
      throw new IllegalArgumentException("Ranges supplied do not contain reference sequence " + templateName);
    }
    mInput = file;
    mRanges = ranges;
    mSampleName = sample;
    mTemplateLength = templateLength;
    mType = type;
    mExtractor = extractor;
    mPassOnly = passOnly;
    mSquashPloidy = squashPloidy;
    mMaxLength = maxLength;
  }

  @Override
  public LoadedVariants call() throws Exception {
    int skipped = 0;
    final List<Variant> list = new ArrayList<>();
    try (VcfReader reader = VcfReader.openVcfReader(mInput, mRanges)) {
      final VcfHeader header = reader.getHeader();
      final String label = mType == VariantSetType.BASELINE ? "baseline" : "calls";
      final int sampleId = VcfUtils.getSampleIndexOrDie(header, mSampleName, label);
      final VariantFactory fact;
      final boolean anyAltComparison = mType == VariantSetType.BASELINE && ANY_ALT_BASELINE;
      if (anyAltComparison) {
        fact = new SquashPloidyVariant.AnyAltFactory(mExtractor);
      } else if (mSquashPloidy) {
        fact = new SquashPloidyVariant.Factory(sampleId, mExtractor);
      } else {
        fact = new Variant.Factory(sampleId, mExtractor);
      }
      Variant last = null;
      while (reader.hasNext()) {
        final VcfRecord rec = reader.next();

        if (mPassOnly && rec.isFiltered()) {
          continue;
        }

        // Skip non-variant, SV
        if (!anyAltComparison && !VcfUtils.hasDefinedVariantGt(rec, sampleId)) {
          continue;
        }

        // Skip variants that are too long (these cause problems during evaluation)
        int length = rec.getRefCall().length();
        for (String alt : rec.getAltCalls()) {
          length = Math.max(alt.length(), length);
        }
        if (mMaxLength > -1 && length > mMaxLength) {
          Diagnostic.userLog("Variant in " + label + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " exceeds maximum length, skipping.");
          skipped++;
          continue;
        }
        
        // Skip variants with starts falling outside the expected length of the template sequence
        if (mTemplateLength >= 0 && rec.getStart() >= mTemplateLength) {
          Diagnostic.userLog("Variant in " + label + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " starts outside the length of the reference sequence (" + mTemplateLength + ").");
          skipped++;
          continue;
        }

        // Skip overlapping variants
        final Variant v = fact.variant(rec);
        if (last != null) {
          if (v.getStart() < last.getEnd()) {
            Diagnostic.userLog("Overlapping variants aren't supported, skipping current variant from " + label + ".\nPrevious variant: " + last + "\nCurrent variant:  " + v);
            skipped++;
            continue;
          }
          if ((v.getStart() == last.getStart()) && (v.getStart() == v.getEnd()) && (last.getStart() == last.getEnd())) { // Pure inserts where ordering is ambiguous
            Diagnostic.userLog("Ambiguous inserts aren't supported, skipping current variant from " + label + ".\nPrevious variant: " + last + "\nCurrent variant:  " + v);
            skipped++;
            continue;
          }
        }
        last = v;
        list.add(v);
      }
    }
    return new LoadedVariants(list, skipped);
  }
}

