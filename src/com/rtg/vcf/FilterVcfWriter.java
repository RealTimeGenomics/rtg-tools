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
import java.util.Arrays;
import java.util.Collection;

import com.rtg.vcf.header.VcfHeader;

/**
 * Convenience wrapper for filtering records going to a VcfWriter
 */
public class FilterVcfWriter implements VcfWriter {

  protected final AllMatchFilter mFilter;
  protected final VcfWriter mInner;

  /**
   * Constructor
   * @param inner the underlying destination for records
   * @param filters filters to apply to the record stream
   */
  public FilterVcfWriter(VcfWriter inner, VcfFilter... filters) {
    this(inner, Arrays.asList(filters));
  }

  /**
   * Constructor
   * @param inner the underlying destination for records
   * @param filters filters to apply to the record stream
   */
  public FilterVcfWriter(VcfWriter inner, Collection<VcfFilter> filters) {
    mInner = inner;
    mFilter = new AllMatchFilter(filters);
    mFilter.setHeader(inner.getHeader());
  }

  @Override
  public VcfHeader getHeader() {
    return mInner.getHeader();
  }

  @Override
  public void write(final VcfRecord record) throws IOException {
    if (mFilter.accept(record)) {
      mInner.write(record);
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfWriter ignore = mInner) { }
  }
}
