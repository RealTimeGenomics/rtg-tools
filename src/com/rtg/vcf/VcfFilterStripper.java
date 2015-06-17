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
package com.rtg.vcf;

import java.util.Iterator;
import java.util.Set;

import com.rtg.vcf.header.FilterField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Removes unwanted filter entries from a VCF record
 */
public class VcfFilterStripper implements VcfAnnotator {

  private final boolean mRemoveAll;
  private final boolean mKeepMode;
  private final Set<String> mFilters;

  /**
   * Remove all filters from header and records
   * @param removeAll false if you don't actually want to do it for some reason.
   */
  VcfFilterStripper(boolean removeAll) {
    mRemoveAll = removeAll;
    mKeepMode = false;
    mFilters = null;
  }

  /**
   * Keep or remove a selected set of filters from header and records
   * @param filterList the list of filter ids
   * @param keep true to keep values in the list, false to remove them
   */
  VcfFilterStripper(Set<String> filterList, boolean keep) {
    mRemoveAll = false;
    mKeepMode = keep;
    mFilters = filterList;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mRemoveAll) {
      header.getFilterLines().clear();
      return;
    } else if (mFilters == null || mFilters.size() == 0) {
      return;
    }
    final Iterator<FilterField> it = header.getFilterLines().iterator();
    while (it.hasNext()) {
      final FilterField filter = it.next();
      if (mKeepMode ^ mFilters.contains(filter.getId())) {
        it.remove();
      }
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    if (mRemoveAll) {
      rec.getFilters().clear();
      return;
    } else if (mFilters == null || mFilters.size() == 0) {
      return;
    }
    final Iterator<String> it = rec.getFilters().iterator();
    while (it.hasNext()) {
      final String e = it.next();
      if (mKeepMode ^ mFilters.contains(e)) {
        it.remove();
      }
    }
  }
}
