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
package com.rtg.vcf.header;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.vcf.VcfFormatException;

/**
 *
 */
public class ContigField implements IdField<ContigField> {

  private static final Pattern CONTIG_LINE_PATTERN = Pattern.compile("^##contig=<(.+)>$");

  private final String mId;
  private final Integer mLength;
  private final Map<String, String> mValues;

  /**
   * @param line filter line from <code>VCF</code> file
   */
  public ContigField(String line) {
    final LinkedHashMap<String, String> temp = VcfHeader.parseMetaLine(line, CONTIG_LINE_PATTERN);
    VcfHeader.checkRequiredMetaKeys(temp, "ID");
    mId = temp.get("ID");
    temp.remove("ID");
    if (temp.containsKey("length")) {
      try {
        mLength = Integer.valueOf(temp.get("length"));
      } catch (NumberFormatException e) {
        throw new VcfFormatException("Non-integer contig length \"" + temp.get("length") + "\"");
      }
      temp.remove("length");
    } else {
      mLength = null;
    }
    mValues = temp;
  }

  ContigField(String id, Integer length) {
    mId = id;
    mLength = length;
    mValues = new LinkedHashMap<>();
  }

  /**
   * Put an additional pair into the header field
   * @param key the key
   * @param value the value
   */
  public void put(String key, String value) {
    mValues.put(key, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ContigField)) {
      return false;
    }
    final ContigField other = (ContigField) obj;
    return mostlyEquals(other)
      && Utils.equals(mLength, other.mLength)
      && mValues.equals(other.mValues);
  }

  // True if no conflicting fields preventing merge
  private boolean mostlyEquals(ContigField other) {
    if (!mId.equals(other.mId)) {
      return false;
    }
    // A null length in either side doesn't break the mostlyEquals
    if (!(mLength == null || other.mLength == null || mLength.equals(other.mLength))) {
      return false;
    }
    for (Map.Entry<String, String> entry : mValues.entrySet()) {
      final String val = other.mValues.get(entry.getKey());
      if (val != null && !val.equals(entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mId.hashCode(), mValues.hashCode(), mLength != null ? mLength : 0);
  }

  @Override
  public ContigField superSet(ContigField other) {
    if (!mostlyEquals(other)) {
      return null;
    }
    if (equals(other)) {
      return this;
    }
    final ContigField result = new ContigField(mId, mLength != null ? mLength : other.mLength);
    result.mValues.putAll(mValues);
    result.mValues.putAll(other.mValues);
    return result;
  }

  @Override
  public String fieldName() {
    return "contig";
  }

  /**
   * @return the ID field
   */
  @Override
  public String getId() {
    return mId;
  }

  /**
   * @return the description field
   */
  public Integer getLength() {
    return mLength;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(VcfHeader.CONTIG_STRING).append("=<ID=").append(mId);
    if (mLength != null) {
      sb.append(",length=").append(mLength);
    }
    for (final Map.Entry<String, String> entry : mValues.entrySet()) {
      sb.append(",").append(entry.getKey()).append("=");
      sb.append(StringUtils.smartQuote(entry.getValue()));
    }
    sb.append(">");
    return sb.toString();
  }
}
