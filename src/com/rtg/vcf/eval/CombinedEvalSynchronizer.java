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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.rtg.util.io.FileUtils;
import com.rtg.vcf.AnnotatingVcfWriter;
import com.rtg.vcf.VcfAltCleaner;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfRecordMerger;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates a single two-sample output VCF.
 */
class CombinedEvalSynchronizer extends WithInfoEvalSynchronizer {

  private static final String OUTPUT_FILE_NAME = "output.vcf";
  static final String SAMPLE_BASELINE = "BASELINE";
  static final String SAMPLE_CALLS = "CALLS";

  private final VcfWriter mVcfOut;
  private final VcfHeader mOutHeader;
  private final VcfRecord[] mInRecs = new VcfRecord[2];
  private final VcfHeader[] mInHeaders = new VcfHeader[2];
  private final VcfRecordMerger mMerger = new VcfRecordMerger();

  /**
   * @param variants the set of variants to evaluate
   * @param extractor extractor of ROC scores
   * @param outdir the output directory into which result files are written
   * @param zip true if output files should be compressed
   * @param slope true to output ROC slope files
   * @param dualRocs true to output additional ROC curves for allele-matches found in two-pass mode
   * @param rocFilters which ROC curves to output
   * @param rocCriteria criteria for selecting a favoured ROC point
   * @throws IOException if there is a problem opening output files
   */
  CombinedEvalSynchronizer(VariantSet variants,
                           RocSortValueExtractor extractor,
                           File outdir, boolean zip, boolean slope, boolean dualRocs, Set<RocFilter> rocFilters, RocPointCriteria rocCriteria) throws IOException {
    super(variants, extractor, outdir, zip, slope, dualRocs, rocFilters, rocCriteria);
    final String zipExt = zip ? FileUtils.GZ_SUFFIX : "";
    mOutHeader = new VcfHeader();
    mOutHeader.addCommonHeader();
    mOutHeader.addContigFields(variants.baselineHeader());
    addInfoHeaders(mOutHeader, null);
    mOutHeader.addFormatField(VcfUtils.FORMAT_GENOTYPE, MetaType.STRING, VcfNumber.ONE, "Genotype");
    mOutHeader.addSampleName(SAMPLE_BASELINE);
    mOutHeader.addSampleName(SAMPLE_CALLS);
    mInHeaders[0] = variants.baselineHeader().copy();
    mInHeaders[0].removeAllSamples();
    mInHeaders[0].addSampleName(SAMPLE_BASELINE);
    mInHeaders[1] = variants.calledHeader().copy();
    mInHeaders[1].removeAllSamples();
    mInHeaders[1].addSampleName(SAMPLE_CALLS);
    VcfWriter w = new VcfWriterFactory().zip(zip).addRunInfo(false).make(mOutHeader, new File(outdir, OUTPUT_FILE_NAME + zipExt));
    if (mBaselineSampleNo != -1 && mCallSampleNo != -1) {
      w = new AnnotatingVcfWriter(w, new VcfAltCleaner());
    }
    mVcfOut = w;
  }

  @Override
  protected void handleUnknownBaseline() throws IOException {
    writeBaseline(updateForBaseline(true, new LinkedHashMap<>()));
  }

  @Override
  protected void handleUnknownCall() throws IOException {
    writeCall(updateForCall(true, new LinkedHashMap<>()));
  }

  @Override
  protected void handleKnownCall() throws IOException {
    writeCall(updateForCall(false, new LinkedHashMap<>()));
  }

  @Override
  protected void handleKnownBaseline() throws IOException {
    writeBaseline(updateForBaseline(false, new LinkedHashMap<>()));
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

  protected void writeCall(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mCrv, mCallSampleNo, 1);
    setNewInfoFields(mCrv, newInfo);
    mVcfOut.write(mCrv);
  }

  protected void writeBaseline(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mBrv, mBaselineSampleNo, 0);
    setNewInfoFields(mBrv, newInfo);
    mVcfOut.write(mBrv);
  }

  protected void writeBoth(Map<String, String> newInfo) throws IOException {
    resetRecordFields(mBrv, mBaselineSampleNo, -1);
    resetRecordFields(mCrv, mCallSampleNo, -1);
    mInRecs[0] = mBrv;
    mInRecs[1] = mCrv;
    final VcfRecord rec = mMerger.mergeRecordsWithSameRef(mInRecs, mInHeaders, mOutHeader, Collections.emptySet(), false);
    setNewInfoFields(rec, newInfo);
    mVcfOut.write(rec);
  }

  private void resetRecordFields(VcfRecord rec, int sampleIndex, int destPos) { // Collapse to a minimal record for easier merging / output
    rec.setId();
    rec.setQuality(null);
    rec.getFilters().clear();
    rec.getInfo().clear();
    final String gtStr = sampleIndex != -1 ? VcfUtils.getValidGtStr(rec, sampleIndex) : VcfRecord.MISSING;
    rec.removeSamples();
    if (destPos == -1) {
      rec.setNumberOfSamples(1);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gtStr);
    } else {
      rec.setNumberOfSamples(2);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? gtStr : VcfUtils.MISSING_FIELD);
      rec.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, destPos == 0 ? VcfUtils.MISSING_FIELD : gtStr);
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    // Try-with-resources for nice closing side effects
    try (VcfWriter ignored = mVcfOut) {
      super.close();
    }
  }
}
