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

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 * Outputs a new sample genotype with respect to the population alleles
 */
public class SampleRecoder extends MergingEvalSynchronizer {

  protected final VcfWriter mAlleles;
  protected final VcfWriter mAuxiliary;
  protected int mCalledNotInPath;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param output the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @throws IOException if there is a problem opening output files
   */
  SampleRecoder(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges, File output, boolean zip) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfHeader h = variants.baseLineHeader().copy();
    h.removeAllSamples();
    mAlleles = new VcfWriter(h, new File(output, "alleles.vcf" + zipExt), null, zip, true); // Contains new population alleles (old + new sample alleles)
    mAuxiliary = new VcfWriter(h, new File(output, "aux.vcf" + zipExt), null, zip, true); // Contains sample calls that were matched (i.e. redundant alleles)
  }

  @Override
  protected void resetRecordFields(VcfRecord rec) {
    rec.removeInfo("STATUS");
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
      mAlleles.write(mCrv);
    } else {
      // Excluded, but we don't have a baseline call at this position.
      // This should not happen if we have accumulated alleles
      throw new IllegalStateException("C-NotInPath - should not happen. Record: " + mCrv.toString());
//        assert mCv instanceof AlleleIdVariant || mCv instanceof SkippedVariant;
//        final String label = mCv instanceof AlleleIdVariant ? "-FP" : "-TooHard";
//        mCrv.addInfo("STATUS", "C" + label + "=" + mCv.toString());
//        mAuxiliary.write(mCrv);
    }
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    if (mBv instanceof OrientedVariant) {
      // Normal status if the sample contains this variant
      // Add the appropriate GT and output the record
      final OrientedVariant ov = (OrientedVariant) mBv;
      mBrv.getFormatAndSample().clear();
      mBrv.setNumberOfSamples(1);
      final String gt = "" + encode(ov.alleleId()) + VcfUtils.UNPHASED_SEPARATOR + encode(ov.other().alleleId());
      mBrv.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gt);
      mBrv.addInfo("STATUS", "B-TP=" + mBv.toString());
      mAlleles.write(mBrv);
    } else if (mBv instanceof SkippedVariant) {
      // Expected sometimes, do nothing here (a relevant call will already have been output if needed during processBoth using its allele representation).
      mBrv.addInfo("STATUS", "B-TooHard");
      mAuxiliary.write(mBrv);
    } else {
      // Excluded, this population variant is not in this sample, no need to output to primary.
      mBrv.addInfo("STATUS", "B-NotInSample");
      mAuxiliary.write(mBrv);
    }
  }

  private String encode(int id) {
    return id == -1 ? VcfUtils.MISSING_FIELD : String.valueOf(id);
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    if (mCv instanceof OrientedVariant) {
      // Normal scenario where sample matches population alleles directly
      // Elsewhere we'll output the baseline version, but make a note here.
      mCrv.addInfo("STATUS", "C-TP-BSame"); // We expect many of these, probably fine to silently drop
      mAuxiliary.write(mCrv);
    } else if (mCv instanceof SkippedVariant) {
      // Happens in TooHard regions.
      // Output a record using the current GT from this record
      mCrv.addInfo("STATUS", "C-TooHard");
      mAlleles.write(mCrv);
    } else {
      // Excluded. This shouldn't happen except where the sample is self-inconsistent.
      // What To Do?
      assert mCv instanceof AlleleIdVariant || mCv instanceof SkippedVariant;
      mCrv.addInfo("STATUS", "C-Inconsistent");
      mAuxiliary.write(mCrv);
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
  void finish() throws IOException {
    super.finish();
    if (mCalledNotInPath > 0) {
      Diagnostic.userLog("There were " + mCalledNotInPath + " call records not in the path");
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mAlleles;
         VcfWriter ignored2 = mAuxiliary) {
      // done for nice closing side effects
    }
  }
}
