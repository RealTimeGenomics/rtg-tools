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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rtg.launcher.CommonFlags;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfAltCleaner;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates typical vcfeval output files with separate VCF files and ROC files.
 */
class UnifiedEvalSynchronizer extends MergingEvalSynchronizer {

  private static final String OUTPUT_FILE_NAME = "output.vcf";
  private static final String INFO_BASE = "BASE";
  private static final String INFO_CALL = "CALL";
  private static final String INFO_SYNCPOS = "SYNC";
  private static final String INFO_CALL_WEIGHT = "CALL_WEIGHT";
  private static final String STATUS_IGNORED = "IGN";
  private static final String STATUS_HARD = "HARD";
  private static final String STATUS_TP = "TP";
  private static final String STATUS_FN = "FN";
  private static final String STATUS_FP = "FP";
  private static final String SAMPLE_BASELINE = "BASELINE";
  private static final String SAMPLE_CALLS = "CALLS";

  private final VcfWriter mVcfOut;
  private final RocContainer mRoc;
  private final RocSortValueExtractor mRocExtractor;
  private final int mBaselineSampleNo;
  private final int mCallSampleNo;
  private final boolean mZip;
  private final boolean mSlope;
  private final File mOutDir;
  private final VcfHeader mOutHeader;
  private final VcfRecord[] mInRecs = new VcfRecord[2];
  private final VcfHeader[] mInHeaders = new VcfHeader[2];
  private final VcfAltCleaner mAltCleaner = new VcfAltCleaner();
  private int mBaselineTruePositives = 0;
  int mCallTruePositives = 0;
  int mFalseNegatives = 0;
  int mFalsePositives = 0;
  private int mUnphasable = 0;
  private int mMisPhasings = 0;
  private int mCorrectPhasings = 0;

