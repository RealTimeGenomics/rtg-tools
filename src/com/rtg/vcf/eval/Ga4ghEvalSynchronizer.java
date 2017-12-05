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
import com.rtg.vcf.VcfRecordMerger;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
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

  static final String FORMAT_DECISION = "BD";
  static final String DECISION_TP = "TP";
  static final String DECISION_FN = "FN";
  static final String DECISION_FP = "FP";
  static final String DECISION_OTHER = "N";

  static final String FORMAT_MATCH_KIND = "BK";
  static final String SUBTYPE_MISMATCH = ".";
  static final String SUBTYPE_GT_MATCH = "gm";
  static final String SUBTYPE_ALLELE_MATCH = "am";
  static final String SUBTYPE_REGIONAL_MATCH = "lm";

  private static final String FORMAT_EXTRA = "BI";
  private static final String EXTRA_MULTI = "multi";
  private static final String EXTRA_TOO_HARD = "too-hard";
  private static final String EXTRA_NON_CONF = "non-confident";

  private static final String FORMAT_ROC_SCORE = "QQ";

  private static final String OUTPUT_FILE_NAME = "output.vcf";
  private static final String SAMPLE_TRUTH = "TRUTH";
  private static final String SAMPLE_QUERY = "QUERY";

  // Order of sample columns in the output VCF
  static final int TRUTH_SAMPLE_INDEX = 0;
  static final int QUERY_SAMPLE_INDEX = 1;

  // Record order during merging (first has field priority)
  private static final int QUERY_MERGE_INDEX = 0;
  private static final int TRUTH_MERGE_INDEX = 1;

  private final VcfWriter mVcfOut;
  private final VcfHeader mOutHeader;
  private final VcfAltCleaner mAltCleaner = new VcfAltCleaner();
  private final RocSortValueExtractor mRocExtractor;
  private final VcfRecordMerger mMerger = new VcfRecordMerger();

  // Helpers for merging records at same position
  private final VcfHeader[] mInHeaders = new VcfHeader[2];
  private final VcfRecord[] mInRecs = new VcfRecord[2];

  protected final int mBaselineSampleNo;
  protected final int mCallSampleNo;

  /**
   * @param variants the set of variants to evaluate
   * @param ranges the regions from which variants are being loaded
   * @param baselineSampleName the name of the sample used in the baseline
   * @param callsSampleName the name of the sample used in the calls
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param looseMatchDistance if greater than 0, apply loose matching rules with the supplied distance
   * @throws IOException if there is a problem opening output files
   */
  Ga4ghEvalSynchronizer(VariantSet variants, ReferenceRanges<String> ranges,
                        String baselineSampleName, String callsSampleName,
                        RocSortValueExtractor extractor,
                        File outdir, boolean zip, int looseMatchDistance) throws IOException {
    super(variants, ranges);
    mRocExtractor = extractor;
    mBaselineSampleNo = VcfUtils.getSampleIndexOrDie(variants.baselineHeader(), baselineSampleName, "baseline");
    mCallSampleNo = VcfUtils.getSampleIndexOrDie(variants.calledHeader(), callsSampleName, "calls");

    mOutHeader = new VcfHeader();
    mOutHeader.addCommonHeader();
    mOutHeader.addContigFields(variants.baselineHeader());
    variants.calledHeader().getFilterLines().forEach(mOutHeader::addFilterField);
    mOutHeader.addInfoField(new InfoField(INFO_SUPERLOCUS_ID, MetaType.INTEGER, VcfNumber.DOT, "Benchmarking superlocus ID for these variants"));
    mOutHeader.addInfoField(new InfoField(INFO_CALL_WEIGHT, MetaType.FLOAT, new VcfNumber("1"), "Call weight (equivalent number of truth variants). When unspecified, assume 1.0"));
    mOutHeader.addFormatField(new FormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, new VcfNumber("1"), "Genotype"));
    mOutHeader.addFormatField(new FormatField(FORMAT_DECISION, MetaType.STRING, new VcfNumber("1"), "Decision for call (TP/FP/FN/N)"));
    mOutHeader.addFormatField(new FormatField(FORMAT_MATCH_KIND, MetaType.STRING, new VcfNumber("1"), "Sub-type for decision (match/mismatch type)" + (looseMatchDistance >= 0 ? ". (Loose match distance is " + looseMatchDistance + ")" : "")));
    mOutHeader.addFormatField(new FormatField(FORMAT_EXTRA, MetaType.STRING, new VcfNumber("1"), "Additional comparison information"));
    mOutHeader.addFormatField(new FormatField(FORMAT_ROC_SCORE, MetaType.FLOAT, new VcfNumber("1"), "Variant quality for ROC creation"));
    mOutHeader.addSampleName(SAMPLE_TRUTH);
    mOutHeader.addSampleName(SAMPLE_QUERY);
    mInHeaders[TRUTH_MERGE_INDEX] = variants.baselineHeader().copy();
    mInHeaders[TRUTH_MERGE_INDEX].removeAllSamples();
    mInHeaders[TRUTH_MERGE_INDEX].addSampleName(SAMPLE_TRUTH);
    mInHeaders[QUERY_MERGE_INDEX] = variants.calledHeader().copy();
    mInHeaders[QUERY_MERGE_INDEX].removeAllSamples();
    mInHeaders[QUERY_MERGE_INDEX].addSampleName(SAMPLE_QUERY);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    final VcfWriter w = new VcfWriterFactory().zip(zip).addRunInfo(false).make(mOutHeader, new File(outdir, OUTPUT_FILE_NAME + zipExt));
    mVcfOut = looseMatchDistance >= 0 ? new Ga4ghLooseMatchFilter(w, looseMatchDistance) : w;
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, mBaselineSampleNo, TRUTH_SAMPLE_INDEX, false);
    writeRecord(updateForBaseline(true, rec));
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, mBaselineSampleNo, TRUTH_SAMPLE_INDEX, false);
    writeRecord(updateForBaseline(false, rec));
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, mCallSampleNo, QUERY_SAMPLE_INDEX, true);
    writeRecord(updateForCall(true, rec));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, mCallSampleNo, QUERY_SAMPLE_INDEX, true);
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
    mInRecs[TRUTH_MERGE_INDEX] = makeSimpleRecord(mBrv, mBaselineSampleNo, -1, false);
    mInRecs[QUERY_MERGE_INDEX] = makeSimpleRecord(mCrv, mCallSampleNo, -1, true);
    return mMerger.mergeRecordsWithSameRef(mInRecs, mInHeaders, mOutHeader, Collections.<String>emptySet(), false); // Takes care of updating ALTs and GTs.
  }

  // Produce a (minimal) record with sample in correct location for easier merging / output
  private VcfRecord makeSimpleRecord(VcfRecord inRec, int sampleIndex, int destPos, boolean preserveFilters) {
    final VcfRecord outRec = new VcfRecord(inRec.getSequenceName(), inRec.getStart(), inRec.getRefCall());
    inRec.getAltCalls().forEach(outRec::addAltCall);
    if (preserveFilters) {
      inRec.getFilters().forEach(outRec::addFilter);
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
    mMerger.close();
  }

  protected VcfRecord updateForCall(boolean unknown, VcfRecord outRec) {
    if (unknown) {
      outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, QUERY_SAMPLE_INDEX);
    } else {
      final String sync;
      if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, QUERY_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_TOO_HARD, QUERY_SAMPLE_INDEX);
        sync = null;
      } else if (mCv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        setRocScore(outRec);
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_TP, QUERY_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_GT_MATCH, QUERY_SAMPLE_INDEX); // XXX If running --squash-ploidy, would we rather consider this ALLELE_MATCH (and check zygosity)?
        sync = Integer.toString(mCSyncStart + 1);
      } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        setRocScore(outRec);
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FP, QUERY_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_ALLELE_MATCH, QUERY_SAMPLE_INDEX);
        if (isMultiAllelicCall(((OrientedVariant) mCv).variant())) {
          outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_MULTI, QUERY_SAMPLE_INDEX);
        }
        sync = Integer.toString(mCSyncStart2 + 1);
      } else if (mCv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, QUERY_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_NON_CONF, QUERY_SAMPLE_INDEX);
        sync = null;
      } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        setRocScore(outRec);
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FP, QUERY_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_MISMATCH, QUERY_SAMPLE_INDEX);
        sync = mCSyncStart2 > 0 ? Integer.toString(mCSyncStart2 + 1) : Integer.toString(mCSyncStart + 1);
      } else {
        throw new RuntimeException("Unhandle variant status during postprocessing: " + mCv);
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
      outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, TRUTH_SAMPLE_INDEX);
    } else {
      final String sync;
      if (mBv.hasStatus(VariantId.STATUS_SKIPPED)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, TRUTH_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_TOO_HARD, TRUTH_SAMPLE_INDEX);
        sync = null;
      } else if (mBv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_OTHER, TRUTH_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_NON_CONF, TRUTH_SAMPLE_INDEX);
        sync = null;
      } else if (mBv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_TP, TRUTH_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_GT_MATCH, TRUTH_SAMPLE_INDEX); // XXX If running --squash-ploidy, would we rather consider this ALLELE_MATCH (and check zygosity)?
        sync = Integer.toString(mBSyncStart + 1);
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FN, TRUTH_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_ALLELE_MATCH, TRUTH_SAMPLE_INDEX);
        if (isMultiAllelicCall(((OrientedVariant) mBv).variant())) {
          outRec.setFormatAndSample(FORMAT_EXTRA, EXTRA_MULTI, TRUTH_SAMPLE_INDEX);
        }
        sync = Integer.toString(mBSyncStart2 + 1);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        outRec.setFormatAndSample(FORMAT_DECISION, DECISION_FN, TRUTH_SAMPLE_INDEX);
        outRec.setFormatAndSample(FORMAT_MATCH_KIND, SUBTYPE_MISMATCH, TRUTH_SAMPLE_INDEX);
        sync = mBSyncStart2 > 0 ? Integer.toString(mBSyncStart2 + 1) : Integer.toString(mBSyncStart + 1);
      } else {
        throw new RuntimeException("Unhandled variant status during postprocessing: " + mBv);
      }
      setSyncId(outRec, sync); // XXX, when there is already a (different) sync ID, we should instead output two separate VCF records
    }
    return outRec;
  }

  private void setRocScore(VcfRecord outRec) {
    final double score = mRocExtractor.getSortValue(mCrv, mCallSampleNo);
    if (!Double.isNaN(score)) {
      outRec.setFormatAndSample(FORMAT_ROC_SCORE,  "" + score, QUERY_SAMPLE_INDEX);
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
