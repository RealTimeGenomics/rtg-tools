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
   * @param dropUnmergeable if true, any non-mergeable FORMAT fields will be dropped, allowing merge to proceed.
   * @return the merged VCF record, or NULL if there are problems with merging them
   */
  public VcfRecord mergeRecordsWithSameRef(VcfRecord[] records, VcfHeader[] headers, VcfHeader destHeader, Set<String> unmergeableFormatFields, boolean dropUnmergeable) {
    final String refCall = VcfUtils.normalizeAllele(records[0].getRefCall());
    final int pos = records[0].getStart();
    final int length = records[0].getLength();
    final Set<String> uniqueIds = new LinkedHashSet<>();
    for (final VcfRecord vcf : records) {
      if (pos != vcf.getStart() || length != vcf.getLength()) { // TODO: Handle gVCF merging
        throw new RuntimeException("Attempt to merge records with different reference span at: " + new SequenceNameLocusSimple(records[0]));
      } else if (!refCall.equalsIgnoreCase(vcf.getRefCall())) {
        throw new VcfFormatException("Records at " + new SequenceNameLocusSimple(records[0]) + " disagree on what the reference bases should be! (" + refCall + " != " + vcf.getRefCall() + ")");
      }
      final String[] ids = StringUtils.split(vcf.getId(), VcfUtils.VALUE_SEPARATOR);
      Collections.addAll(uniqueIds, ids);
    }
    final VcfRecord merged = new VcfRecord(records[0].getSequenceName(), records[0].getStart(), refCall);

    final StringBuilder idsb = new StringBuilder();
    int z = 0;
    for (final String id : uniqueIds) {
      if (z > 0) {
        idsb.append(VcfUtils.VALUE_SEPARATOR);
      }
      idsb.append(id);
      ++z;
    }
    merged.setId(idsb.toString());
    boolean altsChanged = false;
    final int[][] gtMap = new int[records.length][];
    final List<String> mergedAltCalls = merged.getAltCalls();
    for (int i = 0; i < records.length; ++i) {
      final VcfRecord vcf = records[i];
      final int numAlts = vcf.getAltCalls().size();
      gtMap[i] = new int[numAlts + 1];
      for (int j = 0; j < numAlts; ++j) {
        final String alt = VcfUtils.normalizeAllele(vcf.getAltCalls().get(j));
        if (alt.equals(refCall)) {
          gtMap[i][j + 1] = 0;
          altsChanged = true;
        } else {
          int altIndex = mergedAltCalls.indexOf(alt);
          if (altIndex == -1) {
            altIndex = mergedAltCalls.size();
            mergedAltCalls.add(alt);
          }
          gtMap[i][j + 1] = altIndex + 1;
          if (j != altIndex) {
            altsChanged = true;
          }
        }
      }
      if (numAlts != mergedAltCalls.size()) {
        altsChanged = true;
      }
    }
    merged.setQuality(records[0].getQuality());
    merged.getFilters().addAll(records[0].getFilters());
    merged.setNumberOfSamples(destHeader.getNumberOfSamples());
    for (final Map.Entry<String, ArrayList<String>> entry : records[0].getInfo().entrySet()) {
      ArrayList<String> val = merged.getInfo().get(entry.getKey());
      if (val == null) {
        val = new ArrayList<>();
        merged.getInfo().put(entry.getKey(), val);
      }
      for (final String s : entry.getValue()) {
        val.add(s);
      }
    }

    final List<String> names = destHeader.getSampleNames();
    for (int destSampleIndex = 0; destSampleIndex < names.size(); ++destSampleIndex) {
      boolean sampleDone = false;
      for (int i = 0; i < headers.length; ++i) {
        final int sampleIndex = headers[i].getSampleNames().indexOf(names.get(destSampleIndex));
        if (sampleIndex > -1) {
          if (sampleDone) {
            if (++mMultipleRecordsForSampleCount <= DUPLICATE_WARNINGS_TO_PRINT) {
              Diagnostic.warning("Multiple records found at position: " + merged.getSequenceName() + ":" + merged.getOneBasedStart() + " for sample: " + names.get(destSampleIndex) + ". Keeping first.");
            }
            continue;
          }
          sampleDone = true;
          for (final String key : records[i].getFormats()) {
            ArrayList<String> field = merged.getFormat(key);
            if (field == null) {
              field = new ArrayList<>();
              merged.getFormatAndSample().put(key, field);
            }
            while (field.size() <= destSampleIndex) {
              field.add(VcfRecord.MISSING);
            }
            if (key.equals(VcfUtils.FORMAT_GENOTYPE)) {
              final String gtStr = records[i].getFormat(key).get(sampleIndex);
              final int[] splitGt = VcfUtils.splitGt(gtStr);
              for (int gti = 0; gti < splitGt.length; ++gti) {
                if (splitGt[gti] != -1) {
                  if (splitGt[gti] >= gtMap[i].length) {
                    throw new VcfFormatException("Invalid GT " + gtStr + " in input record: " + records[i]);
                  }
                  splitGt[gti] = gtMap[i][splitGt[gti]];
                }
              }
              final char sep = gtStr.indexOf(VcfUtils.PHASED_SEPARATOR) != -1 ? VcfUtils.PHASED_SEPARATOR : VcfUtils.UNPHASED_SEPARATOR;
              final StringBuilder sb = new StringBuilder();
              sb.append(splitGt[0] == -1 ? VcfRecord.MISSING : splitGt[0]);
              for (int gti = 1; gti < splitGt.length; ++gti) {
                sb.append(sep).append(splitGt[gti] == -1 ? VcfRecord.MISSING : splitGt[gti]);
              }
              field.set(destSampleIndex, sb.toString());
            } else {
              field.set(destSampleIndex, records[i].getFormat(key).get(sampleIndex));
            }
          }
        }
      }
    }
    if (names.size() > 0 && merged.getFormats().size() == 0) { // When mixing sample-free and with-sample VCFs, need to ensure at least one format field
      merged.addFormat(mDefaultFormat);
    }
    for (final String key : merged.getFormats()) {
      final ArrayList<String> field = merged.getFormat(key);
      while (field.size() < destHeader.getNumberOfSamples()) {
        field.add(VcfRecord.MISSING);
      }
    }
    if (altsChanged) {
      final Set<String> formats = merged.getFormats();
      for (String field : unmergeableFormatFields) {
        if (formats.contains(field)) {
          if (dropUnmergeable) {
            merged.getFormatAndSample().remove(field);
          } else {
            return null;
          }
        }
      }
    }
    return merged;
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
          ret.add(mergeRecordsWithSameRef(recHolder, headHolder, destHeader, unmergeableFormatFields, !preserveFormats));
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
