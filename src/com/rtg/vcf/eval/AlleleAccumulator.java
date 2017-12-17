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
import java.util.Collections;
import java.util.List;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Output a population alleles VCF that incorporates a new sample, including any new alleles
 * contained only in the call-set.
 * Path finding should use variant factory <code>dip-alt,default-trim-id</code>
 */
public class AlleleAccumulator extends InterleavingEvalSynchronizer {

  protected final VcfWriter mAlleles;
  protected final VcfWriter mAuxiliary;
  protected int mCalledNotInPath;
  protected int mBaselineNotInPath;


  /**
   * @param variants the set of variants to evaluate
   * @param output the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @throws IOException if there is a problem opening output files
   */
  AlleleAccumulator(VariantSet variants, File output, boolean zip) throws IOException {
    super(variants);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    VcfHeader h = variants.baselineHeader().copy();
    h.ensureContains(new InfoField("STATUS", MetaType.STRING, VcfNumber.DOT, "Allele accumulation status"));
    mAuxiliary = new VcfWriterFactory().zip(zip).addRunInfo(true).make(h, new File(output, "auxiliary.vcf" + zipExt)); // Contains sample calls that were matched (i.e. redundant alleles)
    h = h.copy();
    h.removeAllSamples();
    mAlleles = new VcfWriterFactory().zip(zip).addRunInfo(true).make(h, new File(output, "alleles.vcf" + zipExt)); // Contains new population alleles (old + new sample alleles)
  }

  protected void resetRecordFields(VcfRecord rec) {
    rec.setId();
    rec.setQuality(null);
    rec.getFilters().clear();
    rec.getInfo().clear();
    rec.removeSamples();
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
    resetRecordFields(rec);
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
    resetRecordFields(rec);
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    mBrv.setInfo("STATUS", "B-NotInPath"); // Should never happen for us.
    mAlleles.write(mBrv);
    ++mBaselineNotInPath;
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    mCrv.setInfo("STATUS", "C-NotInPath"); // Was skipped during loading, probably OK to just silently drop.
    mAuxiliary.write(mCrv);
    ++mCalledNotInPath;
  }

  @Override
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    // Deal with both as though they were at independent positions
    if (unknownBaseline) {
      handleUnknownBaseline();
      mBrv = null;
    }
    if (unknownCall) {
      handleUnknownCall();
      mCrv = null;
    }
  }

  @Override
  protected void handleKnownCall() throws IOException {
    if (mCv instanceof OrientedVariant) { // Included but the baseline was at a different position. This is interesting
      mCrv.addInfo("STATUS", "C-TP-BDiff=" + mCv);
      mAuxiliary.write(mCrv);
    } else if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) { // Too-hard, output this variant with just the used alleles
      final GtIdVariant v = (GtIdVariant) mCv;
      writeNonRedundant(v, "C-TooHard=" + mCv);
    } else { // Excluded (novel or self-inconsistent)
      assert mCv instanceof GtIdVariant;
      final GtIdVariant v = (GtIdVariant) mCv;
      writeNonRedundant(v, "C-FP=" + mCv);
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    final String status = (mBv instanceof OrientedVariant)
      ? "B-TP=" + mBv
      : (mBv.hasStatus(VariantId.STATUS_SKIPPED)) ? "B-TooHard" : "B-FN";
    mBrv.addInfo("STATUS", status);
    mAlleles.write(mBrv);
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    if (mCv instanceof OrientedVariant) {
      mCrv.addInfo("STATUS", "C-TP-BSame=" + mCv);
      mAuxiliary.write(mCrv);
    } else if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) { // Too hard, merge records into b, adding any new ALT from c, flush c
      final GtIdVariant ov = (GtIdVariant) mCv;
      mergeIntoBaseline(ov, "C-TooHard");
    } else { // Excluded, merge records into b, adding any new ALT from c, flush c
      assert mCv instanceof GtIdVariant;
      final GtIdVariant ov = (GtIdVariant) mCv;
      mergeIntoBaseline(ov, "C-FP");
    }
  }

  protected void writeNonRedundant(GtIdVariant v, String status) throws IOException {
    mCrv.addInfo("STATUS", status);
    final List<String> newAlts = new ArrayList<>();
    addCallAllele(newAlts, v.alleleA());
    if (v.alleleA() != v.alleleB()) {
      addCallAllele(newAlts, v.alleleB());
    }
    mCrv.getAltCalls().clear();
    mCrv.getAltCalls().addAll(newAlts);
    Collections.sort(mCrv.getAltCalls());
    mAlleles.write(mCrv);
  }

  protected void mergeIntoBaseline(GtIdVariant v, String status) throws IOException {
    boolean merged = addCallAlleleToBaseline(v.alleleA());
    if (v.alleleA() != v.alleleB()) {
      merged |= addCallAlleleToBaseline(v.alleleB());
    }
    if (merged) {
      Collections.sort(mBrv.getAltCalls());
    }
    mCrv.addInfo("STATUS", status + (merged ? "-Merged" : "-BSame")); // If merged, interesting, we added a new allele from this sample to existing site
    mAuxiliary.write(mCrv);
  }

  private boolean addCallAlleleToBaseline(int gtId) {
    if (gtId > 0) {
      final String alt = mCrv.getAltCalls().get(gtId - 1);
      if (!mBrv.getAltCalls().contains(alt)) {
        mBrv.addAltCall(alt);
        mBrv.addInfo("STATUS", "B-Merged-" + alt); // Baseline counterpart to C-FP-Merged
        return true;
      }
    }
    return false;
  }

  void addCallAllele(List<String> newAlts, int gtId) {
    if (gtId > 0) {
      final String alt = mCrv.getAltCalls().get(gtId - 1);
      if (!newAlts.contains(alt)) {
        newAlts.add(alt);
      }
    }
  }

  @Override
  void finish() throws IOException {
    super.finish();
    if (mBaselineNotInPath > 0) {
      Diagnostic.userLog("There were " + mBaselineNotInPath + " baseline records not in the path");
    }
    if (mCalledNotInPath > 0) {
      Diagnostic.userLog("There were " + mCalledNotInPath + " call records not in the path");
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    // Try-with-resources for nice closing side effects
    try (VcfWriter ignored = mAlleles;
         VcfWriter ignored2 = mAuxiliary) {
      super.close();
    }
  }
}
