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
package com.rtg.variant.cnv.cnveval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rtg.util.MultiSet;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.IntervalComparator;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.variant.cnv.CnaType;
import com.rtg.variant.cnv.cnveval.CnaVariant.RegionContext;
import com.rtg.variant.cnv.cnveval.CnaVariant.SpanType;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.eval.VariantSetType;
import com.rtg.vcf.header.VcfHeader;

/**
 * Holds all of the CnaVariants for a genome and maintain per-record stats
 */
class CnaVariantSet extends LinkedHashMap<String, CnaVariantList> {

  private final VcfHeader mVcfHeader;
  private final VariantSetType mType;
  private final Map<VcfRecord, CnaRecordStats> mPerRecord = new LinkedHashMap<>();

  CnaVariantSet(VcfHeader header, VariantSetType type) {
    mVcfHeader = header.copy();
    mType = type;
  }

  VcfHeader getHeader() {
    return mVcfHeader;
  }

  VariantSetType variantSetType() {
    return mType;
  }

  Collection<CnaRecordStats> records() {
    return mPerRecord.values();
  }

  void add(CnaVariant v) {
    final CnaVariantList current = computeIfAbsent(v.record().getSequenceName(), k -> new CnaVariantList());
    current.add(v);
    if (!mPerRecord.containsKey(v.record())) {
      mPerRecord.put(v.record(), new CnaRecordStats(v.record()));
    }
  }

  // Called when all variants have been added to the set
  void loaded() {
    for (final CnaVariantList chrVars : values()) {
      chrVars.sort(IntervalComparator.SINGLETON);
    }
    sanityCheck();
  }

  private void sanityCheck() {
    // Check for cases where the intersection between variant set and evaluation region yields multiple variants for an
    // evaluation region.

    // For calls, this is a reasonably common occurrence (e.g. when gene-level evaluation and calls are small pieces of the gene).
    // In this case, each resulting CnaVariant will get an appropriate match status as long as there is at most a single
    // baseline variant assigned to the region (and it fully spans the region).

    // For this reason, issue a warning if a baseline variant only partially spans an evaluation region (handled during loading)
    // or if we have multiple baseline variants for any given evaluation region (handled here).
    for (final CnaVariantList chrVars : values()) {
      CnaVariant last = null;
      int overlaps = 0;
      final MultiSet<CnaType> types = new MultiSet<>();
      for (int i = 0; i < chrVars.size(); i++) {
        final CnaVariant v =  chrVars.get(i);
        if (last != null && last.getStart() == v.getStart() && last.getEnd() == v.getEnd()) {
          overlaps++;
        } else {
          checkOverlaps(chrVars, last, overlaps, i, types);
          overlaps = 0;
          types.clear();
        }
        last = v;
        types.add(v.cnaType());
      }
      checkOverlaps(chrVars, last, overlaps, chrVars.size(), types);
    }
  }

  private void checkOverlaps(final CnaVariantList chrVars, final CnaVariant last, final int overlaps, final int end, final MultiSet<CnaType> types) {
    if (overlaps > 0) {
      final int start = end - 1 - overlaps;
      final SequenceNameLocusSimple l = new SequenceNameLocusSimple(chrVars.get(start).record().getSequenceName(), chrVars.get(start).getStart(), chrVars.get(start).getEnd());

      // We trust that if they are all PARTIAL, they don't actually overlap with each other, so only warn if one of them is FULL.
      // We could check this more thoroughly if we really wanted to.
      for (int j = start; j < end; j++) {
        if (chrVars.get(j).spanType() == SpanType.FULL) {
          Diagnostic.warning("Multiple " + mType.label() + " variants apply to region: " + l + " " + types.toString());
          break;
        }
      }

      final String post;
      if (types.keySet().size() > 1) {
        // Mix of DEL/DUP, mark all in the region as inconsistent
        // Perhaps we should arbitrarily retain one.
        post = " (mixed types, marking all as inconsistent)";
        for (int j = start; j < end; j++) {
          chrVars.get(j).setRegionContext(RegionContext.INCONSISTENT);
        }
      } else {
        // Consistent types, mark all but the first in the region as redundant
        post = " (keeping first, types are consistent)";
        for (int j = start + 1; j < end; j++) {
          chrVars.get(j).setRegionContext(RegionContext.REDUNDANT);
        }
      }

      final String msg = "Multiple " + mType.label() + " variants apply to region: " + l + " " + types.toString() + post;
      if (mType == VariantSetType.BASELINE) {
        Diagnostic.warning(msg);
      } else {
        Diagnostic.userLog(msg);
      }
    }
  }

  // Should be called once the correctness of each CnaVariant has been ascertained
  void computeRecordCounts() {
    values().stream().flatMap(Collection<CnaVariant>::stream).forEach(this::incrementRecordCount);
  }

  private void incrementRecordCount(CnaVariant v) {
    mPerRecord.get(v.record()).increment(v);
  }

  @Override
  public String toString() {
    final int variantcount = mPerRecord.size();
    final int regioncount = values().stream().mapToInt(ArrayList<CnaVariant>::size).sum();
    final MultiSet<CnaType> statuscounts = new MultiSet<>();
    values().stream().flatMap(Collection<CnaVariant>::stream).map(CnaVariant::cnaType).forEach(statuscounts::add);

    return mType.label() + " variant set containing " + variantcount + " variants"
      + " on " + keySet().size() + " chromosomes, " + regioncount + " regions"
      + " (" + statuscounts.get(CnaType.DEL) + " deletions, " + statuscounts.get(CnaType.DUP) + " duplications)";
  }
}
