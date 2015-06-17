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
package com.rtg.sam;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rtg.bed.BedRangeLoader;
import com.rtg.bed.BedRecord;
import com.rtg.reader.SequencesReader;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.intervals.Range;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

/**
 * Utilities for dealing with lists of ranges / regions to be applied when loading SAM.
 */
public final class SamRangeUtils {

  private SamRangeUtils() {
  }

  /**
   * Make a reference range list from whatever the params say
   * @param header the SAM header containing sequence information
   * @param params supplies restriction range configuration
   * @return the ReferenceRanges lookup
   * @throws java.io.IOException if we could not load a required BED file.
   */
  public static ReferenceRanges<String> createReferenceRanges(SAMFileHeader header, SamFilterParams params) throws IOException {
    final ReferenceRanges<String> nameRangeMap;
    if (params.bedRegionsFile() != null) {
      nameRangeMap = createBedReferenceRanges(header, params.bedRegionsFile());
    } else if (params.restriction() != null) { // Single restriction region
      final SamRegionRestriction regionRestriction = params.restriction();
      nameRangeMap = createSingleReferenceRange(header, regionRestriction);
    } else {  // no restriction, add full ranges for each sequence
      nameRangeMap = createFullReferenceRanges(header);
    }

    // In theory the above range loading methods have already performed appropriate validation of sequence names and boundaries
    // Let's validate here just in case.
    validateRanges(header, nameRangeMap);
    return nameRangeMap;
  }


  /**
   * Make a reference range list from all regions contained in a BED file
   * @param header the SAM header containing sequence information
   * @param bedFile the BED file to load
   * @return the ReferenceRanges lookup
   * @throws java.io.IOException if there was a problem loading the BED file
   */
  public static ReferenceRanges<String> createBedReferenceRanges(SAMFileHeader header, File bedFile) throws IOException {
    final BedRangeLoader<String> brl = new ResolvedBedRangeLoader(header);
    brl.loadRanges(bedFile);
    final ReferenceRanges<String> ranges = brl.getReferenceRanges();
    ranges.setIdMap(SamUtils.getSequenceIdLookup(header.getSequenceDictionary()));
    return ranges;
  }

  /**
   * Make a reference range list from all regions contained in a BED file. These have not had the ranges checked against
   * any sequence dictionary, or any id map set.
   * @param bedFile the BED file to load
   * @return the ReferenceRanges lookup
   * @throws java.io.IOException if there was a problem loading the BED file
   */
  public static ReferenceRanges<String> createBedReferenceRanges(File bedFile) throws IOException {
    final BedRangeLoader<String> brl = new SimpleBedRangeLoader();
    brl.loadRanges(bedFile);
    return brl.getReferenceRanges();
  }


  /**
   * Make a reference range list corresponding to the full length of all reference sequences
   * @param header the SAM header containing sequence information
   * @return the ReferenceRanges lookup
   */
  public static ReferenceRanges<String> createFullReferenceRanges(SAMFileHeader header) {
    final ReferenceRanges<String> rangeMap = new ReferenceRanges<>(true);
    for (final SAMSequenceRecord r : header.getSequenceDictionary().getSequences()) {
      final int rlen = r.getSequenceLength();
      if (rlen > 0) {
        rangeMap.put(r.getSequenceName(), new RangeList<>(new RangeList.RangeData<>(0, rlen, r.getSequenceName())));
      }
    }
    rangeMap.setIdMap(SamUtils.getSequenceIdLookup(header.getSequenceDictionary()));
    return rangeMap;
  }

  /**
   * Make a reference range list corresponding to the full length of all reference sequences
   * @param sequencesReader sequences reader used to determine sequence names and lengths
   * @return the ReferenceRanges lookup
   * @throws IOException if an I/O error occurs
   */
  public static ReferenceRanges<String> createFullReferenceRanges(SequencesReader sequencesReader) throws IOException {
    final ReferenceRanges<String> rangeMap = new ReferenceRanges<>(false);
    final Map<String, Integer> idMap = new HashMap<>();
    for (int k = 0; k < sequencesReader.numberSequences(); k++) {
      final int rlen = sequencesReader.length(k);
      if (rlen > 0) {
        final String name = sequencesReader.names().name(k);
        rangeMap.put(name, new RangeList<>(new RangeList.RangeData<>(0, rlen, name)));
        idMap.put(name, k);
      }
    }
    rangeMap.setIdMap(idMap);
    return rangeMap;
  }

  /**
   * Make a reference range list from a single SamRegionRestriction
   * @param header the SAM header containing sequence information
   * @param regionRestriction the region
   * @return the ReferenceRanges lookup
   */
  public static ReferenceRanges<String> createSingleReferenceRange(SAMFileHeader header, SamRegionRestriction regionRestriction) {
    final ReferenceRanges<String> rangeMap = new ReferenceRanges<>(false);
    final SequenceNameLocus resolved = resolveRestriction(header.getSequenceDictionary(), regionRestriction);
    rangeMap.put(resolved.getSequenceName(), new RangeList<>(new RangeList.RangeData<>(resolved, regionRestriction.toString())));
    rangeMap.setIdMap(SamUtils.getSequenceIdLookup(header.getSequenceDictionary()));
    return rangeMap;
  }

