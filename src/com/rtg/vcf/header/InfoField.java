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

import com.rtg.util.StringUtils;
import com.rtg.util.Utils;

/**
 * Class to encapsulate information of a info meta information line in <code>VCF</code>
 */
public class InfoField implements TypedField<InfoField> {
  private static final Pattern INFO_LINE_PATTERN = Pattern.compile("^##INFO=<(.+)>$");

  private final String mId;
  private final MetaType mType;
  private final VcfNumber mNumber;
  private final String mDescription;
  private final String mSource;
  private final String mVersion;

  /**
   * @param line info line from <code>VCF</code> file
   */
  public InfoField(String line) {
    final HashMap<String, String> temp = VcfHeader.parseMetaLine(line, INFO_LINE_PATTERN);
    VcfHeader.checkRequiredMetaKeys(temp, "ID", "Type", "Number", "Description");
    mId = temp.get("ID");
    mType = MetaType.parseValue(temp.get("Type"));
    mNumber = new VcfNumber(temp.get("Number"));
    mDescription = temp.get("Description");
    mSource = temp.getOrDefault("Source", null);
    mVersion = temp.getOrDefault("Version", null);
  }

  /**
   * Makes a VCF InfoField
   * @param id the info field identifier
   * @param type the type of value
   * @param number the specifier for the number of occurrences
   * @param description the field description
   */
  public InfoField(String id, MetaType type, VcfNumber number, String description) {
    mId = id;
    mType = type;
    mNumber = number;
    mDescription = description;
    mSource = null;
    mVersion = null;
  }

  @Override
  public boolean equals(Object obj) {
    return mostlyEquals(obj)
      && mType == ((InfoField) obj).mType
      && Utils.equals(mSource, ((InfoField) obj).mSource)
      && Utils.equals(mVersion, ((InfoField) obj).mVersion);
  }

  // True if no field conflicts preventing merge
  private boolean mostlyEquals(Object obj) {
    if (!(obj instanceof InfoField)) {
      return false;
    }
    final InfoField other = (InfoField) obj;
    return mId.equals(other.mId) && mNumber.equals(other.mNumber)
      && mDescription.equals(other.mDescription);
  }

  @Override
  public InfoField superSet(InfoField other) {
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
  public int hashCode() {
    return Utils.pairHashContinuous(mId.hashCode(), mNumber.hashCode(), mType.ordinal(), mDescription.hashCode(), mSource == null ? 0 : mSource.hashCode(), mVersion == null ? 0 : mVersion.hashCode());
  }

  @Override
  public String fieldName() {
    return "INFO";
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
    return VcfHeader.INFO_STRING + "=<ID=" + mId + ",Number=" + mNumber + ",Type=" + mType
      + ",Description=" + StringUtils.dumbQuote(mDescription)
      + (mSource == null ? "" : ",Source=" + StringUtils.dumbQuote(mSource))
      + (mVersion == null ? "" : ",Version=" + StringUtils.dumbQuote(mVersion))
      + ">";
  }

}
