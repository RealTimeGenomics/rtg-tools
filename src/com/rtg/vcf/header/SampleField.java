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
import java.util.Locale;
import java.util.regex.Pattern;

import com.rtg.reference.Sex;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;

/**
 * Class to encapsulate a sample line in <code>VCF</code>
 */
public class SampleField implements IdField<SampleField> {

  private static final Pattern SAMPLE_LINE_PATTERN = Pattern.compile("^##SAMPLE=<(.+)>$");

  private final String mId;
  private final String mGenomes;
  private final String mMixture;
  private final String mDescription;
  private final Sex mSex;

  /**
   * @param line sample line
   */
  public SampleField(String line) {
    final HashMap<String, String> temp = VcfHeader.parseMetaLine(line, SAMPLE_LINE_PATTERN);
    VcfHeader.checkRequiredMetaKeys(temp, "ID");
    mId = temp.get("ID");
    mGenomes = temp.get("Genomes");
    mMixture = temp.get("Mixture");
    mDescription = temp.get("Description");
    final String sexStr = temp.get("Sex");
    if (sexStr != null) {
      mSex = Sex.valueOf(sexStr.toUpperCase(Locale.getDefault()));
    } else {
      mSex = null;
    }
  }

  /**
   * @return the ID field
   */
  @Override
  public String getId() {
    return mId;
  }

  /**
   * @return genomes field
   */
  public String getGenomes() {
    return mGenomes;
  }

  /**
   * @return mixture field
   */
  public String getMixture() {
    return mMixture;
  }

  /**
   * @return description field
   */
  public String getDescription() {
    return mDescription;
  }

  public Sex getSex() {
    return mSex;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SampleField)) {
      return false;
    }
    final SampleField other = (SampleField) obj;
    return mId.equals(other.mId) && Utils.equals(mGenomes, other.mGenomes)
      && Utils.equals(mMixture, other.mMixture) && Utils.equals(mDescription, other.mDescription);
  }

  @Override
  public int hashCode() {
    final int gHash = mGenomes != null ? mGenomes.hashCode() : 0;
    final int mxHash = mMixture != null ? mMixture.hashCode() : 0;
    final int dHash = mDescription != null ? mDescription.hashCode() : 0;
    final int sxHash = mSex != null ? mSex.hashCode() : 0;
    return Utils.pairHashContinuous(mId.hashCode(), gHash, mxHash, dHash, sxHash);
  }

  @Override
  public SampleField superSet(SampleField other) {
    return equals(other) ? this : null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(VcfHeader.SAMPLE_STRING).append("=<ID=").append(mId);
    if (mGenomes != null) {
      sb.append(",Genomes=").append(mGenomes);
    }
    if (mMixture != null) {
      sb.append(",Mixture=").append(mMixture);
    }
    if (mDescription != null) {
      sb.append(",Description=").append(StringUtils.dumbQuote(mDescription));
    }
    if (mSex != null && (mSex == Sex.MALE || mSex == Sex.FEMALE)) {
      sb.append(",Sex=").append(mSex);
    }
    sb.append(">");
    return sb.toString();
  }
}
