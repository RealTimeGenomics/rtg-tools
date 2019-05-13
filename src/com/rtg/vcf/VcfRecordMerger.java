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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rtg.util.MultiMap;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.header.VcfHeader;

/**
 * Provides routines for merging VCF records.
 */
public class VcfRecordMerger implements AutoCloseable {

  /** Maximum number of duplicate warnings to explicitly print. */
  public static final long DUPLICATE_WARNINGS_TO_PRINT = 5;
  private long mMultipleRecordsForSampleCount = 0;
  private final String mDefaultFormat;
  private final boolean mPaddingAware;

  /**
   * Constructor
   */
  public VcfRecordMerger() {
    this(VcfUtils.FORMAT_GENOTYPE, true);
  }

  /**
   * Constructor
   * @param defaultFormat the ID of the FORMAT field to fall back to when merging sample-free VCFs with those containing samples
   * @param paddingAware if true, do not merge records containing padding bases with those without padding bases, to avoid semantic pollution
   */
  public VcfRecordMerger(String defaultFormat, boolean paddingAware) {
    mDefaultFormat = defaultFormat;
    mPaddingAware = paddingAware;
  }

  // Builds the union of alt alleles plus a map converting allele indices from input
  // records to the union indices.
  static class AlleleMap {
    boolean mAltsChanged = false;
    final int[][] mGtMap;
    AlleleMap(String refCall, List<String> alts, VcfRecord[] records) {
      if (records.length == 1) {
        mGtMap = null;
        alts.addAll(records[0].getAltCalls());
      } else {
        mGtMap = new int[records.length][];
        for (int i = 0; i < records.length; ++i) {
          final VcfRecord vcf = records[i];
          final int numAlts = vcf.getAltCalls().size();
          mGtMap[i] = new int[numAlts + 1];
          for (int j = 0; j < numAlts; ++j) {
            final String alt = VcfUtils.normalizeAllele(vcf.getAltCalls().get(j));
            if (alt.equals(refCall)) {
              mGtMap[i][j + 1] = 0;
              mAltsChanged = true;
            } else {
              int altIndex = alts.indexOf(alt);
              if (altIndex == -1) {
                altIndex = alts.size();
                alts.add(alt);
              }
              mGtMap[i][j + 1] = altIndex + 1;
              if (j != altIndex) {
                mAltsChanged = true;
              }
            }
          }
          if (numAlts != alts.size()) {
            mAltsChanged = true;
          }
        }
      }
    }

    // If true, it means that GT indices require remapping
    boolean altsChanged() {
      return mAltsChanged;
    }

    String remapGt(VcfRecord[] records, int recordIndex, int sampleIndex) {
      assert mGtMap != null || recordIndex == 0 && records.length == 1;
      final VcfRecord record = records[recordIndex];
      final String gtStr = record.getFormat(VcfUtils.FORMAT_GENOTYPE).get(sampleIndex);
      if (mGtMap == null) { // Shortcut
        return gtStr;
      }
      final int[] splitGt = splitRemapGt(gtStr, record, recordIndex);
      return VcfUtils.joinGt(VcfUtils.isPhasedGt(gtStr), splitGt);
    }

    // Split a GT string into remapped allele indices
    int[] splitRemapGt(String gtStr, VcfRecord record, int recordIndex) {
      final int[] splitGt = VcfUtils.splitGt(gtStr);
      return remapGt(splitGt, record, recordIndex);
    }

    int[] remapGt(int[] splitGt, VcfRecord record, int recordIndex) {
      if (mGtMap != null) {
        for (int gti = 0; gti < splitGt.length; ++gti) {
          if (splitGt[gti] != -1) {
            if (splitGt[gti] >= mGtMap[recordIndex].length) {
              throw new VcfFormatException("Invalid GT allele index " + splitGt[gti] + " in input record: " + record);
            }
            splitGt[gti] = mGtMap[recordIndex][splitGt[gti]];
          }
        }
      }
      return splitGt;
    }
  }

