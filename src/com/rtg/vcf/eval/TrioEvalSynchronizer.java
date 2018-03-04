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

import com.rtg.relation.Family;
import com.rtg.relation.PedigreeException;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfAltCleaner;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfRecordMerger;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates a single three-sample output VCF for the trio.
 */
class TrioEvalSynchronizer extends WithInfoEvalSynchronizer {

  private static final String OUTPUT_FILE_NAME = "output.vcf";
  private static final String SAMPLE_FATHER = "FATHER";
  private static final String SAMPLE_MOTHER = "MOTHER";
  private static final String SAMPLE_CHILD = "CHILD";

  // Order of sample columns in the output VCF
  static final int FATHER_SAMPLE_INDEX = 0;
  //static final int MOTHER_SAMPLE_INDEX = 1;
  static final int CHILD_SAMPLE_INDEX = 2;

  // Record order during merging (first has field priority)
  private static final int QUERY_MERGE_INDEX = 0;
  private static final int TRUTH_MERGE_INDEX = 1;

  private final VcfWriter mVcfOut;
  private final VcfHeader mOutHeader;
  private final VcfAltCleaner mAltCleaner = new VcfAltCleaner();
  private final VcfRecordMerger mMerger = new VcfRecordMerger();

  // Helpers for merging records at same position
  private final VcfHeader[] mInHeaders = new VcfHeader[2];
  private final VcfRecord[] mInRecs = new VcfRecord[2];

  protected final int mFatherSampleNo;
  protected final int mMotherSampleNo;
  protected final int mChildSampleNo;

