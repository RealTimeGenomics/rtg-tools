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
import java.util.Map;
import java.util.regex.Pattern;

import com.rtg.util.Utils;

/**
 * Class to encapsulate a pedigree line in <code>VCF</code>
 */
public class PedigreeField {

  //Family pedigree relationship fields
  private static final String PEDIGREE_FATHER = "Father";
  private static final String PEDIGREE_MOTHER = "Mother";
  private static final String PEDIGREE_CHILD = "Child";

  //Clonal pedigree relationship fields
  private static final String PEDIGREE_DERIVED = "Derived";
  private static final String PEDIGREE_ORIGINAL = "Original";

  private static final Pattern PEDIGREE_LINE_PATTERN = Pattern.compile("^##PEDIGREE=<(.+)>$");
  private final HashMap<String, String> mSamples;

  /**
   * @param line pedigree line
   */
  public PedigreeField(String line) {
    mSamples = VcfHeader.parseMetaLine(line, PEDIGREE_LINE_PATTERN);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PedigreeField)) {
      return false;
    }
    final PedigreeField other = (PedigreeField) obj;
    if (mSamples.size() != other.mSamples.size()) {
        return false;
    }
    for (final String key : mSamples.keySet()) {
      if (!mSamples.get(key).equals(other.mSamples.get(key))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (final Map.Entry<String, String> e : mSamples.entrySet()) {
      h = Utils.pairHash(h, Utils.pairHash(e.getKey().hashCode(), e.getValue().hashCode()));
    }
    return h;
  }

  /**
   * @return the child sample
   */
  public String getChild() {
    return mSamples.get(PEDIGREE_CHILD);
  }

  /**
   * @return the mother sample
   */
  public String getMother() {
    return mSamples.get(PEDIGREE_MOTHER);
  }

  /**
   * @return the father sample
   */
  public String getFather() {
    return mSamples.get(PEDIGREE_FATHER);
  }

  /**
   * @return the derived sample
   */
  public String getDerived() {
    return mSamples.get(PEDIGREE_DERIVED);
  }

  /**
   * @return the original original sample
   */
  public String getOriginal() {
    return mSamples.get(PEDIGREE_ORIGINAL);
  }

  /**
   * Relabel any occurrences of a sample name in this pedigree line.
   * @param originalName original sample name
   * @param newName new sample name
   */
  public void relabelSample(final String originalName, final String newName) {
    for (final Map.Entry<String, String> e : mSamples.entrySet()) {
      if (e.getValue().equals(originalName)) {
        e.setValue(newName);
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(VcfHeader.PEDIGREE_STRING).append("=<");
    boolean first = true;
    for (final Map.Entry<String, String> e : mSamples.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append(e.getKey()).append("=").append(e.getValue());
    }
    sb.append(">");
    return sb.toString();
  }
}
