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

import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfAltCleaner;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates a single two-sample output VCF.
 */
class Ga4ghEvalSynchronizer extends InterleavingEvalSynchronizer {


  private static final String INFO_SUPERLOCUS_ID = "BS";
  private static final String INFO_CALL_WEIGHT = "CALL_WEIGHT";

  private static final String FORMAT_DECISION = "BD";
  private static final String DECISION_TP = "TP";
  private static final String DECISION_FN = "FN";
  private static final String DECISION_FP = "FP";
  private static final String DECISION_OTHER = "N";

  private static final String FORMAT_MATCH_KIND = "BK";
  private static final String SUBTYPE_MISMATCH = ".";
  private static final String SUBTYPE_GT_MATCH = "gm";
  private static final String SUBTYPE_ALLELE_MATCH = "am";
  private static final String SUBTYPE_REGIONAL_MATCH = "lm"; // perhaps implement some loose positional comparison?

  private static final String FORMAT_EXTRA = "BI";
  private static final String EXTRA_MULTI = "multi";
  private static final String EXTRA_TOO_HARD = "too-hard";

  private static final String FORMAT_ROC_SCORE = "QQ";

  private static final String OUTPUT_FILE_NAME = "output.vcf";
  private static final String SAMPLE_TRUTH = "TRUTH";
  private static final String SAMPLE_QUERY = "QUERY";
  private static final int TRUTH = 0;
  private static final int QUERY = 1;

  private final VcfWriter mVcfOut;
  private final VcfHeader mOutHeader;
  private final VcfAltCleaner mAltCleaner = new VcfAltCleaner();
  private final RocSortValueExtractor mRocExtractor;

  // Helpers for merging records at same position
  private final VcfHeader[] mInHeaders = new VcfHeader[2];
  private final VcfRecord[] mInRecs = new VcfRecord[2];

  protected final int mBaselineSampleNo;
  protected final int mCallSampleNo;

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
   * @throws IOException if there is a problem opening output files
   */
  Ga4ghEvalSynchronizer(File baseLineFile, File callsFile, VariantSet variants, ReferenceRanges<String> ranges,
                        String baselineSampleName, String callsSampleName,
                        RocSortValueExtractor extractor,
                        File outdir, boolean zip) throws IOException {
    super(baseLineFile, callsFile, variants, ranges);
    mRocExtractor = extractor;
    mBaselineSampleNo = VcfUtils.getSampleIndexOrDie(variants.baseLineHeader(), baselineSampleName, "baseline");
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");

    mOutHeader = new VcfHeader();
    mOutHeader.addCommonHeader();
    mOutHeader.addContigFields(variants.baseLineHeader());
    mOutHeader.addInfoField(new InfoField(INFO_SUPERLOCUS_ID, MetaType.INTEGER, VcfNumber.DOT, "Benchmarking superlocus ID for these variants."));
    mOutHeader.addInfoField(new InfoField(INFO_CALL_WEIGHT, MetaType.FLOAT, new VcfNumber("1"), "Call weight (equivalent number of truth variants). When unspecified, assume 1.0"));
    mOutHeader.addFormatField(new FormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, new VcfNumber("1"), "Genotype"));
    mOutHeader.addFormatField(new FormatField(FORMAT_DECISION, MetaType.STRING, new VcfNumber("1"), "Decision for call (TP/FP/FN/N)"));
    mOutHeader.addFormatField(new FormatField(FORMAT_MATCH_KIND, MetaType.STRING, new VcfNumber("1"), "Sub-type for decision (match/mismatch type)"));
    mOutHeader.addFormatField(new FormatField(FORMAT_EXTRA, MetaType.STRING, new VcfNumber("1"), "Additional comparison information"));
    mOutHeader.addFormatField(new FormatField(FORMAT_ROC_SCORE, MetaType.FLOAT, new VcfNumber("1"), "Variant quality for ROC creation."));
    mOutHeader.addSampleName(SAMPLE_TRUTH);
    mOutHeader.addSampleName(SAMPLE_QUERY);

