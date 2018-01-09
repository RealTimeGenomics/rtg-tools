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
package com.rtg.vcf;

import java.io.IOException;

import com.rtg.vcf.header.VcfHeader;

/**
 * A VcfWriter support annotation of records as the pass through.
 */
public class AnnotatingVcfWriter implements VcfWriter {

  private final VcfWriter mParent;
  private final VcfAnnotator mAnnotator;

  /**
   * Construct an annotating VcfWriter.
   * @param parent underlying writer
   * @param annotator annotator to be applied
   */
  public AnnotatingVcfWriter(final VcfWriter parent, final VcfAnnotator annotator) {
    mParent = parent;
    mAnnotator = annotator;
    mAnnotator.updateHeader(mParent.getHeader()); // Assumes parent has not already written the header!
  }

  @Override
  public VcfHeader getHeader() {
    return mParent.getHeader();
  }

  @Override
  public void write(final VcfRecord record) throws IOException {
    mAnnotator.annotate(record);
    mParent.write(record);
  }

  @Override
  public void close() throws IOException {
    mParent.close();
  }
}
