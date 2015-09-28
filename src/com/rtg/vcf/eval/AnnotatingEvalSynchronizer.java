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
import java.util.LinkedHashMap;

import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 * Creates typical vcfeval output files with separate VCF files and ROC files.
 */
class AnnotatingEvalSynchronizer extends WithInfoEvalSynchronizer {

  private final VcfWriter mBase;
  private final VcfWriter mCalls;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param callsSampleName the name of the sample used in the calls
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param rtgStats true to output additional ROC curves for RTG specific attributes
   * @throws IOException if there is a problem opening output files
   */
  AnnotatingEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                             String callsSampleName, RocSortValueExtractor extractor,
                             File outdir, boolean zip, boolean slope, boolean rtgStats) throws IOException {
    super(baseLineFile, callsFile, variants, ranges, callsSampleName, extractor, outdir, zip, slope, rtgStats);
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfHeader bh = variants.baseLineHeader().copy();
    UnifiedEvalSynchronizer.addInfoHeaders(bh, VariantSetType.BASELINE);
    mBase = new VcfWriter(bh, new File(outdir, "baseline.vcf" + zipExt), null, zip, true);
    final VcfHeader ch = variants.calledHeader().copy();
    UnifiedEvalSynchronizer.addInfoHeaders(ch, VariantSetType.CALLS);
    mCalls = new VcfWriter(ch, new File(outdir, "calls.vcf" + zipExt), null, zip, true);
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
    // No-op
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
    // No-op
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    setNewInfoFields(mBrv, updateForBaseline(true, new LinkedHashMap<String, String>()));
    mBase.write(mBrv);
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    setNewInfoFields(mCrv, updateForCall(true, new LinkedHashMap<String, String>()));
    mCalls.write(mCrv);
  }

  @Override
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    if (unknownBaseline) {
      setNewInfoFields(mBrv, updateForBaseline(true, new LinkedHashMap<String, String>()));
      mBase.write(mBrv);
      mBrv = null;
    }
    if (unknownCall) {
      setNewInfoFields(mCrv, updateForCall(true, new LinkedHashMap<String, String>()));
      mCalls.write(mCrv);
      mCrv = null;
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    setNewInfoFields(mBrv, updateForBaseline(false, new LinkedHashMap<String, String>()));
    mBase.write(mBrv);
  }

  @Override
  protected void handleKnownCall() throws IOException {
    setNewInfoFields(mCrv, updateForCall(false, new LinkedHashMap<String, String>()));
    mCalls.write(mCrv);
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    // Just deal with the call side first, and let the baseline call take care of itself
    handleKnownCall();
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mBase;
         VcfWriter ignored2 = mCalls) {
      // done for nice closing side effects
    }
  }
}
