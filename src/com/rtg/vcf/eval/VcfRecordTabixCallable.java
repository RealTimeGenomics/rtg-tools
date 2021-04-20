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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.ReferenceRegions;
import com.rtg.vcf.DecomposingVcfIterator;
import com.rtg.vcf.NullVcfWriter;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfSortRefiner;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;

/**
 * A callable which loads VcfRecords for given template
 */
public class VcfRecordTabixCallable implements Callable<LoadedVariants> {

  private static final boolean DECOMPOSE_MNPS = GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_DECOMPOSE_MNPS);
  private static final boolean DECOMPOSE_INDELS = GlobalFlags.getBooleanValue(ToolsGlobalFlags.VCFEVAL_DECOMPOSE_INDELS);

  private final File mInput;
  private final ReferenceRanges<String> mRanges;
  private final ReferenceRegions mEvalRegions;
  private final int mTemplateLength;
  private final VariantSetType mType;
  private final VariantFactory mFactory;
  private final int mMaxLength;
  private final File mDecomposedFile;
  private final boolean mRelaxedRef;

  VcfRecordTabixCallable(File file, ReferenceRanges<String> ranges, ReferenceRegions evalRegions, String templateName, Integer templateLength, VariantSetType type, VariantFactory factory, int maxLength, File preprocessDestDir, boolean relaxedRef) {
    if (!ranges.containsSequence(templateName)) {
      throw new IllegalArgumentException("Ranges supplied do not contain reference sequence " + templateName);
    }
    mInput = file;
    mRanges = ranges;
    mEvalRegions = evalRegions;
    mFactory = factory;
    mTemplateLength = templateLength;
    mType = type;
    mMaxLength = maxLength;
    mRelaxedRef = relaxedRef;
    if (preprocessDestDir != null) {
      mDecomposedFile = new File(preprocessDestDir, "decomposed_" + type.label() + "_" + templateName + ".vcf.gz");
    } else {
      mDecomposedFile = null;
    }
  }

  @Override
  public LoadedVariants call() throws Exception {
    int skipped = 0;
    int id = 0;
    final List<Variant> list = new ArrayList<>();
    try (VcfIterator reader = getReader()) {
      try (VcfWriter preprocessed = mDecomposedFile == null ? new NullVcfWriter(reader.getHeader()) : new VcfWriterFactory().make(reader.getHeader(), mDecomposedFile)) {
        while (reader.hasNext()) {
          final VcfRecord rec = reader.next();
          preprocessed.write(rec);
          ++id;

          // Skip variants that are too long (these cause problems during evaluation)
          int length = rec.getRefCall().length();
          for (String alt : rec.getAltCalls()) {
            length = Math.max(alt.length(), length);
          }
          if (mMaxLength > -1 && length > mMaxLength) {
            Diagnostic.userLog("Variant allele in " + mType.label() + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " has length (" + length + ") exceeding maximum allele length (" + mMaxLength + "), skipping.");
            ++skipped;
            continue;
          }

          // Skip variants which end outside the length of the template sequence
          if (mTemplateLength >= 0 && rec.getEnd() > mTemplateLength) {
            Diagnostic.userLog("Variant in " + mType.label() + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " ends outside the length of the reference sequence (" + mTemplateLength + ").");
            ++skipped;
            continue;
          }

          try {
            final Variant v = mFactory.variant(rec, id);
            if (v == null) { // Just wasn't variant according to the factory
              continue;
            }
            if (mEvalRegions != null && !mEvalRegions.overlapped(v)) {
              v.setStatus(VariantId.STATUS_OUTSIDE_EVAL);
            }
            list.add(v);
          } catch (SkippedVariantException e) {
            Diagnostic.userLog("Variant in " + mType.label() + " at " + rec.getSequenceName() + ":" + rec.getOneBasedStart() + " was skipped: " + e.getMessage());
            ++skipped;
          } catch (RuntimeException e) {
            Diagnostic.userLog("Got an exception processing " + mType.label() + " VCF record: " + rec);
            throw e;
          }
        }
        if (mRelaxedRef) {
          Variant.trimAlleles(list);
        }
        return new LoadedVariants(list, skipped, mDecomposedFile);
      }
    }
  }

  private VcfIterator getReader() throws IOException {
    VcfIterator reader = VcfReader.openVcfReader(mInput, mRanges);
    if (mDecomposedFile != null) {
      reader = new DecomposingVcfIterator(reader, null, DECOMPOSE_MNPS, DECOMPOSE_INDELS);
    }
    return new VcfSortRefiner(reader);
  }
}

