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

import java.util.HashMap;
import java.util.regex.Pattern;

import com.rtg.util.Utils;

/**
 * Class to encapsulate information of a format meta information line in <code>VCF</code>
 */
public class FormatField implements TypedField<FormatField> {

  private static final Pattern FORMAT_LINE_PATTERN = Pattern.compile("^##FORMAT=<(.+)>$");

  private final String mId;
  private final MetaType mType;
  private final VcfNumber mNumber;
  private final String mDescription;

  /**
   * @param line format line
   */
  public FormatField(String line) {
    final HashMap<String, String> temp = VcfHeader.parseMetaLine(line, FORMAT_LINE_PATTERN);
    VcfHeader.checkRequiredMetaKeys(temp, "ID", "Type", "Number", "Description");
    mId = temp.get("ID");
    mType = MetaType.parseValue(temp.get("Type"));
    mNumber = new VcfNumber(temp.get("Number"));
    mDescription = temp.get("Description");
  }

  /**
   * Makes a VCF FormatField
   * @param id the format field identifier
   * @param type the type of value
   * @param number the specifier for the number of occurrences
   * @param description the field description
   */
  public FormatField(String id, MetaType type, VcfNumber number, String description) {
    mId = id;
    mType = type;
    mNumber = number;
    mDescription = description;
  }

  @Override
  public boolean equals(Object obj) {
    return mostlyEquals(obj) && mType == ((FormatField) obj).mType;
  }

  // True if no field conflicts preventing merge
  private boolean mostlyEquals(Object obj) {
    if (!(obj instanceof FormatField)) {
      return false;
    }
    final FormatField other = (FormatField) obj;
    return mId.equals(other.mId) && mNumber.equals(other.mNumber) && mDescription.equals(other.mDescription);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(Utils.pairHash(Utils.pairHash(mId.hashCode(), mNumber.hashCode()), mType.ordinal()), mDescription.hashCode());
  }

  @Override
  public FormatField superSet(FormatField other) {
    if (!mostlyEquals(other)) {
      return null;
    }
    if (mType.isSuperSet(other.mType)) {
      return this;
    } else if (other.mType.isSuperSet(mType)) {
      return other;
    }
    return null;
  }

  @Override
  public String getId() {
    return mId;
  }

  @Override
  public VcfNumber getNumber() {
    return mNumber;
  }

  @Override
  public MetaType getType() {
    return mType;
  }

  @Override
  public String getDescription() {
    return mDescription;
  }

  @Override
  public String toString() {
    return VcfHeader.FORMAT_STRING + "=<ID=" + mId + ",Number=" + mNumber.toString() + ",Type=" + mType.toString() + ",Description=\"" + mDescription + "\">";
  }

}
