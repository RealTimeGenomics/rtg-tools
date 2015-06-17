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

package com.rtg.util.intervals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rtg.reader.ReaderUtils;
import com.rtg.reader.SequencesReader;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;

/**
 * Loads a bunch of chromosomal regions and allows you to ask if a position falls within any of the regions
 */
public class ReferenceRegions {


  final Map<String, MergedIntervals> mSequences;

  /**
   * Construct an empty region set
   */
  public ReferenceRegions() {
    this(new LinkedHashMap<String, MergedIntervals>());
  }

  /**
   * Construct an empty region set, backed by the user-supplied map
   * @param map the user supplied map
   */
  public ReferenceRegions(Map<String, MergedIntervals> map) {
    mSequences = map;
  }


  private MergedIntervals getOrAdd(String name) {
    MergedIntervals regions = mSequences.get(name);
    if (regions == null) {
      regions = new MergedIntervals();
      mSequences.put(name, regions);
    }
    return regions;
  }

  /**
   * Add a new region to the set
   * @param region the region to add
   */
  public void add(SequenceNameLocus region) {
    getOrAdd(region.getSequenceName()).add(region);
  }

  /**
   * Add a new region to the set
   * @param sequence name of the sequence the region is in
   * @param start 0-based inclusive start position of the region
   * @param end 0-based exclusive end position of the region
   */
  public void add(String sequence, int start, int end) {
    getOrAdd(sequence).add(start, end);
  }

  /**
   * @param sequence name of the sequence
   * @param pos zero based position within the sequence
   * @return true if the position provided falls within a bed record
   */
  public boolean enclosed(String sequence, int pos) {
    final MergedIntervals mergedIntervals = mSequences.get(sequence);
    return mergedIntervals != null && mergedIntervals.enclosed(pos);
  }

  /**
   * @param region the region to query
   * @return true if the locus provided falls entirely within the regions
   */
  public boolean enclosed(SequenceNameLocus region) {
    return enclosed(region.getSequenceName(), region.getStart(), region.getEnd());
  }

  /**
   * @param sequence name of the sequence
   * @param start zero based position within the sequence
   * @param end zero based position within the sequence
   * @return true if the position provided falls entirely within a bed record
   */
  public boolean enclosed(String sequence, int start, int end) {
    final MergedIntervals mergedIntervals = mSequences.get(sequence);
    return mergedIntervals != null && mergedIntervals.enclosed(start, end);
  }

  /**
   * @param region the region to query
   * @return true if the range specified is overlapped by a bed record
   */
  public boolean overlapped(SequenceNameLocus region) {
    return overlapped(region.getSequenceName(), region.getStart(), region.getEnd());
  }

  /**
   * @param sequence name of the sequence
   * @param start zero based start position within the sequence
   * @param end zero based end position within the sequence
   * @return true if the range specified is overlapped by a bed record
   */
  public boolean overlapped(String sequence, int start, int end) {
    final MergedIntervals mergedIntervals = mSequences.get(sequence);
    return mergedIntervals != null && mergedIntervals.overlapped(start, end);
  }

  /**
   * Work out the total length of all covered sections of the given sequence
   * @param sequence name of a sequence
   * @return the number of bases within <code>sequence</code> covered by regions
   */
  public int coveredLength(String sequence) {
    final MergedIntervals mergedIntervals = mSequences.get(sequence);
    return mergedIntervals == null ? 0 : mergedIntervals.totalLength();
  }

  /**
   * Map of sequence names to covered lengths
   * @return a map from sequence name to number of bases covered by regions
   */
  public Map<String, Integer> coveredLengths() {
    final Map<String, Integer> map = new HashMap<>();
    for (Map.Entry<String, MergedIntervals> entry : mSequences.entrySet()) {
      map.put(entry.getKey(), entry.getValue().totalLength());
    }
    return map;
  }

  /**
   * Will throw <code>InvalidParamsException</code> if the regions defined are not in the given template.
   * Note: this method assumes that the sequences reader provided has already been checked that it contains names.
   * @param reader the template sequences reader to validate
   * @throws IOException if an IO error occurs
   */
  public void validateTemplate(SequencesReader reader) throws IOException {
    final List<String> missingChromosomes = new ArrayList<>();
    final Map<String, Long> nameMap = ReaderUtils.getSequenceNameMap(reader);
    for (final String chr : mSequences.keySet()) {
      if (!nameMap.containsKey(chr)) {
        missingChromosomes.add(chr);
      }
    }
    if (missingChromosomes.size() > 0) {
      throw new InvalidParamsException("The following sequences specified in the BED regions are not present in the template: " + StringUtils.implode(missingChromosomes, ", "));
    }
  }
}
