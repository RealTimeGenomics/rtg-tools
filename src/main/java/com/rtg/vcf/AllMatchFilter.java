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

import java.util.Arrays;
import java.util.Collection;

import com.rtg.vcf.header.VcfHeader;

/**
 * Accepts records that are accepted by all delegate filters
 */
public class AllMatchFilter implements VcfFilter {

  Collection<VcfFilter> mFilters;

  /**
   * Constructor
   * @param filters delegate filters
   */
  public AllMatchFilter(VcfFilter... filters) {
    this(Arrays.asList(filters));
  }

  /**
   * Constructor
   * @param filters delegate filters
   */
  public AllMatchFilter(Collection<VcfFilter> filters) {
    mFilters = filters;
  }

  @Override
  public void setHeader(VcfHeader header) {
    mFilters.forEach(f -> f.setHeader(header));
  }

  @Override
  public boolean accept(VcfRecord rec) {
    return mFilters.stream().allMatch(filter -> filter.accept(rec));
  }
}
