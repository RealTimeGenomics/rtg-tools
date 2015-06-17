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

import static com.rtg.util.StringUtils.TAB;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.rtg.util.MultiMap;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.vcf.header.VcfHeader;

/**
 * Class to hold a single VCF record
 */
public class VcfRecord implements SequenceNameLocus {

  /** VCF missing value string **/
  public static final String MISSING = "" + VcfUtils.MISSING_VALUE;

  private static final String FORMAT_AND_SAMPLE_SEPARATOR = ":";
  private static final String FILTER_AND_INFO_SEPARATOR = ";";
  private static final String ALT_CALL_INFO_SEPARATOR = ",";

  private String mSequence;
  private int mStart = -1;
  private String mId;
  private String mRefCall;
  private final List<String> mAltCalls;
  private String mQual;
  private final List<String> mFilters;

  private int mNumSamples;

  /**
   * Each <code>Key=Value;</code> entry in the <code>INFO</code> field becomes
   * one (Key,Value) entry in this map.
   */
  private final Map<String, ArrayList<String>> mInfo;

  /**
   * Maps from each format key to all the values of that key for each sample.
   * For example, <code>"GT:GQ 0|0:48 1|0:49"</code> would give:
   *  <code>"GT"</code> maps to <code>["0|0","1|0"]</code> and
   *  <code>"GQ"</code> maps to <code>["48","49"]</code>.
   */
  private final Map<String, ArrayList<String>> mFormatAndSample;

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
  static VcfRecord mergeRecordsWithSameRef(VcfRecord[] records, VcfHeader[] headers, VcfHeader destHeader, Set<String> unmergeableFormatFields, boolean dropUnmergeable) {
    final VcfRecord merged = new VcfRecord();
    merged.mSequence = records[0].mSequence;
    merged.mStart = records[0].mStart;

    final Set<String> uniqueIds = new LinkedHashSet<>();
    for (final VcfRecord vcf : records) {
      if (merged.mRefCall == null || merged.mRefCall.length() < vcf.mRefCall.length()) {
        merged.mRefCall = vcf.mRefCall;
      }
      final String[] ids = StringUtils.split(vcf.getId(), VcfUtils.VALUE_SEPARATOR);
      Collections.addAll(uniqueIds, ids);
    }
    final StringBuilder idsb = new StringBuilder();
    int z = 0;
    for (final String id : uniqueIds) {
      if (z > 0) {
        idsb.append(VcfUtils.VALUE_SEPARATOR);
      }
      idsb.append(id);
      z++;
    }
    merged.mId = idsb.toString();
    boolean altsChanged = false;
    final int[][] gtMap = new int[records.length][];
    for (int i = 0; i < records.length; i++) {
      final VcfRecord vcf = records[i];
      gtMap[i] = new int[vcf.mAltCalls.size() + 1];
      final String refCall = merged.getRefCall();
      final int lengthDiff = merged.getRefCall().length() - vcf.getRefCall().length();
      for (int j = 0; j < vcf.mAltCalls.size(); j++) {
        final String newCall = vcf.mAltCalls.get(j) + refCall.substring(refCall.length() - lengthDiff);
        if (!newCall.equals(refCall)) {
          int altIndex = merged.mAltCalls.indexOf(newCall);
          if (altIndex == -1) {
            altIndex = merged.mAltCalls.size();
            merged.mAltCalls.add(newCall);
          }
          gtMap[i][j + 1] = altIndex + 1;
          if (j != altIndex) {
            altsChanged = true;
          }
        } else {
          gtMap[i][j + 1] = 0;
          altsChanged = true;
        }
      }
      if (vcf.mAltCalls.size() != merged.mAltCalls.size()) {
        altsChanged = true;
      }
    }
    merged.mQual = records[0].mQual;
    merged.mFilters.addAll(records[0].mFilters);
    merged.mNumSamples = destHeader.getNumberOfSamples();
    for (final Map.Entry<String, ArrayList<String>> entry : records[0].mInfo.entrySet()) {
      ArrayList<String> val = merged.mInfo.get(entry.getKey());
      if (val == null) {
        val = new ArrayList<>();
        merged.mInfo.put(entry.getKey(), val);
      }
      for (final String s : entry.getValue()) {
        val.add(s);
      }
    }

    final List<String> names = destHeader.getSampleNames();
    for (int destSampleIndex = 0; destSampleIndex < names.size(); destSampleIndex++) {
      boolean sampleDone = false;
      for (int i = 0; i < headers.length; i++) {
        final int sampleIndex = headers[i].getSampleNames().indexOf(names.get(destSampleIndex));
        if (sampleIndex > -1) {
          if (sampleDone) {
            Diagnostic.warning("Multiple records found at position: " + merged.getSequenceName() + ":" + merged.getOneBasedStart() + " for sample: " + names.get(destSampleIndex) + ". Keeping first.");
            continue;
          }
          sampleDone = true;
          for (final String key : records[i].getFormats()) {
            ArrayList<String> field = merged.getFormatAndSample().get(key);
            if (field == null) {
              field = new ArrayList<>();
              merged.mFormatAndSample.put(key, field);
            }
            while (field.size() <= destSampleIndex) {
              field.add(MISSING);
            }
            if (key.equals(VcfUtils.FORMAT_GENOTYPE)) {
              final String gtStr = records[i].getFormatAndSample().get(key).get(sampleIndex);
              final int[] splitGt = VcfUtils.splitGt(gtStr);
              for (int gti = 0; gti < splitGt.length; gti++) {
                if (splitGt[gti] != -1) {
                  splitGt[gti] = gtMap[i][splitGt[gti]];
                }
              }
              final char sep = gtStr.indexOf(VcfUtils.PHASED_SEPARATOR) != -1 ? VcfUtils.PHASED_SEPARATOR : VcfUtils.UNPHASED_SEPARATOR;
              final StringBuilder sb = new StringBuilder();
              sb.append(splitGt[0] == -1 ? MISSING : splitGt[0]);
              for (int gti = 1; gti < splitGt.length; gti++) {
                sb.append(sep).append(splitGt[gti] == -1 ? MISSING : splitGt[gti]);
              }
              field.set(destSampleIndex, sb.toString());
            } else {
              field.set(destSampleIndex, records[i].getFormatAndSample().get(key).get(sampleIndex));
            }
          }
        }
      }
    }
    for (final String key : merged.getFormats()) {
      final ArrayList<String> field = merged.getFormatAndSample().get(key);
      while (field.size() < destHeader.getNumberOfSamples()) {
        field.add(MISSING);
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

  static VcfRecord[] mergeRecords(VcfRecord[] records, VcfHeader[] headers, VcfHeader destHeader, Set<String> unmergeableFormatFields, boolean preserveFormats) {
    final MultiMap<String, VcfRecord> recordSets = new MultiMap<>(true);
    final MultiMap<String, VcfHeader> headerSets = new MultiMap<>(true);
    for (int i = 0; i < records.length; i++) {
      recordSets.put(records[i].getRefCall(), records[i]);
      headerSets.put(records[i].getRefCall(), headers[i]);
    }
    final ArrayList<VcfRecord> ret = new ArrayList<>();
    for (String key : recordSets.keySet()) {
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
        for (int i = 0; i < recsArray.length; i++) {
          recHolder[0] = recsArray[i];
          headHolder[0] = headsArray[i];
          ret.add(mergeRecordsWithSameRef(recHolder, headHolder, destHeader, unmergeableFormatFields, !preserveFormats));
        }
      }
    }
    return ret.toArray(new VcfRecord[ret.size()]);
  }

  /**
   * Construct a new VcfRecord
   */
  public VcfRecord() {
    mAltCalls = new ArrayList<>();
    mFilters = new ArrayList<>();
    mInfo = new LinkedHashMap<>();
    mFormatAndSample = new LinkedHashMap<>();
  }

  @Override
  public String getSequenceName() {
    return mSequence;
  }

  @Override
  public int getStart() {
    return mStart;
  }

  @Override
  public int getEnd() {
    return mStart + getLength();
  }

  @Override
  public int getLength() {
    return mRefCall == null ? 0 : mRefCall.length();
  }

  @Override
  public boolean overlaps(SequenceNameLocus other) {
    return SequenceNameLocusSimple.overlaps(this, other);
  }

  @Override
  public boolean contains(String sequence, int pos) {
    return SequenceNameLocusSimple.contains(this, sequence, pos);
  }

  /**
   * Set the sequence name
   * @param sequence sequence name to set
   * @return this, for call chaining
   */
  public VcfRecord setSequence(String sequence) {
    mSequence = sequence;
    return this;
  }

  /**
   * Sets the current position of the record
   * @param start position to set, 0-based
   * @return this, for call chaining
   */
  public VcfRecord setStart(int start) {
    mStart = start;
    return this;
  }

  /**
   * Gets the one-based start position
   * @return the one-based start position
   */
  public int getOneBasedStart() {
    return mStart + 1;
  }

  /**
   * @return Id
   */
  public String getId() {
    return mId == null ? MISSING : mId;
  }

  /**
   * Sets the variant ID
   * @param id id (or ids) to set
   * @return this, for call chaining
   */
  public VcfRecord setId(String... id) {
    if (id.length == 0) {
      mId = null;
    } else if (id.length == 1) {
      mId = id[0];
    } else {
      final StringBuilder ids = new StringBuilder(id[0]);
      for (int i = 1; i < id.length; i++) {
        ids.append(";").append(id[i]);
      }
      mId = ids.toString();
    }
    return this;
  }

  /**
   * @return reference call
   */
  public String getRefCall() {
    return mRefCall;
  }

  /**
   * Sets the reference allele
   * @param ref reference call to set
   * @return this, for call chaining
   */
  public VcfRecord setRefCall(String ref) {
    mRefCall = ref;
    return this;
  }

  /**
   *
   * @param altCall the next alternate call
   * @return this, for call chaining
   */
  public VcfRecord addAltCall(String altCall) {
    mAltCalls.add(altCall);
    return this;
  }

  /**
   * @return alternate calls (this should be treated as read-only).
   */
  public List<String> getAltCalls() {
    return mAltCalls;
  }

  /**
   * @return quality
   */
  public String getQuality() {
    return mQual == null ? MISSING : mQual;
  }

  /**
   * @param qual set quality values
   * @return this, for call chaining
   */
  public VcfRecord setQuality(String qual) {
    mQual = qual;
    return this;
  }

  /**
   * @return list of filters (should be treated as read-only).
   */
  public List<String> getFilters() {
    return mFilters;
  }

  /**
   * Adds a filter
   * @param filter filter to be added
   * @return this, for call chaining
   */
  public VcfRecord addFilter(String filter) {
    // AVR acts on VCF records so we don't know at construction time if this has failed AVR score threshold
    // VCF spec says the field is either PASS or semicolon separated list of failures. So if a filter is
    // added it should make sense to remove the PASS.
    mFilters.remove(VcfUtils.FILTER_PASS);
    mFilters.add(filter);
    return this;
  }

  /**
   * @return info fields (should be treated as read-only).
   */
  public Map<String, ArrayList<String>> getInfo() {
    return mInfo;
  }

  /**
   * Adds an info field
   * @param key key for field
   * @param values values for the field
   * @return this, for call chaining
   */
  public VcfRecord addInfo(String key, String... values) {
    ArrayList<String> val = mInfo.get(key);
    if (val == null) {
      val = new ArrayList<>();
      mInfo.put(key, val);
    }
    if (values != null) {
      Collections.addAll(val, values);
    }
    return this;
  }

  /**
   * Adds or resets an info field
   * @param key key for field
   * @param values values for the field
   * @return this, for call chaining
   */
  public VcfRecord setInfo(String key, String... values) {
    ArrayList<String> val = mInfo.get(key);
    if (val == null) {
      val = new ArrayList<>();
      mInfo.put(key, val);
    }
    val.clear();
    if (values != null) {
      Collections.addAll(val, values);
    }
    return this;
  }

  /**
   * Remove an existing info field.
   * @param key field to remove
   * @return this, for call chaining
   */
  public VcfRecord removeInfo(final String key) {
    mInfo.remove(key);
    return this;
  }

  /**
   * Returns a map that maps each genotype keyword to a list of value
   * strings, one for each sample.
   * @return format keywords mapped to sample values (should be treated as read-only).
   */
  public Map<String, ArrayList<String>> getFormatAndSample() {
    return mFormatAndSample;
  }

  /**
   * @return a set of the genotype keywords
   */
  public Set<String> getFormats() {
    return mFormatAndSample.keySet();
  }

  /**
   * Adds a format key without setting any sample values.
   * @param key format value to be set
   * @return this, for call chaining
   */
  public VcfRecord addFormat(String key) {
    if (!mFormatAndSample.containsKey(key)) {
      mFormatAndSample.put(key, new ArrayList<String>());
    }
    return this;
  }

  /**
   * Sets format key and value for the next sample.
   * @param key format value to be set
   * @param val value for the key
   * @return this, for call chaining
   */
  public VcfRecord addFormatAndSample(String key, String val) {
    if (mFormatAndSample.containsKey(key)) {
      assert mFormatAndSample.get(key).size() < mNumSamples : "Tried to insert more " + key + " format values than number of samples";
      mFormatAndSample.get(key).add(val);
    } else {
      final ArrayList<String> list = new ArrayList<>();
      list.add(val);
      mFormatAndSample.put(key, list);
    }
    return this;
  }

  /**
   * Sets format key and value for the specified sample. If the format key does not already exist in this record,
   * it will be created with missing values for all other samples.
   * @param key format value to be set
   * @param val value for the key
   * @param sampleIndex index of sample, (from <code>VcfHeader</code>)
   * @return this, for call chaining
   */
  public VcfRecord setFormatAndSample(String key, String val, int sampleIndex) {
    assert sampleIndex < mNumSamples : "Invalid sample index: " + sampleIndex;
    final ArrayList<String> vals;
    if (mFormatAndSample.containsKey(key)) {
      vals = mFormatAndSample.get(key);
    } else {
      vals = new ArrayList<>();
      mFormatAndSample.put(key, vals);
      while (vals.size() < mNumSamples) {
        vals.add(MISSING);
      }
    }
    vals.set(sampleIndex, val);
    return this;
  }

  /**
   * Resets all format values to the missing value for the given sample
   * @param sampleIndex the index to reset
   * @return this, for call chaining
   */
  public VcfRecord resetSample(int sampleIndex) {
    assert sampleIndex < mNumSamples : "Invalid sample index: " + sampleIndex;
    for (List<String> vals : mFormatAndSample.values()) {
      if (sampleIndex < vals.size()) {
        vals.set(sampleIndex, MISSING);
      }
    }
    return this;
  }

  /**
   * Fills any remaining values with "missing value" marker
   * @param key attribute to fill
   * @return this, for chain calling
   */
  public VcfRecord padFormatAndSample(String key) {
    if (mFormatAndSample.containsKey(key)) {
      final ArrayList<String> list = mFormatAndSample.get(key);
      while (list.size() < mNumSamples) {
        list.add(MISSING);
      }
    }
    return this;
  }

  /**
   * @return the number of samples
   */
  public int getNumberOfSamples() {
    return mNumSamples;
  }

  /**
   * Sets number of samples
   * @param num the number of samples
   * @return this, for chain calling
   */
  public VcfRecord setNumberOfSamples(int num) {
    mNumSamples = num;
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getSequenceName());
    sb.append(TAB);
    sb.append(String.valueOf(getOneBasedStart()));
    sb.append(TAB);
    sb.append(getId());
    sb.append(TAB);
    sb.append(getRefCall());
    sb.append(TAB);
    sb.append(getAltCalls(mAltCalls));
    sb.append(TAB);
    sb.append(getQuality());
    sb.append(TAB);
    sb.append(getFilter(mFilters));
    sb.append(TAB);
    sb.append(getPrintableInfo(getInfo()));
    if (countNumberOfSamples(mFormatAndSample) != mNumSamples) {
      throw new IllegalStateException("Number of samples (" + mNumSamples + ") disagrees with contents of VCF record (" + countNumberOfSamples(mFormatAndSample) + ")");
    }
    if (mNumSamples > 0) {
      sb.append(TAB);
      sb.append(getFormat(getFormatAndSample()));
      for (int i = 0; i < mNumSamples; i++) {
        sb.append(TAB);
        sb.append(getSample(i, getFormatAndSample()));
      }
    }
    return sb.toString();
  }

  private static String getAltCalls(List<String> altCalls) {
    if (altCalls.size() == 0) {
      return MISSING;
    }
    return StringUtils.implode(altCalls, ALT_CALL_INFO_SEPARATOR);
  }

  private static int countNumberOfSamples(Map<String, ArrayList<String>> formatAndSample) {
    int currentCount = 0;
    boolean first = true;
    for (final Entry<String, ArrayList<String>> formatField : formatAndSample.entrySet()) {
      final int numSamples = formatField.getValue().size();
      if (first) {
        currentCount = numSamples;
        first = false;
      }
      if (currentCount != numSamples) {
        throw new IllegalStateException("not enough data for all samples, first size = " + currentCount + ", current key = " + formatField.getKey() + " count = " + numSamples);
      }
    }
    return currentCount;
  }

  private static String getPrintableInfo(Map<String, ArrayList<String>> info) {
    final StringBuilder sb = new StringBuilder();
    for (final Entry<String, ArrayList<String>> e : info.entrySet()) {
      sb.append(e.getKey());
      final Collection<String> values = e.getValue();
      if (values != null && values.size() > 0) {
        sb.append("=")
        .append(StringUtils.implode(values, ALT_CALL_INFO_SEPARATOR));
      }
      sb.append(FILTER_AND_INFO_SEPARATOR);
    }
    if (sb.length() == 0) {
      return MISSING;
    }
    return sb.substring(0, sb.length() - 1);
  }

  private static String getSample(int i, Map<String, ArrayList<String>> formatAndSample) {
    final StringBuilder sb = new StringBuilder();
    final StringBuilder msb = new StringBuilder(); // Allow omitting trailing missing sub-fields.
    for (final Entry<String, ArrayList<String>> formatField : formatAndSample.entrySet()) {
      final String val = formatField.getValue().get(i);
      if (MISSING.equals(val)) {
        msb.append(val).append(FORMAT_AND_SAMPLE_SEPARATOR);
      } else {
        sb.append(msb).append(val).append(FORMAT_AND_SAMPLE_SEPARATOR);
        msb.setLength(0);
      }
    }
    if (sb.length() == 0) {
      return MISSING;
    }
    return sb.substring(0, sb.length() - 1);
  }

  private static String getFormat(Map<String, ArrayList<String>> formatAndSample) {
    if (formatAndSample.size() == 0) {
      return MISSING;
    }
    return StringUtils.implode(formatAndSample.keySet(), FORMAT_AND_SAMPLE_SEPARATOR);
  }

  private static String getFilter(List<String> filter) {
    if (filter.size() == 0) {
      return MISSING;
    }
    return StringUtils.implode(filter, FILTER_AND_INFO_SEPARATOR);
  }

  /**
   * Returns the value of the specified sample field as a String.
   * @param sampleNumber sample number
   * @param formatField field of sample
   * @return value as a String or null if not specified
   */
  public String getSampleString(int sampleNumber, String formatField) {
    final ArrayList<String> samples = mFormatAndSample.get(formatField);
    if (samples != null) {
      return samples.get(sampleNumber);
    }
    return null;
  }

  /**
   * Returns the value of the specified sample field as a Double.
   * @param sampleNumber sample number
   * @param formatField field of sample
   * @return value as a double or null if not specified
   * @throws NumberFormatException if value is not a double
   */
  public Double getSampleDouble(int sampleNumber, String formatField) {
    final String valueStr = getSampleString(sampleNumber, formatField);
    if (valueStr != null && !VcfRecord.MISSING.equals(valueStr)) {
      return Double.parseDouble(valueStr);
    }
    return null;
  }

  /**
   * Returns the value of the specified sample field as an Integer.
   * @param sampleNumber sample number
   * @param formatField field of sample
   * @return value as an integer or null if not specified
   * @throws NumberFormatException if value is not an integer
   */
  public Integer getSampleInteger(int sampleNumber, String formatField) {
    final String valueStr = getSampleString(sampleNumber, formatField);
    if (valueStr != null && !VcfRecord.MISSING.equals(valueStr)) {
      return Integer.parseInt(valueStr);
    }
    return null;
  }

  /**
   * @return true if the record has failed filters
   */
  public boolean isFiltered() {
    for (final String f : getFilters()) {
      if (!(VcfUtils.FILTER_PASS.equals(f) || VcfRecord.MISSING.equals(f))) {
        return true;
      }
    }
    return false;
  }
}
