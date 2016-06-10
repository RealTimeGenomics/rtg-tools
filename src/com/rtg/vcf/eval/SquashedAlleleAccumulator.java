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
import java.util.List;

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
 * Output a population alleles VCF that incorporates a new sample, including any new alleles
 * contained only in the call-set.
 * Path finding should use variant factory <code>dip-alt,squash-id</code>
 */
public class SquashedAlleleAccumulator extends AlleleAccumulator {

  protected final VcfWriter mAlternate;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param output the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @throws IOException if there is a problem opening output files
   */
  SquashedAlleleAccumulator(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges, File output, boolean zip) throws IOException {
    super(baseLineFile, callsFile, variants, ranges, output, zip);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfHeader h = variants.calledHeader().copy();
    h.ensureContains(new InfoField("STATUS", MetaType.STRING, VcfNumber.DOT, "Allele accumulation status"));
    mAlternate = new VcfWriter(h, new File(output, "alternate.vcf" + zipExt), null, zip, true); // Contains sample calls after subtraction of matched alleles from double-alt cases
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
    rec.setId();
    rec.setQuality(null);
    rec.getFilters().clear();
    rec.getInfo().clear();
    rec.getFormatAndSample().clear();
    rec.setNumberOfSamples(0);
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
    rec.getInfo().clear();
  }

  @Override
  protected void handleKnownCall() throws IOException {
    if (mCv instanceof OrientedVariant) { // Included but the baseline was at a different position. This is interesting
      mCrv.addInfo("STATUS", "C-TP-BDiff=" + mCv.toString());
      writeResidual((OrientedVariant) mCv);
    } else if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) { // Too-hard, output this variant with just the used alleles
      final GtIdVariant v = (GtIdVariant) mCv;
      writeNonRedundant(v, "C-TooHard=" + mCv.toString());
    } else { // Excluded (novel or self-inconsistent)
      assert mCv instanceof GtIdVariant;
      final GtIdVariant v = (GtIdVariant) mCv;
      writeNonRedundant(v, "C-FP=" + mCv.toString());
    }
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    if (mCv instanceof OrientedVariant) {
      mCrv.addInfo("STATUS", "C-TP-BSame=" + mCv.toString());
      writeResidual((OrientedVariant) mCv);
    } else if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) { // Too hard, merge records into b, adding any new ALT from c, flush c
      final GtIdVariant ov = (GtIdVariant) mCv;
      mergeIntoBaseline(ov, "C-TooHard");
    } else { // Excluded, merge records into b, adding any new ALT from c, flush c
      assert mCv instanceof GtIdVariant;
      final GtIdVariant ov = (GtIdVariant) mCv;
      mergeIntoBaseline(ov, "C-FP");
    }
  }

  private void writeResidual(OrientedVariant ov) throws IOException {
    // If the original variant contained multiple ALTs, null out the one used and then write to alternate, otherwise write to alternate as-is
    final GtIdVariant v = (GtIdVariant) ov.variant();
    int remaining = -1;
    int numAlts = 0;
    if (v.numAlleles() > 2) {
      for (int i = 1; i < v.numAlleles(); i++) {
        if (v.allele(i) != null) {
          numAlts++;
          if (i != ov.alleleId()) {
            if (remaining != -1) {
              throw new IllegalStateException("Cannot have two remaining ALT alleles");
            }
            remaining = i;
          }
        }
      }
    }
    if (numAlts > 1) {
      if (remaining == -1) {
        throw new IllegalStateException("Cannot have two " + numAlts + " alts without a remaining ALT alleles");
      }
      final List<String> alts = mCrv.getAltCalls();
      final String alt = alts.get(remaining - 1);
      alts.clear();
      alts.add(alt);
      mCrv.getFormatAndSample().clear();
      mCrv.setNumberOfSamples(1);
      mCrv.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, "1");
    }
    mAlternate.write(mCrv);
  }

  @Override
  protected void writeNonRedundant(GtIdVariant v, String status) throws IOException {
    mCrv.addInfo("STATUS", status);
    mAlternate.write(mCrv); // Write as-is to the alternate
    // Prepare for usual incorporation
    mCrv.getFormatAndSample().clear();
    mCrv.setNumberOfSamples(0);
    mCrv.setInfo("STATUS");
    super.writeNonRedundant(v, status);
  }

  @Override
  void finish() throws IOException {
    super.finish();
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mAlternate) {
      super.close();
    }
  }
}
