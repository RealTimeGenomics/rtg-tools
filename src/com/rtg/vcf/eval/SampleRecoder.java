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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Outputs a new sample genotype with respect to the population alleles
 */
@TestClass("com.rtg.vcf.eval.AlleleAccumulatorTest")
public class SampleRecoder extends MergingEvalSynchronizer {

  protected final VcfWriter mSampleVcf;
  protected final VcfWriter mAuxiliary;
  protected int mCalledNotInPath;
  protected int mCallSampleIndex;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param output the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param sampleName the name of the sample being recoded
   * @throws IOException if there is a problem opening output files
   */
  SampleRecoder(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges, File output, boolean zip, String sampleName) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mCallSampleIndex = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), sampleName, VariantSetType.CALLS.label());
    final VcfHeader h = variants.calledHeader().copy();
    h.ensureContains(new InfoField("STATUS", MetaType.STRING, VcfNumber.DOT, "Recoding variant status"));
    if (h.getNumberOfSamples() != 1) {
      h.getSampleNames().clear();
      h.addSampleName(sampleName == null ? "SAMPLE" : sampleName);
    }
    mSampleVcf = new VcfWriter(h, new File(output, "sample.vcf" + zipExt), null, zip, true); // Primary output containing new representation of sample using population alleles
    mAuxiliary = new VcfWriter(h, new File(output, "aux.vcf" + zipExt), null, zip, true);
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
    rec.getInfo().clear();
    rec.setNumberOfSamples(1);
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
    rec.setId();
    rec.setQuality(null);
    rec.getFilters().clear();
    rec.getInfo().clear();
  }

  @Override
  protected void handleKnownCall() throws IOException {
    if (mCv instanceof OrientedVariant) {
      // Included but the baseline was at a different position.
      // Good, evidence that we are recoding to new representation when needed
      mCrv.addInfo("STATUS", "C-TP-BDiff");
      mAuxiliary.write(mCrv);
    } else if (mCv instanceof SkippedVariant) {
      // We don't have a baseline record at this position -- during allele accumulation this record was determined to be redundant / equivalent to other variants.
      // We have a good indication that it should have been possible to simplify this if the region were not too hard
      mCrv.addInfo("STATUS", "C-TooHard-BDiff");
      normalize(mCrv, mCallSampleIndex);
      mSampleVcf.write(mCrv);
    } else {
      // Excluded, but we don't have a baseline call at this position.
      // This should not normally happen if we have accumulated all alleles
      // Output the original representation
      mCrv.addInfo("STATUS", "C-FP=" + mCv.toString());
      normalize(mCrv, mCallSampleIndex);
      mSampleVcf.write(mCrv);
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    if (mBv instanceof OrientedVariant) {
      // Normal status if the sample contains this variant
      // Add the appropriate GT and output the record
      final OrientedVariant ov = (OrientedVariant) mBv;
      mBrv.getFormatAndSample().clear();
      mBrv.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.joinGt(false, ov.alleleId(), ov.other().alleleId()));
      mBrv.addInfo("STATUS", "B-TP=" + mBv.toString());
      normalize(mBrv, 0);
      mSampleVcf.write(mBrv);
    } else if (mBv instanceof SkippedVariant) {
      // Expected sometimes, do nothing here (a relevant call will already have been output if needed during processBoth using its allele representation).
      mBrv.addInfo("STATUS", "B-TooHard");
      mBrv.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.MISSING_FIELD);
      mAuxiliary.write(mBrv);
    } else {
      // Excluded, this population variant is not in this sample, no need to output to primary.
      mBrv.addInfo("STATUS", "B-NotInSample");
      mBrv.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.MISSING_FIELD);
      mAuxiliary.write(mBrv);
    }
  }

  protected static void normalize(VcfRecord rec, int sampleIndex) {
    final int[] gt = VcfUtils.getValidGt(rec, sampleIndex);
    Arrays.sort(gt);
    int lastId = -1;
    final List<String> newAlts = new ArrayList<>();
    for (int id : gt) {
      if (id > 0 && id != lastId) {
        newAlts.add(rec.getAllele(id));
      }
      lastId = id;
    }
    Collections.sort(newAlts);
    for (int i = 0; i < gt.length; i++) {
      if (gt[i] > 0) {
        gt[i] = newAlts.indexOf(rec.getAllele(gt[i])) + 1;
      }
    }
    Arrays.sort(gt);
    rec.getAltCalls().clear();
    rec.getAltCalls().addAll(newAlts);
    rec.getFormatAndSample().clear();
    rec.setNumberOfSamples(1);
    rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, VcfUtils.joinGt(false, gt));
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    if (mCv instanceof OrientedVariant) {
      // Normal scenario where sample matches population alleles directly
      // Elsewhere we'll output the baseline version, but make a note here.
      // We expect many of these, probably fine to silently drop, but interesting to see the status
      final String status = (mBv instanceof OrientedVariant) ? "C-TP-BSame" : (mBv instanceof SkippedVariant) ? "C-TP-BSkipped" : "C-TP-BSwitched";
      mCrv.addInfo("STATUS", status);
      mAuxiliary.write(mCrv);
    } else if (mCv instanceof SkippedVariant) {
      // Happens in TooHard regions.
      // Output the original representation
      mCrv.addInfo("STATUS", "C-TooHard");
      normalize(mCrv, mCallSampleIndex);
      mSampleVcf.write(mCrv);
    } else {
      // Excluded. This shouldn't happen except where the sample is self-inconsistent or perhaps the population allele accumulation had to drop the site.
      // Output the original representation
      mCrv.addInfo("STATUS", "C-Inconsistent");
      normalize(mCrv, mCallSampleIndex);
      mSampleVcf.write(mCrv);
    }
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    // Was skipped during loading (e.g. record was same-as-ref)
    // OK to just silently drop.
    mCrv.setInfo("STATUS", "C-NotInPath");
    mAuxiliary.write(mCrv);
    mCalledNotInPath++;
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    throw new IllegalStateException("B-NotInPath - should not happen. Record: " + mBrv.toString());
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
  void finish() throws IOException {
    super.finish();
    if (mCalledNotInPath > 0) {
      Diagnostic.userLog("There were " + mCalledNotInPath + " call records not in the path");
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mSampleVcf;
         VcfWriter ignored2 = mAuxiliary) {
      // done for nice closing side effects
    }
  }
}