  /**
   * Merges multiple VCF records into one VCF record
   *
   * May refuse to merge (by returning NULL) if there are problems merging
   * the records. In particular where the input records do not have the same
   * set of ALTs and where the records also use any FORMAT type contained
   * in <code>unmergeableFormatFields</code>.
   *
   * @param records the VCF records to be merged
   * @param headers the headers for each of the VCF records to be merged
   * @param destHeader the header for the resulting VCF record
   * @param unmergeableFormatFields the set of alternate allele based format tags that cannot be meaningfully merged
   * @param dropUnmergeable if true, alow merge to proceed when encountering any non-mergeable FORMAT fields or duplicate records for the same sample, by dropping appropriate information.
   * @return the merged VCF record, or NULL if there are problems with merging them
   */
  public VcfRecord mergeRecordsWithSameRef(VcfRecord[] records, VcfHeader[] headers, VcfHeader destHeader, Set<String> unmergeableFormatFields, boolean dropUnmergeable) {
    final String refCall = VcfUtils.normalizeAllele(records[0].getRefCall());
    checkConsistentRefAllele(refCall, records);

    final VcfRecord merged = new VcfRecord(records[0].getSequenceName(), records[0].getStart(), refCall);

    mergeIds(merged, records);

    // Merge ALTs and create mapping for use in GT translation
    final AlleleMap map = new AlleleMap(refCall, merged.getAltCalls(), records);

    merged.setQuality(records[0].getQuality());
    merged.getFilters().addAll(records[0].getFilters());

    // Copy INFO from first record into destination (INFO from other records are ignored)
    for (final Map.Entry<String, ArrayList<String>> entry : records[0].getInfo().entrySet()) {
      merged.getInfo().computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).addAll(entry.getValue());
    }

