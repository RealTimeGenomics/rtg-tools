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

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;

/**
 * A callable which loads VcfRecords for given template
 */
public class VcfRecordTabixCallable implements Callable<LoadedVariants> {

  private final File mInput;
  private final ReferenceRanges<String> mRanges;
  private final int mTemplateLength;
  private final VariantSetType mType;
  private final VariantFactory mFactory;
  private final boolean mPassOnly;
  private final int mMaxLength;

  VcfRecordTabixCallable(File file, ReferenceRanges<String> ranges, String templateName, Integer templateLength, VariantSetType type, VariantFactory factory, boolean passOnly, int maxLength) {
    if (!ranges.containsSequence(templateName)) {
      throw new IllegalArgumentException("Ranges supplied do not contain reference sequence " + templateName);
    }
    mInput = file;
    mRanges = ranges;
    mFactory = factory;
    mTemplateLength = templateLength;
    mType = type;
    mPassOnly = passOnly;
    mMaxLength = maxLength;
  }

  @Override
  public LoadedVariants call() throws Exception {
    int skipped = 0;
    int id = 0;
    final List<Variant> list = new ArrayList<>();
    try (VcfReader reader = VcfReader.openVcfReader(mInput, mRanges)) {
      while (reader.hasNext()) {
        final VcfRecord rec = reader.next();
        id++;

        if (mPassOnly && rec.isFiltered()) {
          continue;
        }

        // Skip variants that are too long (these cause problems during evaluation)
        int length = rec.getRefCall().length();
        for (String alt : rec.getAltCalls()) {
          length = Math.max(alt.length(), length);
        }
        if (mMaxLength > -1 && length > mMaxLength) {
          Diagnostic.userLog("Variant allele in " + mType.label() + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " has length (" + length + ") exceeding maximum allele length (" + mMaxLength + "), skipping.");
          skipped++;
          continue;
        }
        
        // Skip variants with starts falling outside the expected length of the template sequence
        if (mTemplateLength >= 0 && rec.getStart() >= mTemplateLength) {
          Diagnostic.userLog("Variant in " + mType.label() + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " starts outside the length of the reference sequence (" + mTemplateLength + ").");
          skipped++;
          continue;
        }

        final Variant v = mFactory.variant(rec, id);
        if (v == null) { // Just wasn't variant according to the factory
          continue;
        }

        list.add(v);
      }
    }
    return new LoadedVariants(list, skipped);
  }
}

