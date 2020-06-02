/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
package com.rtg.variant.sv.bndeval;

import java.io.File;
import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.VcfWriterFactory;
import com.rtg.vcf.eval.VariantSetType;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Adds match status annotations to existing VCF records.
 */
@TestClass("com.rtg.variant.sv.bndeval.BndEvalCliTest")
class AnnotatingBndEvalVcfWriter implements BndEvalVcfWriter {

  private static final String INFO_BASE = "BASE";
  private static final String INFO_CALL = "CALL";
  private static final String INFO_MATCH_COUNT = "MATCH_COUNT";
  private static final String STATUS_TP = "TP";
  private static final String STATUS_FN = "FN";
  private static final String STATUS_FP = "FP";
  private static final String STATUS_IGN = "IGN";

  private final VcfWriter mWriter;
  private final String mStatusLabel;

  AnnotatingBndEvalVcfWriter(VariantSetType setType, VcfHeader header, File outDir, boolean gzip) throws IOException {
    Diagnostic.userLog("Writing " + setType.label() + " results");
    mStatusLabel = getInfoLabel(setType);
    addInfoHeaders(header, setType);
    final String name = setType == VariantSetType.BASELINE ? "baseline.vcf" : "calls.vcf";
    mWriter = new VcfWriterFactory().zip(gzip).addRunInfo(true).make(header, FileUtils.getZippedFileName(gzip, new File(outDir, name)));
  }

  @Override
  public void writeVariant(VariantSetType setType, VcfRecord rec, BndVariant v) throws IOException {
    resetOurAnnotations(rec);
    rec.setInfo(mStatusLabel, getInfoStatus(setType, v));
    if (v != null && v.matches().size() > 1) {
      rec.setInfo(INFO_MATCH_COUNT, Integer.toString(v.matches().size()));
    }
    mWriter.write(rec);
  }

  private String getInfoStatus(VariantSetType setType, BndVariant v) {
    if (v == null) {
      return STATUS_IGN;
    }
    switch (setType) {
      case BASELINE:
        return v.isCorrect() ? STATUS_TP : STATUS_FN;
      case CALLS:
        return v.isCorrect() ? STATUS_TP : STATUS_FP;
      default:
        throw new RuntimeException("Unknown variant set type");
    }
  }

  private String getInfoLabel(VariantSetType setType) {
    switch (setType) {
      case BASELINE:
        return INFO_BASE;
      case CALLS:
        return INFO_CALL;
      default:
        throw new RuntimeException("Unknown variant set type");
    }
  }

  // Remove any pre-existing annotations for VCFs that have already been through evaluation
  private void resetOurAnnotations(VcfRecord rec) {
    rec.removeInfo(INFO_BASE);
    rec.removeInfo(INFO_CALL);
  }

  private void addInfoHeaders(VcfHeader header, VariantSetType type) {
    if (type == null || type == VariantSetType.BASELINE) {
      header.ensureContains(new InfoField(INFO_BASE, MetaType.STRING, VcfNumber.ONE, "Baseline variant status"));
    }
    if (type == null || type == VariantSetType.CALLS) {
      header.ensureContains(new InfoField(INFO_CALL, MetaType.STRING, VcfNumber.ONE, "Call variant status"));
    }
    header.ensureContains(new InfoField(INFO_MATCH_COUNT, MetaType.INTEGER, VcfNumber.ONE, "For multi-matches, the number of variants matched"));
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mWriter) {
      // done for nice closing side effects
    }
  }
}
