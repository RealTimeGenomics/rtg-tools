/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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
 * Convenience wrapper for filtering a VcfReader
 */
public class VcfFilterIterator implements VcfIterator {

  private final AllMatchFilter mFilter;
  private final VcfIterator mDelegate;
  private VcfRecord mCurrent;

  /**
   * Constructor
   * @param delegate the underlying source of records
   * @param filters filters to apply to the record stream
   * @throws IOException if there is a problem reading the VCF
   */
  public VcfFilterIterator(VcfIterator delegate, VcfFilter... filters) throws IOException {
    this(delegate, Arrays.asList(filters));
  }

  /**
   * Constructor
   * @param delegate the underlying source of records
   * @param filters filters to apply to the record stream
   * @throws IOException if there is a problem reading the VCF
   */
  public VcfFilterIterator(VcfIterator delegate, Collection<VcfFilter> filters) throws IOException {
    mDelegate = delegate;
    mFilter = new AllMatchFilter(filters);
    mFilter.setHeader(delegate.getHeader());
    setNext();
  }

  @Override
  public VcfHeader getHeader() {
    return mDelegate.getHeader();
  }

  @Override
  public boolean hasNext() {
    return mCurrent != null;
  }

  @Override
  public VcfRecord next() throws IOException {
    final VcfRecord ret = mCurrent;
    setNext();
    return ret;
  }

  private void setNext() throws IOException {
    mCurrent = null;
    while (mDelegate.hasNext()) {
      final VcfRecord next = mDelegate.next();
      if (mFilter.accept(next)) {
        mCurrent = next;
        return;
      }
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (VcfIterator ignore = mDelegate) { }
  }
}
