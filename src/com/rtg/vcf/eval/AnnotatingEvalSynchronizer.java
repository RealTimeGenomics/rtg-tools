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
import java.util.Set;

import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.VcfHeader;

/**
 * Outputs <code>baseline.vcf</code> and <code>calls.vcf</code>, each as original VCF records with additional status annotations.
 */
class AnnotatingEvalSynchronizer extends WithInfoEvalSynchronizer {

  private final VcfWriter mBase;
  protected final VcfWriter mCalls;

  /**
   * @param variants the set of variants to evaluate
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @param rocCriteria criteria for selecting a favoured ROC point
   * @throws IOException if there is a problem opening output files
   */
  AnnotatingEvalSynchronizer(VariantSet variants,
                             RocSortValueExtractor extractor,
                             File outdir, boolean zip, boolean slope, boolean dualRocs, Set<RocFilter> rocFilters, RocPointCriteria rocCriteria) throws IOException {
    super(variants, extractor, outdir, zip, slope, dualRocs, rocFilters, rocCriteria);
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfHeader bh = variants.baselineHeader().copy();
    final VcfWriterFactory vf = new VcfWriterFactory().zip(zip).addRunInfo(true);
    CombinedEvalSynchronizer.addInfoHeaders(bh, VariantSetType.BASELINE);
    mBase = vf.make(bh, new File(outdir, "baseline.vcf" + zipExt));
    final VcfHeader ch = variants.calledHeader().copy();
    CombinedEvalSynchronizer.addInfoHeaders(ch, VariantSetType.CALLS);
    mCalls = vf.make(ch, new File(outdir, "calls.vcf" + zipExt));
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    setNewInfoFields(mBrv, updateForBaseline(true, new LinkedHashMap<>()));
    mBase.write(mBrv);
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    setNewInfoFields(mCrv, updateForCall(true, new LinkedHashMap<>()));
    mCalls.write(mCrv);
  }

  @Override
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    if (unknownBaseline) {
      setNewInfoFields(mBrv, updateForBaseline(true, new LinkedHashMap<>()));
      mBase.write(mBrv);
      mBrv = null;
    }
    if (unknownCall) {
      setNewInfoFields(mCrv, updateForCall(true, new LinkedHashMap<>()));
      mCalls.write(mCrv);
      mCrv = null;
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    setNewInfoFields(mBrv, updateForBaseline(false, new LinkedHashMap<>()));
    mBase.write(mBrv);
  }

  @Override
  protected void handleKnownCall() throws IOException {
    setNewInfoFields(mCrv, updateForCall(false, new LinkedHashMap<>()));
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
    // Try-with-resources for nice closing side effects
    try (VcfWriter ignored = mBase;
         VcfWriter ignored2 = mCalls) {
      super.close();
    }
  }
}
