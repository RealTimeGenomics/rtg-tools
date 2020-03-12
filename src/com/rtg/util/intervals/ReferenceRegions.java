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
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rtg.bed.BedRecord;
import com.rtg.bed.BedWriter;
import com.rtg.util.io.IOIterator;

/**
 * Loads a bunch of chromosomal regions and allows you to ask if a position falls within any of the regions
 */
public class ReferenceRegions implements Iterable<SequenceNameLocus> {

  /**
   * Create a new instance from the specified region iterator
   * @param reader the reader supplying regions
   * @return a new <code>ReferenceRegions</code>
   * @throws java.io.IOException when reading the file fails
   */
  public static ReferenceRegions regions(IOIterator<? extends SequenceNameLocus> reader) throws IOException {
    final ReferenceRegions regions = new ReferenceRegions();
    regions.add(reader);
    return regions;
  }

  final Map<String, MergedIntervals> mSequences;

  /**
   * Construct an empty region set
   */
  public ReferenceRegions() {
    mSequences = new LinkedHashMap<>();
  }

  @Override
  public Iterator<SequenceNameLocus> iterator() {
    return new Iterator<SequenceNameLocus>() {
      final Iterator<String> mSeqIt = mSequences.keySet().iterator();
      String mSequence;
      Iterator<Map.Entry<Integer, Integer>> mRegionIt = null;
      @Override
      public boolean hasNext() {
        if (mRegionIt == null || !mRegionIt.hasNext()) {
          mRegionIt = null;
          while (mSeqIt.hasNext()) {
            mSequence = mSeqIt.next();
            mRegionIt = mSequences.get(mSequence).mIntervals.entrySet().iterator();
            if (mRegionIt.hasNext()) {
              break;
            }
          }
        }
        return mRegionIt != null && mRegionIt.hasNext();
      }

      @Override
      public SequenceNameLocus next() {
        final Map.Entry<Integer, Integer> entry = mRegionIt.next();
        return new SequenceNameLocusSimple(mSequence, entry.getKey(), entry.getValue());
      }
    };
  }

  /**
   * @return the names of all sequences referenced by these regions
   */
  public Collection<String> sequenceNames() {
    return mSequences.keySet();
  }

  private MergedIntervals getOrAdd(String name) {
    return mSequences.computeIfAbsent(name, k -> new MergedIntervals());
  }

  /**
   * Modifies this instance to be the intersection of this and provided regions
   * @param regions regions to intersect with
   */
  public void intersect(ReferenceRegions regions) {
    for (String seq : sequenceNames()) {
      int outStart = 0;
      final MergedIntervals theirs = regions.mSequences.get(seq);
      final MergedIntervals ours = mSequences.get(seq);
      if (theirs != null) {
        for (Map.Entry<Integer, Integer> interval : theirs.mIntervals.entrySet()) {
          final int outEnd = interval.getKey();
          ours.subtract(outStart, outEnd);
          outStart = interval.getValue();
        }
        ours.subtract(outStart, Integer.MAX_VALUE);
      }
    }
  }

  /**
   * Add all regions returned by an iterator to the set
   * @param reader supplies the regions to add
   * @throws java.io.IOException when reading the region stream fails
   */
  public void add(IOIterator<? extends SequenceNameLocus> reader) throws IOException {
    while (reader.hasNext()) {
      add(reader.next());
    }
  }

  /**
   * Subtract all regions returned by iterator from the set
   * @param reader supplies the regions to add
   * @throws java.io.IOException when reading the region stream fails
   */
  public void subtract(IOIterator<? extends SequenceNameLocus> reader) throws IOException {
    while (reader.hasNext()) {
      subtract(reader.next());
    }
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
   * Subtract a new region from the set
   * @param region the region to subtract
   */
  public void subtract(SequenceNameLocus region) {
    final MergedIntervals intervals = mSequences.get(region.getSequenceName());
    if (intervals != null) {
      intervals.subtract(region);
    }
  }

  /**
   * @param sequence name of a sequence
   * @return a mask that can be used for fast containment tests, or null if the sequence does not exist in the regions
   */
  public BitSet mask(String sequence) {
    final MergedIntervals mergedIntervals = mSequences.get(sequence);
    return mergedIntervals == null ? null : mergedIntervals.mask();
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
    final Map<String, Integer> map = new HashMap<>(mSequences.size());
    for (Map.Entry<String, MergedIntervals> entry : mSequences.entrySet()) {
      map.put(entry.getKey(), entry.getValue().totalLength());
    }
    return map;
  }

  /** @return the number of regions */
  public int size() {
    return mSequences.values().stream().mapToInt(MergedIntervals::size).sum();
  }

  /**
   * @return these ReferenceRegions as a ReferenceRanges data structure
   */
  public ReferenceRanges<String> toReferenceRanges() {
    final ReferenceRanges.Accumulator<String> rangeData = new ReferenceRanges.Accumulator<>();
    for (SequenceNameLocus r : this) {
      rangeData.addRangeData(r.getSequenceName(), new SimpleRangeMeta<>(r.getStart(), r.getEnd(), ""));
    }
    return rangeData.getReferenceRanges();
  }

  /**
   * Writes regions out to bed
   * Note: this method closes the Writer that is wrapped around the stream
   * @param stream destination for bed
   * @throws IOException if an IO error occurs
   */
  public void toBed(OutputStream stream) throws IOException {
    try (BedWriter bw = new BedWriter(stream)) {
      for (SequenceNameLocus r : this) {
        bw.write(new BedRecord(r.getSequenceName(), r.getStart(), r.getEnd()));
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (SequenceNameLocus r : this) {
      sb.append(String.format("%s\t%d\t%d%n", r.getSequenceName(), r.getStart(), r.getEnd()));
    }
    return sb.toString();
  }
}
