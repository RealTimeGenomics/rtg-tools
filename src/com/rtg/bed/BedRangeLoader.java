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

package com.rtg.bed;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.RangeList.RangeData;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;

/**
 * @param <T> type of annotation
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public abstract class BedRangeLoader<T> {

  /**
   * Convenience method to load BED ranges from a file using the supplied loader
   * @param brl the BED range loader
   * @param bedFile the BED file
   * @param <U> type of data associated with the ReferenceRanges
   * @return the ReferenceRanges
   * @throws IOException if an exception occurs while reading a BED file
   */
  public static <U> ReferenceRanges<U> getReferenceRanges(BedRangeLoader<U> brl, File bedFile) throws IOException {
    brl.loadRanges(bedFile);
    return brl.getReferenceRanges();
  }

  private final ReferenceRanges.Accumulator<T> mRangeData = new ReferenceRanges.Accumulator<>();

  private final int mMinAnnotations;
  private int mExtendWarningCount = 0;

  /**
   *
   * @param minAnnotations the minimum number of annotation columns each record in the bed file must contain
   */
  protected BedRangeLoader(int minAnnotations) {
    mMinAnnotations = minAnnotations;
  }

  /**
   * Loads annotations from a set of bed files.
   * @param bedFiles the bed files containing regions to load
   * @throws IOException if an exception occurs while reading a BED file
   */
  public void loadRanges(Collection<File> bedFiles) throws IOException {
    for (final File bedFile : bedFiles) {
      loadRanges(bedFile);
    }
    if (mExtendWarningCount >= 10) {
      Diagnostic.warning("Zero length range extension occurred " + mExtendWarningCount + " times.");
    }
  }

  /**
   * Loads annotations from a bed file.
   * @param region region to load or null for entire file
   * @param bedFile the bed file containing regions to load
   * @throws IOException if an exception occurs while reading a BED file
   */
  public void loadRanges(RegionRestriction region, File bedFile) throws IOException {
    try (BedReader reader = BedReader.openBedReader(region, bedFile, mMinAnnotations)) {
      loadRanges(reader);
    }
  }

  /**
   * Loads annotations from a bed file.
   * @param bedFile the bed file containing regions to load
   * @throws IOException if an exception occurs while reading a BED file
   */
  public void loadRanges(File bedFile) throws IOException {
    loadRanges(null, bedFile);
  }

  private void loadRanges(BedReader reader) throws IOException {
    while (reader.hasNext()) {
      final BedRecord rec = reader.next();
      mRangeData.addRangeData(rec.getSequenceName(), getRangeData(rec));
    }
  }

  /**
   * Merges overlaps between regions and returns the result as a map of <code>RangeList</code> objects, keyed by chromosome name.
   *
   * @return a map of <code>RangeList</code> objects keyed by chromosome name.
   */
  public ReferenceRanges<T> getReferenceRanges() {
    return mRangeData.getReferenceRanges();
  }

  /**
   * Get a range data item corresponding to the given BED record.
   * @param rec the bed record.
   * @return the range data item, or null if this record should be skipped
   */
  protected RangeData<T> getRangeData(BedRecord rec) {
    final int start = rec.getStart();
    int end = rec.getEnd();
    if (end == start) {
      ++end;
      // warning - need to have a range of at least 1
      if (mExtendWarningCount < 10) {
        Diagnostic.warning("Zero length range in BED record, extending end by 1 : " + rec.toString());
      }
      ++mExtendWarningCount;
    }

    return new RangeData<>(start, end, getMeta(rec));
  }

  /**
   * Get the <code>metadata</code> annotation for the specified bed record
   * @param rec the bed record to return a <code>metadata</code> annotation from
   * @return a <code>metadata</code> annotation
   */
  protected abstract T getMeta(BedRecord rec);

}