    if (!mergeSamples(records, headers, merged, destHeader, map, unmergeableFormatFields, dropUnmergeable)) {
      return null;
    }
    return merged;
  }

  protected boolean mergeSamples(VcfRecord[] records, VcfHeader[] headers, VcfRecord dest, VcfHeader destHeader, AlleleMap map, Set<String> unmergeableFormatFields, boolean dropUnmergeable) {
    dest.setNumberOfSamples(destHeader.getNumberOfSamples());
    final List<String> names = destHeader.getSampleNames();
    for (int destSampleIndex = 0; destSampleIndex < names.size(); ++destSampleIndex) {
      boolean sampleDone = false;
      for (int i = 0; i < headers.length; ++i) {
        final int sampleIndex = headers[i].getSampleNames().indexOf(names.get(destSampleIndex));
        if (sampleIndex > -1) {
          if (sampleDone) {
            if (dropUnmergeable) {
              if (++mMultipleRecordsForSampleCount <= DUPLICATE_WARNINGS_TO_PRINT) {
                Diagnostic.warning("Multiple records found at position: " + dest.getSequenceName() + ":" + dest.getOneBasedStart() + " for sample: " + names.get(destSampleIndex) + ". Keeping first.");
              }
              continue;
            } else {
              return false;
            }
          }
          sampleDone = true;
          for (final String key : records[i].getFormats()) {
            ArrayList<String> field = dest.getFormat(key);
            if (field == null) {
              field = new ArrayList<>();
              dest.getFormatAndSample().put(key, field);
            }
            while (field.size() <= destSampleIndex) {
              field.add(VcfRecord.MISSING);
            }
            if (VcfUtils.FORMAT_GENOTYPE.equals(key)) {
              field.set(destSampleIndex, map.remapGt(records, i, sampleIndex));
            } else {
              field.set(destSampleIndex, records[i].getFormat(key).get(sampleIndex));
            }
          }
        }
      }
    }
    if (map.altsChanged()) {
      final Set<String> formats = dest.getFormats();
      for (String field : unmergeableFormatFields) {
        if (formats.contains(field)) {
          if (dropUnmergeable) {
            dest.getFormatAndSample().remove(field);
          } else {
            return false;
          }
        }
      }
    }
    padSamples(dest, destHeader);
    return true;
  }

  // Ensure format fields of samples in record are appropriately padded
  protected void padSamples(VcfRecord dest, VcfHeader destHeader) {
    if (destHeader.getNumberOfSamples() > 0 && dest.getFormats().isEmpty()) { // When mixing sample-free and with-sample VCFs, need to ensure at least one format field
      dest.addFormat(mDefaultFormat);
    }
    for (final String key : dest.getFormats()) {
      final ArrayList<String> field = dest.getFormat(key);
      while (field.size() < destHeader.getNumberOfSamples()) {
        field.add(VcfRecord.MISSING);
      }
    }
  }

  protected void mergeIds(VcfRecord dest, VcfRecord[] records) {
    final Set<String> uniqueIds = new LinkedHashSet<>();
    for (final VcfRecord vcf : records) {
      final String[] ids = StringUtils.split(vcf.getId(), VcfUtils.VALUE_SEPARATOR);
      Collections.addAll(uniqueIds, ids);
    }
    dest.setId(StringUtils.join(VcfUtils.VALUE_SEPARATOR, uniqueIds));
  }

  private void checkConsistentRefAllele(String refCall, VcfRecord[] records) {
    final int pos = records[0].getStart();
    final int length = records[0].getLength();
    for (final VcfRecord vcf : records) {
      if (pos != vcf.getStart() || length != vcf.getLength()) { // TODO: Handle gVCF merging
        throw new RuntimeException("Attempt to merge records with different reference span at: " + new SequenceNameLocusSimple(records[0]));
      } else if (!refCall.equalsIgnoreCase(vcf.getRefCall())) {
        throw new VcfFormatException("Records at " + new SequenceNameLocusSimple(records[0]) + " disagree on what the reference bases should be! (" + refCall + " != " + vcf.getRefCall() + ")");
      }
    }
  }

  /**
   * Perform merge operation on a set of records with the same start position, batching up into separate merge operations for each reference span.
   * @param records the records to merge
   * @param headers the VcfHeader corresponding to each record
   * @param destHeader the VcfHeader of of the destination VCF
   * @param unmergeableFormatFields the set of alternate allele based format tags that cannot be meaningfully merged
   * @param preserveFormats if true, any non-mergeable FORMAT fields will be kept (resulting non-merged records), otherwise dropped, allowing merge to proceed.
   * @return the merged records
   */
  VcfRecord[] mergeRecords(VcfRecord[] records, VcfHeader[] headers, VcfHeader destHeader, Set<String> unmergeableFormatFields, boolean preserveFormats) {
    assert records.length == headers.length;
    final MultiMap<Integer, VcfRecord> recordSets = new MultiMap<>(true);
    final MultiMap<Integer, VcfHeader> headerSets = new MultiMap<>(true);
    for (int i = 0; i < records.length; ++i) {
      final int key = mPaddingAware && VcfUtils.hasRedundantFirstNucleotide(records[i]) ? -records[i].getLength() : records[i].getLength();
      recordSets.put(key, records[i]);
      headerSets.put(key, headers[i]);
    }
    final ArrayList<VcfRecord> ret = new ArrayList<>();
    for (Integer key : recordSets.keySet()) {
      final Collection<VcfRecord> recs = recordSets.get(key);
      final Collection<VcfHeader> heads = headerSets.get(key);
      final VcfRecord[] recsArray = recs.toArray(new VcfRecord[recs.size()]);
      final VcfHeader[] headsArray = heads.toArray(new VcfHeader[heads.size()]);
      final VcfRecord merged = mergeRecordsWithSameRef(recsArray, headsArray, destHeader, unmergeableFormatFields, !preserveFormats);
      if (merged != null) {
        ret.add(merged);
      } else {
        final VcfRecord[] recHolder = new VcfRecord[1];
        final VcfHeader[] headHolder = new VcfHeader[1];
        for (int i = 0; i < recsArray.length; ++i) {
          recHolder[0] = recsArray[i];
          headHolder[0] = headsArray[i];
          final VcfRecord r = mergeRecordsWithSameRef(recHolder, headHolder, destHeader, unmergeableFormatFields, !preserveFormats);
          if (r == null) {
            throw new IllegalArgumentException("VCF record could not be converted to destination structure:\n" + recHolder[0]);
          }
          ret.add(r);
        }
      }
    }
    return ret.toArray(new VcfRecord[ret.size()]);
  }

  @Override
  public void close() {
    if (mMultipleRecordsForSampleCount > VcfRecordMerger.DUPLICATE_WARNINGS_TO_PRINT) {
      Diagnostic.warning("A total of " + mMultipleRecordsForSampleCount + " loci had multiple records for a sample.");
    }
  }
}