  /**
   * Make a reference range list from a multiple SamRegionRestrictions
   * @param header the SAM header containing sequence information
   * @param regions the region
   * @return the ReferenceRanges lookup
   */
  public static ReferenceRanges<String> createExplicitReferenceRange(SAMFileHeader header, SamRegionRestriction... regions) {
    final ReferenceRanges.Accumulator<String> acc = new ReferenceRanges.Accumulator<>();
    for (SamRegionRestriction region : regions) {
      final SequenceNameLocus resolved = resolveRestriction(header.getSequenceDictionary(), region);
      acc.addRangeData(resolved.getSequenceName(), new RangeList.RangeData<>(resolved, region.toString()));
    }
    final ReferenceRanges<String> ranges = acc.getReferenceRanges();
    ranges.setIdMap(SamUtils.getSequenceIdLookup(header.getSequenceDictionary()));
    return ranges;
  }

  /**
   * This is a somewhat dodgy method of making a ReferenceRanges corresponding to a single region restriction where no
   * sequence length information is available. Sets missing start to Integer.MIN_VALUE and missing end to Integer.MAX_VALUE. Note
   * that this has not had the sequence id mapping information supplied either.
   * @param regions the region restrictions
   * @return the ReferenceRanges
   * @throws java.lang.NullPointerException if any of the regions is null
   */
  public static ReferenceRanges<String> createExplicitReferenceRange(SequenceNameLocus... regions) {
    final ReferenceRanges.Accumulator<String> acc = new ReferenceRanges.Accumulator<>();
    for (SequenceNameLocus region : regions) {
      if (region == null || region.getSequenceName() == null) {
        throw new NullPointerException();
      }
      final Range wideRange = new Range(region.getStart() == RegionRestriction.MISSING ? Integer.MIN_VALUE : region.getStart(),
        region.getEnd() == RegionRestriction.MISSING ? Integer.MAX_VALUE : region.getEnd());
      acc.addRangeData(region.getSequenceName(), new RangeList.RangeData<>(wideRange, region.toString()));
    }
    return acc.getReferenceRanges();
  }


  // Validation of the supplied ranges against names and lengths in SequenceDictionary
  static <T> void validateRanges(SAMFileHeader header, ReferenceRanges<T> rangeMap) {
    for (final String seq : rangeMap.sequenceNames()) {
      final SAMSequenceRecord r  = header.getSequenceDictionary().getSequence(seq);
      if (r == null) {
        throw new NoTalkbackSlimException("Sequence \"" + seq + "\" referenced in regions not found in the SAM sequence dictionary.");
      }

      if (r.getSequenceLength() > 0) {
        final RangeList<T> rs = rangeMap.get(seq);
        if (rs != null) {
          final List<RangeList.RangeData<T>> ranges = rs.getRangeList();
          final RangeList.RangeData<T> last = ranges.get(ranges.size() - 1);
          if (last.getEnd() >  r.getSequenceLength()) {
            throw new NoTalkbackSlimException("Specified sequence range (" + r.getSequenceName() + ":" + last.toString() + ") is outside the length of the sequence (" + r.getSequenceLength() + ")");
          }
        }
      }
    }
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param range the range
   * @param dictionary the dictionary with which to validate/resolve the range
   * @return the resolved range.
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static SequenceNameLocus resolveRestriction(SAMSequenceDictionary dictionary, SequenceNameLocus range) {
    final SAMSequenceRecord sequence = dictionary.getSequence(range.getSequenceName());
    if (sequence == null) {
      throw new NoTalkbackSlimException("Sequence \"" + range.getSequenceName() + "\" referenced in region was not found in the SAM sequence dictionary.");
    }
    final int start = range.getStart() == SamRegionRestriction.MISSING ? 0 : range.getStart();
    final int length = sequence.getSequenceLength();
    if (start > length || (length != 0 && start == length)) {  // Allow start == 0 if empty sequence
      throw new NoTalkbackSlimException("The start position \"" + start + "\" must be less than than length of the sequence \"" + length + "\".");
    }
    int end = range.getEnd() == LongRange.MISSING ? length : range.getEnd();
    if (end > length) {
      Diagnostic.warning("The end position \"" + range.getEnd() + "\" is outside the length of the sequence (" + length
        + "). Defaulting end to \"" + length + "\"");
      end = length;
    }
    return new SequenceNameLocusSimple(range.getSequenceName(), start, end);
  }


  /** Loads BED records into range data that has either name (if present) or string representation of the region as meta data */
  private static class SimpleBedRangeLoader extends BedRangeLoader<String> {
    SimpleBedRangeLoader() {
      super(0);
    }

    @Override
    protected RangeList.RangeData<String> getRangeData(BedRecord rec) {
      SequenceNameLocus region = rec;
      if (region.getEnd() == region.getStart()) {
        region = new SequenceNameLocusSimple(rec.getSequenceName(), rec.getStart(), rec.getEnd() + 1);
      }
      return new RangeList.RangeData<>(region, getMeta(rec));
    }

    @Override
    protected String getMeta(final BedRecord rec) {
      if (rec.getAnnotations() != null && rec.getAnnotations().length > 0) {
        return rec.getAnnotations()[0];
      }
      return rec.getSequenceName() + ":" + (rec.getStart() + 1) + "-" + rec.getEnd();
    }
  }

  /** Adds checking that referenced sequences and coordinates are valid */
  private static class ResolvedBedRangeLoader extends SimpleBedRangeLoader {
    private final SAMSequenceDictionary mDictionary;

    ResolvedBedRangeLoader(SAMFileHeader header) {
      super();
      mDictionary = header.getSequenceDictionary();
    }

    @Override
    protected RangeList.RangeData<String> getRangeData(BedRecord rec) {
      SequenceNameLocus region = rec;
      if (region.getEnd() == region.getStart()) {
        region = new SequenceNameLocusSimple(rec.getSequenceName(), rec.getStart(), rec.getEnd() + 1);
      }

      final SequenceNameLocus r = resolveRestriction(mDictionary, region);
      return new RangeList.RangeData<>(r, getMeta(rec));
    }

  }
}
