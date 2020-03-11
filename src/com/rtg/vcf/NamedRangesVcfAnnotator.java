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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.RangeList.RangeView;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Annotates VCF records that overlap named regions with the name of each matching region.
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public abstract class NamedRangesVcfAnnotator implements VcfAnnotator {


  /** Per chromosome annotations. */
  private final ReferenceRanges<List<String>> mAnnotations;

  private final String mInfoId;
  private final String mInfoDescription;
  private final boolean mFullSpan;

  /**
   * Constructor
   * @param infoId if non-null, annotations will be added as an INFO field with this ID, otherwise add to VCF id column.
   * @param description if non-null, use this description for the INFO field header, if it doesn't already exist.
   * @param namedRanges contains all named ranges.
   * @param fullSpan if true, full reference span each VCF record will be considered, otherwise just the start position.
   */
  public NamedRangesVcfAnnotator(String infoId, String description, ReferenceRanges<List<String>> namedRanges, boolean fullSpan) {
    mInfoId = infoId;
    mInfoDescription = description;
    mAnnotations = namedRanges;
    mFullSpan = fullSpan;
  }

  private List<String> getAnnotations(String chr, int start, int end) {
    final RangeList<List<String>> chrRanges = mAnnotations.get(chr);
    if (chrRanges != null) {
      final Set<String> found = new HashSet<>();
      final List<RangeView<List<String>>> ranges = chrRanges.getFullRangeList();
      for (int rangeIndex = chrRanges.findFullRangeIndex(start); rangeIndex < ranges.size(); ++rangeIndex) {
        final RangeView<List<String>> range = ranges.get(rangeIndex);
        if (range.getStart() >= end) {
          break;
        }
        if (range.hasRanges()) {
          range.getMeta().forEach(found::addAll);
        }
      }
      if (!found.isEmpty()) {
        final List<String> anno = new ArrayList<>(found);
        Collections.sort(anno);
        return anno;
      }
    }
    return null;
  }

  private List<String> getAnnotations(String chr, int loc) {
    final RangeList<List<String>> chrRanges = mAnnotations.get(chr);
    if (chrRanges != null) {
      final List<List<String>> m = chrRanges.find(loc);
      if (m != null) {
        final Set<String> found = new HashSet<>();
        m.forEach(found::addAll);
        if (!found.isEmpty()) {
          final List<String> anno = new ArrayList<>(found);
          Collections.sort(anno);
          return anno;
        }
      }
    }
    return null;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mInfoId != null) {
      header.ensureContains(new InfoField(mInfoId, MetaType.STRING, VcfNumber.DOT, mInfoDescription));
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    final List<String> annotation;
    if (!mFullSpan) {
      annotation = getAnnotations(rec.getSequenceName(), rec.getStart());
    } else {
      annotation = getAnnotations(rec.getSequenceName(), rec.getStart(), VcfUtils.getEnd(rec));
    }
    if (annotation != null) {
      if (annotation.size() > 0) {
        if (mInfoId == null) {
          rec.setId(annotation.toArray(new String[0]));
        } else {
          rec.addInfo(mInfoId, annotation.toArray(new String[0]));
        }
      }
    }
  }
}
