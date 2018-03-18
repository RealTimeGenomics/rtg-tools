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
import com.rtg.vcf.header.VcfHeader;

/**
 * Writes out split files for correct and incorrect variants
 */
@TestClass("com.rtg.variant.sv.discord.BndEvalCliTest")
class SplitBndEvalVcfWriter implements BndEvalVcfWriter {

  private final VcfWriter mCorrectWriter;
  private final VcfWriter mIncorrectWriter;

  SplitBndEvalVcfWriter(VariantSetType setType, VcfHeader header, File outDir, boolean gzip) throws IOException {
    Diagnostic.userLog("Writing " + setType.label() + " results");
    final VcfWriterFactory vf = new VcfWriterFactory().zip(gzip).addRunInfo(true);
    final String cname = setType == VariantSetType.BASELINE ? "tp-baseline.vcf" : "tp.vcf";
    final String iname = setType == VariantSetType.BASELINE ? "fn.vcf" : "fp.vcf";
    mCorrectWriter = vf.make(header, FileUtils.getZippedFileName(gzip, new File(outDir, cname)));
    mIncorrectWriter = vf.make(header, FileUtils.getZippedFileName(gzip, new File(outDir, iname)));
  }

  @Override
  public void writeVariant(VariantSetType setType, VcfRecord rec, BndVariant v) throws IOException {
    if (v != null) {
      (v.isCorrect() ? mCorrectWriter : mIncorrectWriter).write(rec);
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mCorrectWriter;
         VcfWriter ignored2 = mIncorrectWriter) {
      // done for nice closing side effects
    }
  }
}