  /**
   * @param baseLineFile tabix indexed base line VCF file
   * @param callsFile tabix indexed calls VCF file
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param baselineSampleName the name of the sample used in the baseline
   * @param callsSampleName the name of the sample used in the calls
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param rtgStats true to output additional ROC curves for RTG specific attributes
   * @throws IOException if there is a problem opening output files
   */
  UnifiedEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                          String baselineSampleName, String callsSampleName,
                          RocSortValueExtractor extractor,
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
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mOutHeader = new VcfHeader();
    mOutHeader.addCommonHeader();
    mOutHeader.addContigFields(variants.baseLineHeader());
    mOutHeader.addInfoField(INFO_BASE, MetaType.STRING, new VcfNumber("1"), "Baseline genotype status");
    mOutHeader.addInfoField(INFO_CALL, MetaType.STRING, new VcfNumber("1"), "Call genotype status");
    mOutHeader.addInfoField(INFO_SYNCPOS, MetaType.INTEGER, VcfNumber.DOT, "Chromosome-unique sync region ID. When IDs differ for baseline/call, both will be listed.");
    mOutHeader.addInfoField(INFO_CALL_WEIGHT, MetaType.FLOAT, new VcfNumber("1"), "Call weight (equivalent number of baseline variants). When unspecified, assume 1.0");
    mOutHeader.addFormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, new VcfNumber("1"), "Genotype");
    mOutHeader.addSampleName(SAMPLE_BASELINE);
    mOutHeader.addSampleName(SAMPLE_CALLS);
    mInHeaders[0] = variants.baseLineHeader().copy();
    mInHeaders[0].removeAllSamples();
    mInHeaders[0].addSampleName(SAMPLE_BASELINE);
    mInHeaders[1] = variants.calledHeader().copy();
    mInHeaders[1].removeAllSamples();
    mInHeaders[1].addSampleName(SAMPLE_CALLS);
    mVcfOut = new VcfWriter(mOutHeader, new File(outdir, OUTPUT_FILE_NAME + zipExt), null, zip, true);
    mBaselineSampleNo = VcfUtils.getSampleIndexOrDie(variants.baseLineHeader(), baselineSampleName, "baseline");
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");
  }

  @Override
  protected void addPhasingCountsInternal(int misPhasings, int correctPhasings, int unphasable) {
    mMisPhasings += misPhasings;
    mUnphasable += unphasable;
    mCorrectPhasings += correctPhasings;
  }

  @Override
  protected void resetBaselineRecordFields(VcfRecord rec) {
  }

  @Override
  protected void resetCallRecordFields(VcfRecord rec) {
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    writeBaseline(updateForBaseline(true, new LinkedHashMap<String, String>()));
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    writeCall(updateForCall(true, new LinkedHashMap<String, String>()));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    writeCall(updateForCall(false, new LinkedHashMap<String, String>()));
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    writeBaseline(updateForBaseline(false, new LinkedHashMap<String, String>()));
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    final HashMap<String, String> newInfo = new LinkedHashMap<>();
    updateForBaseline(false, newInfo);
    updateForCall(false, newInfo);
    writeBoth(newInfo);
    mBv = null;
    mBrv = null;
    mCv = null;
    mCrv = null;
  }

  @Override
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    final HashMap<String, String> newInfo = new LinkedHashMap<>();
    updateForBaseline(unknownBaseline, newInfo);
    updateForCall(unknownCall, newInfo);
    writeBoth(newInfo);
    mBrv = null;
    mCrv = null;
    if (!unknownBaseline) {
      mBv = null;
    }
    if (!unknownCall) {
      mCv = null;
    }
  }


  private Map<String, String> updateForCall(boolean unknown, Map<String, String> newInfo) {
    final String status;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync = Integer.toString(mCSyncStart + 1);
      final String oldSync = newInfo.get(INFO_SYNCPOS);
      newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (oldSync + VcfRecord.ALT_CALL_INFO_SEPARATOR + sync));
      if (mCv instanceof OrientedVariant) {
        mCallTruePositives++;
        final double weight = ((OrientedVariant) mCv).getWeight();
        if (Math.abs(weight - 1.0) > 0.001) {
          newInfo.put(INFO_CALL_WEIGHT, String.format("%.3g", weight));
        }
        addToROCContainer(weight);
        status = STATUS_TP;
      } else if (mCv instanceof SkippedVariant) {
        status = STATUS_HARD;
      } else {
        mFalsePositives++;
        addToROCContainer(0);
        status = STATUS_FP;
      }
    }
    newInfo.put(INFO_CALL, status);
    return newInfo;
  }

  private Map<String, String> updateForBaseline(boolean unknown, Map<String, String> newInfo) {
    final String status;
    if (unknown) {
      status = STATUS_IGNORED;
    } else {
      final String sync = Integer.toString(mBSyncStart + 1);
      final String oldSync = newInfo.get(INFO_SYNCPOS);
      newInfo.put(INFO_SYNCPOS, oldSync == null || oldSync.equals(sync) ? sync : (sync + VcfRecord.ALT_CALL_INFO_SEPARATOR + oldSync));
      if (mBv instanceof OrientedVariant) {
        mBaselineTruePositives++;
        status = STATUS_TP;
      } else if (mCv instanceof SkippedVariant) {
        status = STATUS_HARD;
      } else {
        mFalseNegatives++;
        status = STATUS_FN;
      }
    }
    newInfo.put(INFO_BASE, status);
    return newInfo;

  }

  protected void writeCall(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mCrv, mCallSampleNo, 1);
    for (Map.Entry<String, String> e : newInfo.entrySet()) {
      mCrv.addInfo(e.getKey(), e.getValue());
    }
    mAltCleaner.annotate(mCrv);
    mVcfOut.write(mCrv);
  }

  protected void writeBaseline(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mBrv, mBaselineSampleNo, 0);
    for (Map.Entry<String, String> e : newInfo.entrySet()) {
      mBrv.addInfo(e.getKey(), e.getValue());
    }
    mAltCleaner.annotate(mBrv);
    mVcfOut.write(mBrv);
  }

  protected void writeBoth(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mBrv, mBaselineSampleNo, -1);
    resetRecordFields(mCrv, mCallSampleNo, -1);
    mInRecs[0] = mBrv;
    mInRecs[1] = mCrv;
    final VcfRecord rec = VcfRecord.mergeRecordsWithSameRef(mInRecs, mInHeaders, mOutHeader, Collections.<String>emptySet(), false);
    for (Map.Entry<String, String> e : newInfo.entrySet()) {
      rec.addInfo(e.getKey(), e.getValue());
    }
    mAltCleaner.annotate(rec);
    mVcfOut.write(rec);
  }

  private void resetRecordFields(VcfRecord rec, int sampleIndex, int destPos) { // Collapse to a minimal record for easier merging / output
    rec.setId();
    rec.setQuality(null);
    rec.getFilters().clear();
    rec.getInfo().clear();
    final String gtStr = VcfUtils.getValidGtStr(rec, sampleIndex);
    rec.getFormatAndSample().clear();
    if (destPos == -1) {
      rec.setNumberOfSamples(1);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gtStr);
    } else {
      rec.setNumberOfSamples(2);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? gtStr : VcfUtils.MISSING_FIELD);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? VcfUtils.MISSING_FIELD : gtStr);
    }
  }

  private void addToROCContainer(double weight) {
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
    mRoc.addRocLine(score, weight, filters);
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
    mRoc.writeRocs(mOutDir, mBaselineTruePositives, mFalsePositives, mFalseNegatives, mZip, mSlope);
    writePhasingInfo();
    mRoc.writeSummary(new File(mOutDir, CommonFlags.SUMMARY_FILE), mBaselineTruePositives, mFalsePositives, mFalseNegatives);
  }

  private void writePhasingInfo() throws IOException {
    final File phasingFile = new File(mOutDir, "phasing.txt");
    FileUtils.stringToFile("Correct phasings: " + getCorrectPhasings() + StringUtils.LS
      + "Incorrect phasings: " + getMisPhasings() + StringUtils.LS
      + "Unresolvable phasings: " + getUnphasable() + StringUtils.LS, phasingFile);
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mVcfOut) {
      // done for nice closing side effects
    }
  }
}
