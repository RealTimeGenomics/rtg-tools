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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.rtg.util.StringUtils;
import com.rtg.util.intervals.SequenceNameLocus;
import com.rtg.util.intervals.SequenceNameLocusSimple;

/**
 * Class to hold a single VCF record
 */
public class VcfRecord implements SequenceNameLocus {

  /** VCF missing value string **/
  public static final String MISSING = "" + VcfUtils.MISSING_VALUE;

  /** The character used to delimit subfields within FORMAT and SAMPLE fields */
  public static final String FORMAT_AND_SAMPLE_SEPARATOR = ":";
  /** The character used to delimit subfields within FILTER and INFO fields */
  public static final String ID_FILTER_AND_INFO_SEPARATOR = ";";
  /** The character used to delimit ALT alleles and multi-valued subfield values */
  public static final String ALT_CALL_INFO_SEPARATOR = ",";

  private String mSequence;
  private int mStart = -1;
  private String mId; // These are stored in VCF representation (users may need to perform splitting)
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
   * Construct a new standard (non gVCF) VcfRecord
   * @param sequence the sequence name
   * @param start the start position
   * @param ref the ref allele
   */
  public VcfRecord(String sequence, int start, String ref) {
    mSequence = sequence;
    mStart = start;
    mRefCall = ref;
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
   * Gets the one-based start position
   * @return the one-based start position
   */
  public int getOneBasedStart() {
    return getStart() + 1;
  }

  /**
   * @return id field, caller responsibility to split on ";" for multiple ids
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
      for (int i = 1; i < id.length; ++i) {
        ids.append(ID_FILTER_AND_INFO_SEPARATOR).append(id[i]);
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
    assert ref != null;
    mRefCall = ref;
    return this;
  }

  /**
   *
   * @param altCall the next alternate call
   * @return this, for call chaining
   */
  public VcfRecord addAltCall(String altCall) {
    if (MISSING.equals(altCall)) {
      throw new VcfFormatException("Attempt to add missing value '.' as explicit ALT allele");
    } else {
      mAltCalls.add(altCall);
    }
    return this;
  }

  /**
   * @return alternate calls (this should be treated as read-only).
   */
  public List<String> getAltCalls() {
    return mAltCalls;
  }

  /**
   * Get the allele with the specified index
   * @param allele the allele to retrieve
   * @return the allele, or null if allele index is -1 (missing)
   */
  public String getAllele(int allele) {
    if (allele > mAltCalls.size()) {
      throw new VcfFormatException("Invalid allele number " + allele);
    }
    return allele == -1 ? null : allele == 0 ? getRefCall() : mAltCalls.get(allele - 1);
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
    // VCF spec says the field is either PASS or semicolon separated list of failures.
    // So we may need to remove any existing PASS, or other filters, depending on what comes in.
    if (VcfUtils.FILTER_PASS.equals(filter)) {
      mFilters.clear();
    } else {
      mFilters.remove(VcfUtils.FILTER_PASS);
    }
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
   * @return a set of the format ids used in this record
   */
  public Set<String> getFormats() {
    return mFormatAndSample.keySet();
  }

  /**
   * Returns true if the record contains the specified format field
   * @param key format value
   * @return true if the format field is contained in this record
   */
  public boolean hasFormat(String key) {
    return mFormatAndSample.containsKey(key);
  }

  /**
   * Remove the specified format field from this record
   * @param key format value
   */
  public void removeFormat(String key) {
    mFormatAndSample.remove(key);
  }

  /**
   * Gets the sample format values for a specified format
   * @param key format value to be retrieved
   * @return the sample values for this format field
   */
  public ArrayList<String> getFormat(String key) {
    return mFormatAndSample.get(key);
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
   * Removes all samples from the record and clears format information
   * @return this, for call chaining
   */
  public VcfRecord removeSamples() {
    mNumSamples = 0;
    mFormatAndSample.clear();
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
      throw new IllegalStateException("Number of samples (" + mNumSamples + ") disagrees with contents of VCF record (" + countNumberOfSamples(mFormatAndSample) + ") at: " + getSequenceName() + ":" + getOneBasedStart());
    }
    if (mNumSamples > 0) {
      sb.append(TAB);
      sb.append(getFormat(getFormatAndSample()));
      for (int i = 0; i < mNumSamples; ++i) {
        sb.append(TAB);
        sb.append(getSample(i, getFormatAndSample()));
      }
    }
    return sb.toString();
  }

  private static String getAltCalls(List<String> altCalls) {
    if (altCalls.isEmpty()) {
      return MISSING;
    }
    return StringUtils.join(ALT_CALL_INFO_SEPARATOR, altCalls);
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
        .append(StringUtils.join(ALT_CALL_INFO_SEPARATOR, values));
      }
      sb.append(ID_FILTER_AND_INFO_SEPARATOR);
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
    if (formatAndSample.isEmpty()) {
      return MISSING;
    }
    return StringUtils.join(FORMAT_AND_SAMPLE_SEPARATOR, formatAndSample.keySet());
  }

  private static String getFilter(List<String> filter) {
    if (filter.isEmpty()) {
      return MISSING;
    }
    return StringUtils.join(ID_FILTER_AND_INFO_SEPARATOR, filter);
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
      return Double.valueOf(valueStr);
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
      return Integer.valueOf(valueStr);
    }
    return null;
  }

}
