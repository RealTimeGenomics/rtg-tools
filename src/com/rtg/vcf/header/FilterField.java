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
 * Class to encapsulate information of a filter meta information line in <code>VCF</code>
 */
public class FilterField implements IdField<FilterField> {

  private static final Pattern FILTER_LINE_PATTERN = Pattern.compile("^##FILTER=<(.+)>$");

  private final String mId;
  private final String mDescription;

  /**
   * @param line filter line from <code>VCF</code> file
   */
  public FilterField(String line) {
    final HashMap<String, String> temp = VcfHeader.parseMetaLine(line, FILTER_LINE_PATTERN);
    VcfHeader.checkRequiredMetaKeys(temp, "ID", "Description");
    mId = temp.get("ID");
    mDescription = temp.get("Description");
  }

  /**
   * Makes a VCF FilterField
   * @param id the filter field identifier
   * @param description the field description
   */
  public FilterField(String id, String description) {
    mId = id;
    mDescription = description;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FilterField)) {
      return false;
    }
    final FilterField other = (FilterField) obj;
    return mId.equals(other.mId) && mDescription.equals(other.mDescription);
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(mId.hashCode(), mDescription.hashCode());
  }

  @Override
  public FilterField superSet(FilterField other) {
    return equals(other) ? this : null;
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
  public String getDescription() {
    return mDescription;
  }

  @Override
  public String toString() {
    return VcfHeader.FILTER_STRING + "=<ID=" + mId + ",Description=\"" + mDescription + "\">";
  }

}
