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
import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfNumber;

/**
 * Updates the GT value of any matched called variant records according to the phase used
 * during matching. The assumption is that the baseline has been fully phased, and this permits
 * transferring that phase information to an annotated call set.
 */
public class PhaseTransferEvalSynchronizer extends AnnotatingEvalSynchronizer {

  private static final String FORMAT_ORIGINAL_GT = "OGT";
  private static final String INFO_PHASE = "PHASE";

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
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @throws IOException if there is a problem opening output files
   */
  PhaseTransferEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges, String callsSampleName, RocSortValueExtractor extractor, File outdir, boolean zip, boolean slope, boolean dualRocs, EnumSet<RocFilter> rocFilters) throws IOException {
    super(baseLineFile, callsFile, variants, ranges, callsSampleName, extractor, outdir, zip, slope, dualRocs, rocFilters);
    mCalls.getHeader().ensureContains(new InfoField(INFO_PHASE, MetaType.CHARACTER, VcfNumber.ONE, "Phase of match, A = matched in same phase, B = matched in opposite phase"));
    mCalls.getHeader().ensureContains(new FormatField(FORMAT_ORIGINAL_GT, MetaType.STRING, VcfNumber.ONE, "Original pre-phasing genotype value"));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    setNewInfoFields(mCrv, updateForCall(false, new LinkedHashMap<String, String>()));
    if (mCv.hasStatus(VariantId.STATUS_GT_MATCH)) {
      assert mCv instanceof OrientedVariant;
      final OrientedVariant ov = (OrientedVariant) mCv;
      final String ogt = mCrv.getSampleString(mCallSampleNo, VcfUtils.FORMAT_GENOTYPE);
      final int[] gtArr = VcfUtils.splitGt(ogt);
      if (gtArr.length == 2 && !ov.isAlleleA()) {
        final int t = gtArr[0]; gtArr[0] = gtArr[1]; gtArr[1] = t;
      }
      final String ngt = VcfUtils.joinGt(true, gtArr);
      mCrv.setFormatAndSample(FORMAT_ORIGINAL_GT, ogt, mCallSampleNo);
      mCrv.setFormatAndSample(VcfUtils.FORMAT_GENOTYPE, ngt, mCallSampleNo);
      mCrv.addInfo(INFO_PHASE, ov.isAlleleA() ? "A" : "B");
    }
    mCalls.write(mCrv);
  }
}