    mInHeaders[TRUTH] = variants.baseLineHeader().copy();
    mInHeaders[TRUTH].removeAllSamples();
    mInHeaders[TRUTH].addSampleName(SAMPLE_TRUTH);
    mInHeaders[QUERY] = variants.calledHeader().copy();
    mInHeaders[QUERY].removeAllSamples();
    mInHeaders[QUERY].addSampleName(SAMPLE_QUERY);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mVcfOut = new VcfWriter(mOutHeader, new File(outdir, OUTPUT_FILE_NAME + zipExt), null, zip, true);
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, mBaselineSampleNo, TRUTH);
    writeRecord(updateForBaseline(true, rec));
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, mBaselineSampleNo, TRUTH);
    writeRecord(updateForBaseline(false, rec));
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, mCallSampleNo, QUERY);
    writeRecord(updateForCall(true, rec));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, mCallSampleNo, QUERY);
    writeRecord(updateForCall(false, rec));
  }

  @Override
  protected void handleKnownBoth() throws IOException {
    handleUnknownBoth(false, false);
  }

  @Override
  protected void handleUnknownBoth(boolean unknownBaseline, boolean unknownCall) throws IOException {
    final VcfRecord rec = makeCombinedRecord();
    updateForBaseline(unknownBaseline, rec);
    updateForCall(unknownCall, rec);
    writeRecord(rec);
    mBrv = null;
    mCrv = null;
    if (!unknownBaseline) {
      mBv = null;
    }
    if (!unknownCall) {
      mCv = null;
    }
  }

  protected void writeRecord(VcfRecord rec) throws IOException {
    mAltCleaner.annotate(rec);
    mVcfOut.write(rec);
  }

  private VcfRecord makeCombinedRecord() {
    mInRecs[TRUTH] = makeSimpleRecord(mBrv, mBaselineSampleNo, -1);
    mInRecs[QUERY] = makeSimpleRecord(mCrv, mCallSampleNo, -1);
    return VcfRecord.mergeRecordsWithSameRef(mInRecs, mInHeaders, mOutHeader, Collections.<String>emptySet(), false); // Takes care of updating ALTs and GTs.
  }

  // Produce a (minimal) record with sample in correct location for easier merging / output
  private VcfRecord makeSimpleRecord(VcfRecord inRec, int sampleIndex, int destPos) {
    final VcfRecord outRec = new VcfRecord(inRec.getSequenceName(), inRec.getStart(), inRec.getRefCall());
    for (String alt : inRec.getAltCalls()) {
      outRec.addAltCall(alt);
    }
    final String gtStr = VcfUtils.getValidGtStr(inRec, sampleIndex);
    if (destPos == -1) {
      outRec.setNumberOfSamples(1);
      outRec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gtStr);
    } else {
      outRec.setNumberOfSamples(2);
      outRec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? gtStr : VcfUtils.MISSING_FIELD);
      outRec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? VcfUtils.MISSING_FIELD : gtStr);
    }
    return outRec;
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mVcfOut) {
      // done for nice closing side effects
    }
  }

  protected VcfRecord updateForCall(boolean unknown, VcfRecord outRec) {
    if (unknown) {
      outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, QUERY);
    } else {
      final String sync;
      final byte s = mCv.getStatus();
      switch (s) {
        case VariantId.STATUS_SKIPPED:
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, QUERY);
          outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_TOO_HARD, QUERY);
          sync = null;
          break;
        case VariantId.STATUS_GT_MATCH:
          setRocScore(outRec);
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_TP, QUERY);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_GT_MATCH, QUERY); // XXX If running --squash-ploidy, would we rather consider this ALLELE_MATCH (and check zygosity)?
          sync = Integer.toString(mCSyncStart + 1);
          break;
        case VariantId.STATUS_ALLELE_MATCH:
          setRocScore(outRec);
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FP, QUERY);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_ALLELE_MATCH, QUERY);
          if (isMultiAllelicCall(((OrientedVariant) mCv).variant())) {
            outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_MULTI, QUERY);
          }
          sync = Integer.toString(mCSyncStart2 + 1);
          break;
        case VariantId.STATUS_NO_MATCH:
          setRocScore(outRec);
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FP, QUERY);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_MISMATCH, QUERY);
          sync = mCSyncStart2 > 0 ? Integer.toString(mCSyncStart2 + 1) : Integer.toString(mCSyncStart + 1);
          break;
        default:
          throw new RuntimeException("Unhandled variant status during postprocessing: " + mCv.getStatus() + " cv:" + mCv);
      }
      setSyncId(outRec, sync); // XXX, when there is already a (different) sync ID, we should instead output two separate VCF records
    }
    return outRec;
  }

  private boolean isMultiAllelicCall(Variant ov) {
    if (ov instanceof GtIdVariant) {
      // Detect if this is (probably) a simple difference in zygosity vs one common and one novel allele
      final GtIdVariant gv = (GtIdVariant) ov;
      if (gv.alleleB() > 0 && gv.alleleA() > 0 && gv.alleleA() != gv.alleleB()) {
        return true;
      }
    }
    return false;
  }

  protected VcfRecord updateForBaseline(boolean unknown, VcfRecord outRec) {
    if (unknown) {
      outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, TRUTH);
    } else {
      final String sync;
      switch (mBv.getStatus()) {
        case VariantId.STATUS_SKIPPED:
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, TRUTH);
          outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_TOO_HARD, TRUTH);
          sync = null;
          break;
        case VariantId.STATUS_GT_MATCH:
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_TP, TRUTH);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_GT_MATCH, TRUTH); // XXX If running --squash-ploidy, would we rather consider this ALLELE_MATCH (and check zygosity)?
          sync = Integer.toString(mBSyncStart + 1);
          break;
        case VariantId.STATUS_ALLELE_MATCH:
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FN, TRUTH);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_ALLELE_MATCH, TRUTH);
          if (isMultiAllelicCall(((OrientedVariant) mBv).variant())) {
            outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_MULTI, TRUTH);
          }
          sync = Integer.toString(mBSyncStart2 + 1);
          break;
        case VariantId.STATUS_NO_MATCH:
          outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FN, TRUTH);
          outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_MISMATCH, TRUTH);
          sync = mBSyncStart2 > 0 ? Integer.toString(mBSyncStart2 + 1) : Integer.toString(mBSyncStart + 1);
          break;
        default:
          throw new RuntimeException("Unhandled variant status during postprocessing: " + mBv.getStatus() + " bv:" + mBv);
      }
      setSyncId(outRec, sync); // XXX, when there is already a (different) sync ID, we should instead output two separate VCF records
    }
    return outRec;
  }

  private void setRocScore(VcfRecord outRec) {
    final double score = mRocExtractor.getSortValue(mCrv, mCallSampleNo);
    if (!Double.isNaN(score)) {
      outRec.setFormatAndSample(FORMAT_ROC_SCORE,  "" + score, QUERY);
    }
  }

  private void setSyncId(VcfRecord outRec, String sync) {
    if (sync != null) {
      final ArrayList<String> oldSync = outRec.getInfo().get(INFO_SUPERLOCUS_ID);
      if (oldSync == null || oldSync.size() == 0 || !oldSync.get(0).equals(sync)) {
        outRec.addInfo(INFO_SUPERLOCUS_ID, sync);
      }
    }
  }

}