  /**
   * @param variants the set of variants to evaluate
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @throws IOException if there is a problem opening output files
   */
  TrioEvalSynchronizer(VariantSet variants,
                       File outdir, boolean zip) throws IOException {
    super(variants, RocSortValueExtractor.NULL_EXTRACTOR, outdir, zip, false, false, Collections.emptySet());
    final Family family;
    try {
      family = Family.getFamily(VcfPedigreeParser.load(variants.baselineHeader()));
    } catch (PedigreeException e) {
      throw new NoTalkbackSlimException(e.getMessage());
    }
    mFatherSampleNo = VcfUtils.getSampleIndexOrDie(variants.baselineHeader(), family.getFather(), VariantSetType.BASELINE.label());
    mMotherSampleNo = VcfUtils.getSampleIndexOrDie(variants.baselineHeader(), family.getMother(), VariantSetType.BASELINE.label());
    mChildSampleNo = variants.calledSample();

    mOutHeader = new VcfHeader();
    mOutHeader.addCommonHeader();
    mOutHeader.addContigFields(variants.baselineHeader());
    variants.calledHeader().getFilterLines().forEach(mOutHeader::addFilterField);
    addInfoHeaders(mOutHeader, null);
    mOutHeader.addFormatField(new FormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, new VcfNumber("1"), "Genotype"));
    mOutHeader.addSampleName(SAMPLE_FATHER);
    mOutHeader.addSampleName(SAMPLE_MOTHER);
    mOutHeader.addSampleName(SAMPLE_CHILD);
    mInHeaders[TRUTH_MERGE_INDEX] = variants.baselineHeader().copy();
    mInHeaders[TRUTH_MERGE_INDEX].removeAllSamples();
    mInHeaders[TRUTH_MERGE_INDEX].addSampleName(SAMPLE_FATHER);
    mInHeaders[TRUTH_MERGE_INDEX].addSampleName(SAMPLE_MOTHER);
    mInHeaders[QUERY_MERGE_INDEX] = variants.calledHeader().copy();
    mInHeaders[QUERY_MERGE_INDEX].removeAllSamples();
    mInHeaders[QUERY_MERGE_INDEX].addSampleName(SAMPLE_CHILD);

    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mVcfOut = new VcfWriterFactory().zip(zip).addRunInfo(false).make(mOutHeader, new File(outdir, OUTPUT_FILE_NAME + zipExt));
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, false, 3, FATHER_SAMPLE_INDEX, mFatherSampleNo, mMotherSampleNo);
    writeRecord(updateForBaseline(true, rec));
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mBrv, false, 3, FATHER_SAMPLE_INDEX, mFatherSampleNo, mMotherSampleNo);
    writeRecord(updateForBaseline(false, rec));
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, true, 3, CHILD_SAMPLE_INDEX, mChildSampleNo);
    writeRecord(updateForCall(true, rec));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    final VcfRecord rec = makeSimpleRecord(mCrv, true, 3, CHILD_SAMPLE_INDEX, mChildSampleNo);
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
    mInRecs[TRUTH_MERGE_INDEX] = makeSimpleRecord(mBrv, false, 2, 0, mFatherSampleNo, mMotherSampleNo);
    mInRecs[QUERY_MERGE_INDEX] = makeSimpleRecord(mCrv, true, 1, 0, mChildSampleNo);
    return mMerger.mergeRecordsWithSameRef(mInRecs, mInHeaders, mOutHeader, Collections.emptySet(), false); // Takes care of updating ALTs and GTs.
  }

  // Produce a (minimal) record with sample in correct location for easier merging / output
  private VcfRecord makeSimpleRecord(VcfRecord inRec, boolean preserveFilters, int numSamples, int dest, int... src) {
    final VcfRecord outRec = new VcfRecord(inRec.getSequenceName(), inRec.getStart(), inRec.getRefCall());
    inRec.getAltCalls().forEach(outRec::addAltCall);
    if (preserveFilters) {
      inRec.getFilters().forEach(outRec::addFilter);
    }
    outRec.addInfo(INFO_BASE).addInfo(INFO_CALL); // Just to ensure info field ordering.
    outRec.setNumberOfSamples(numSamples);
    outRec.padFormatAndSample(VcfUtils.FORMAT_GENOTYPE);
    int d = dest;
    for (int s : src) {
      final String gtStr = VcfUtils.getValidGtStr(inRec, s);
      outRec.setFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gtStr, d++);
    }
    return outRec;
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    // Try-with-resources for nice closing side effects
    try (VcfWriter ignored = mVcfOut) {
      mMerger.close();
      super.close();
    }
  }

  protected VcfRecord updateForCall(boolean unknown, VcfRecord outRec) {
    if (unknown) {
      outRec.setInfo(INFO_CALL, STATUS_IGNORED);
    } else {
      if (mCv.hasStatus(VariantId.STATUS_SKIPPED)) {
        outRec.setInfo(INFO_CALL, STATUS_HARD);
      } else if (mCv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        setSyncId(outRec, Integer.toString(mCSyncStart + 1));
        outRec.setInfo(INFO_CALL, STATUS_TP);
      } else if (mCv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        setSyncId(outRec, Integer.toString(mCSyncStart + 1));
        outRec.setInfo(INFO_CALL, STATUS_FP_CA);
      } else if (mCv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        outRec.setInfo(INFO_CALL, STATUS_OUTSIDE);
      } else if (mCv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        setSyncId(outRec, mCSyncStart2 > 0 ? Integer.toString(mCSyncStart2 + 1) : Integer.toString(mCSyncStart + 1));
        outRec.setInfo(INFO_CALL, STATUS_FP);
      } else {
        throw new RuntimeException("Unhandle variant status during postprocessing: " + mCv);
      }
    }
    return outRec;
  }

  protected VcfRecord updateForBaseline(boolean unknown, VcfRecord outRec) {
    if (unknown) {
      outRec.setInfo(INFO_BASE, STATUS_IGNORED);
    } else {
      if (mBv.hasStatus(VariantId.STATUS_SKIPPED)) {
        outRec.setInfo(INFO_BASE, STATUS_HARD);
      } else if (mBv.hasStatus(VariantId.STATUS_OUTSIDE_EVAL)) {
        outRec.setInfo(INFO_BASE, STATUS_OUTSIDE);
      } else if (mBv.hasStatus(VariantId.STATUS_GT_MATCH)) {
        setSyncId(outRec, Integer.toString(mBSyncStart + 1));
        outRec.setInfo(INFO_BASE, STATUS_TP);
      } else if (mBv.hasStatus(VariantId.STATUS_ALLELE_MATCH)) {
        setSyncId(outRec, Integer.toString(mBSyncStart2 + 1));
        outRec.setInfo(INFO_BASE, STATUS_FN_CA);
      } else if (mBv.hasStatus(VariantId.STATUS_NO_MATCH)) {
        setSyncId(outRec, mBSyncStart2 > 0 ? Integer.toString(mBSyncStart2 + 1) : Integer.toString(mBSyncStart + 1));
        outRec.setInfo(INFO_BASE, STATUS_FN);
      } else {
        throw new RuntimeException("Unhandled variant status during postprocessing: " + mBv);
      }
    }
    return outRec;
  }

  private void setSyncId(VcfRecord outRec, String sync) {
    if (sync != null) {
      final ArrayList<String> oldSync = outRec.getInfo().get(INFO_SYNCPOS);
      if (oldSync == null || oldSync.size() == 0 || !oldSync.get(0).equals(sync)) {
        outRec.addInfo(INFO_SYNCPOS, sync);
      }
    }
  }

}
