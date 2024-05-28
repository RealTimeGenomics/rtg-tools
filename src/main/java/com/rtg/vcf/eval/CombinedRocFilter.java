/*
 * Copyright (c) 2024. Ricky Magnar.
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

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;

import java.util.ArrayList;
import java.util.List;

/**
 * CombinedRocFilter accepts only records that are accepted by all delegate RocFilters.
 */
public class CombinedRocFilter extends RocFilter {
  private final ArrayList<RocFilter> mRocFilters = new ArrayList<>();

  /**
   * Create a CombinedRocFilter which accepts only records that are accepted by all delegate RocFilters.
   * @param inputRocFilters List of RocFilters matched against to accept variant
   * @param rescale true if the call counts should be rescaled to baseline, false if the counts should not be rescaled, null to use global default
   */
  public CombinedRocFilter(List<RocFilter> inputRocFilters, Boolean rescale) {
    super("", rescale);
    final ArrayList<String> names = new ArrayList<>();

    for (RocFilter rocFilter : inputRocFilters) {
      if (rocFilter != RocFilter.ALL) {
        mRocFilters.add(rocFilter);
        names.add(rocFilter.name());
      }
    }

    mName = String.join("+", names);
  }

  @Override
  public void setHeader(VcfHeader header) {
    mRocFilters.forEach(f -> f.setHeader(header));
  }

  @Override
  public boolean requiresGt() {
    return mRocFilters.stream().anyMatch(RocFilter::requiresGt);
  }

  @Override
  public boolean accept(VcfRecord rec, int[] gt) {
    return mRocFilters.stream().allMatch(f -> f.accept(rec, gt));
  }
}
