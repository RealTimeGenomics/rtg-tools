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
import java.util.List;

import com.rtg.vcf.header.VcfHeader;

/**
 * Gather statistics on records passing through.
 */
public class StatisticsVcfWriter<S extends VariantStatistics> implements VcfWriter {

  private final VcfWriter mInner;
  private final S mStatistics;
  private final List<VcfFilter> mFilters;
  private final VcfHeader mVcfHeader;

  /**
   * Create a new VCF writer that keeps statistics.
   * @param writer underlying writer
   * @param statistics statistics tracker
   */
  public StatisticsVcfWriter(final VcfWriter writer, final S statistics, final List<VcfFilter> filters) {
    mInner = writer;
    mStatistics = statistics;
    mFilters = filters;
    mVcfHeader = mInner.getHeader();
    for (final VcfFilter filter : mFilters) {
      filter.setHeader(mVcfHeader);
    }
  }

  @Override
  public VcfHeader getHeader() {
    return mVcfHeader;
  }

  @Override
  public void write(final VcfRecord record) throws IOException {
    boolean keep = true;
    for (final VcfFilter filter : mFilters) {
      if (!filter.accept(record)) {
        keep = false;
        break;
      }
    }
    if (keep) {
      mStatistics.tallyVariant(mVcfHeader, record);
      mInner.write(record);
    }
  }

  @Override
  public void close() throws IOException {
    mInner.close();
  }
}
