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
import java.util.Map;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Support output files with INFO field annotations containing variant status.
 */
abstract class WithInfoEvalSynchronizer extends InterleavingEvalSynchronizer {

  static final String INFO_BASE = "BASE";
  static final String INFO_CALL = "CALL";
  static final String INFO_SYNCPOS = "SYNC";
  static final String INFO_CALL_WEIGHT = "CALL_WEIGHT";
  static final String STATUS_IGNORED = "IGN";
  static final String STATUS_HARD = "HARD";
  static final String STATUS_TP = "TP";
  static final String STATUS_FN = "FN";
  static final String STATUS_FP = "FP";
  static final String STATUS_FN_CA = "FN_CA";
  static final String STATUS_FP_CA = "FP_CA";

  private final RocContainer mRoc;
  private final RocSortValueExtractor mRocExtractor;
  private final boolean mZip;
  private final boolean mSlope;
  private final File mOutDir;
  private int mBaselineTruePositives = 0;
  protected final int mCallSampleNo;
  private int mCallTruePositives = 0;
  private int mFalseNegatives = 0;
  private int mFalsePositives = 0;
  private int mFalseNegativesCommonAllele = 0;
  private int mFalsePositivesCommonAllele = 0;
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;

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
  WithInfoEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                           String callsSampleName, RocSortValueExtractor extractor,
                           File outdir, boolean zip, boolean slope, boolean rtgStats) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);
    final RocContainer roc = new RocContainer(extractor.getSortOrder(), extractor.toString());
    roc.addStandardFilters();
    if (rtgStats) {
      roc.addExtraFilters();
    }
    mRoc = roc;
    mRocExtractor = extractor;
    mZip = zip;
    mSlope = slope;
    mOutDir = outdir;
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");
  }

  static void addInfoHeaders(VcfHeader header, VariantSetType type) {
    header.addInfoField(INFO_SYNCPOS, MetaType.INTEGER, VcfNumber.DOT, "Chromosome-unique sync region ID. When IDs differ for baseline/call, both will be listed.");
    if (type == null || type == VariantSetType.BASELINE) {
      header.addInfoField(INFO_BASE, MetaType.STRING, new VcfNumber("1"), "Baseline genotype status");
    }
    if (type == null || type == VariantSetType.CALLS) {
      header.addInfoField(INFO_CALL, MetaType.STRING, new VcfNumber("1"), "Call genotype status");
      header.addInfoField(INFO_CALL_WEIGHT, MetaType.FLOAT, new VcfNumber("1"), "Call weight (equivalent number of baseline variants). When unspecified, assume 1.0");
    }
  }

  protected Map<String, String> updateForCall(boolean unknown, Map<String, String> newInfo) {
    final String status;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync;
      String weight = null;
      double w;
      final byte s = mCv.getStatus();
      switch (s) {
        case VariantId.STATUS_SKIPPED:
          status = STATUS_HARD;
          sync = null;
          break;
        case VariantId.STATUS_GT_MATCH:
          mCallTruePositives++;
          w = ((OrientedVariant) mCv).getWeight();
          if (Math.abs(w - 1.0) > 0.001) {
            weight = String.format("%.3g", w);
          }
          addToROCContainer(w, s);
          status = STATUS_TP;
          sync = Integer.toString(mCSyncStart + 1);
          break;
        case VariantId.STATUS_ALLELE_MATCH:
          mFalsePositivesCommonAllele++;
          w = ((OrientedVariant) mCv).getWeight();
          if (Math.abs(w - 1.0) > 0.001) {
            weight = String.format("%.3g", w);
          }
          addToROCContainer(w, s);
          status = STATUS_FP_CA;
          sync = Integer.toString(mCSyncStart2 + 1);
          break;
        case VariantId.STATUS_NO_MATCH:
          mFalsePositives++;
          addToROCContainer(0, s);
          status = STATUS_FP;
          sync = mCSyncStart2 > 0 ? Integer.toString(mCSyncStart2 + 1) : Integer.toString(mCSyncStart + 1);
          break;
        default:
          throw new RuntimeException("Unhandled variant status during postprocessing: " + mCv.getStatus() + " cv:" + mCv);
      }
      if (sync != null) {
        final String oldSync = newInfo.get(INFO_SYNCPOS);
        newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (oldSync + VcfRecord.ALT_CALL_INFO_SEPARATOR + sync));
      }
      if (weight != null) {
        newInfo.put(INFO_CALL_WEIGHT, weight);
      }
    }
    newInfo.put(INFO_CALL, status);
    return newInfo;
  }

  protected Map<String, String> updateForBaseline(boolean unknown, Map<String, String> newInfo) {
    final String status;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync;
      switch (mBv.getStatus()) {
        case VariantId.STATUS_SKIPPED:
          status = STATUS_HARD;
          sync = null;
          break;
        case VariantId.STATUS_GT_MATCH:
          mBaselineTruePositives++;
          status = STATUS_TP;
          sync = Integer.toString(mBSyncStart + 1);
          break;
        case VariantId.STATUS_ALLELE_MATCH:
          mFalseNegativesCommonAllele++;
          status = STATUS_FN_CA;
          sync = Integer.toString(mBSyncStart2 + 1);
          break;
        case VariantId.STATUS_NO_MATCH:
          mFalseNegatives++;
          status = STATUS_FN;
          sync = mBSyncStart2 > 0 ? Integer.toString(mBSyncStart2 + 1) : Integer.toString(mBSyncStart + 1);
          break;
        default:
          throw new RuntimeException("Unhandled variant status during postprocessing: " + mBv.getStatus() + " bv:" + mBv);
      }
      if (sync != null) {
        final String oldSync = newInfo.get(INFO_SYNCPOS);
        newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (sync + VcfRecord.ALT_CALL_INFO_SEPARATOR + oldSync));
      }
    }
    newInfo.put(INFO_BASE, status);
    return newInfo;
  }

  @Override
  protected void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    mMisPhasings += misPhasings;
    mUnphasable += unphasable;
    mCorrectPhasings += correctPhasings;
  }

  protected void addToROCContainer(double weight, byte status) {
    final EnumSet<RocFilter> filters = EnumSet.noneOf(RocFilter.class);
    for (final RocFilter filter : RocFilter.values()) {
      if (filter.accept(mCrv, mCallSampleNo)) {
        filters.add(filter);
      }
    }
    double score = Double.NaN;
    try {
      score = mRocExtractor.getSortValue(mCrv, mCallSampleNo);
    } catch (IndexOutOfBoundsException ignored) {
    }
    if (status == VariantId.STATUS_ALLELE_MATCH) {
      mRoc.addRocLine(score, 0, filters);
    } else {
      mRoc.addRocLine(score, weight, filters);
    }
  }

  int getUnphasable() {
    return mUnphasable;
  }

  int getMisPhasings() {
    return mMisPhasings;
  }

  int getCorrectPhasings() {
    return mCorrectPhasings;
  }


  @Override
  void finish() throws IOException {
    super.finish();
    final int strictFp = mFalsePositives + mFalsePositivesCommonAllele;
    final int strictFn = mFalseNegatives + mFalseNegativesCommonAllele;
    mRoc.writeRocs(mOutDir, mBaselineTruePositives, strictFp, strictFn, mZip, mSlope);
    writePhasingInfo();
    mRoc.writeSummary(new File(mOutDir, CommonFlags.SUMMARY_FILE), mBaselineTruePositives, strictFp, strictFn);
  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }

  protected void setNewInfoFields(VcfRecord rec, Map<String, String> newInfo) {
    for (Map.Entry<String, String> e : newInfo.entrySet()) {
      rec.addInfo(e.getKey(), e.getValue());
    }
  }
}
