/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.vcf;

import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.reader.SequencesReader;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Decomposes variants during writing
 */
@TestClass(value = "com.rtg.vcf.VcfDecomposerCliTest")
class DecomposingVcfWriter extends Decomposer implements VcfWriter {

  private static final String ORP = "ORP";
  private static final String ORL = "ORL";

  private final VcfWriter mOut;

  DecomposingVcfWriter(VcfWriter dest, SequencesReader template, boolean breakMnps, boolean breakIndels) throws IOException {
    super(template, dest.getHeader(), breakMnps, breakIndels);
    mOut = new ReorderingVcfWriter(dest); // Needed to ensure appropriate ordering of output variants
    mOut.getHeader().ensureContains(new InfoField(ORP, MetaType.STRING, VcfNumber.ONE, "Original variant position"));
    mOut.getHeader().ensureContains(new InfoField(ORL, MetaType.STRING, VcfNumber.ONE, "Original reference length"));
  }

  @Override
  public VcfHeader getHeader() {
    return mOut.getHeader();
  }

  @Override
  public void write(VcfRecord record) throws IOException {
    if (!canDecompose(record)) {
      mOut.write(record); // Just pass straight through
    } else {
      for (final VcfRecord res : decompose(record)) {
        mOut.write(res);
      }
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignored = mOut) {
      // Use try with resources on existing writer for nice closing
    }
  }
}
