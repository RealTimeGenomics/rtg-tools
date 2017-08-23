/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.rtg.util.StringUtils;

/**
 * Represents a VCF symbolic ALT allele.
 */
public class SymbolicAlt {

  private final String mType;
  private final List<String> mSubTypes;
  private final String mRefSubs;

  /**
   * Parse a symbolic ALT string. The caller should have already determined
   * that the ALT represents a symbolic allele.
   * @param alt the VCF ALT string
   */
  public SymbolicAlt(String alt) {
    int tmpPos = alt.indexOf('<');
    if (tmpPos == -1) {
      throw new IllegalArgumentException("Invalid symbolic allele specification: " + alt);
    }
    mRefSubs = alt.substring(0, tmpPos);
    final String rest = alt.substring(tmpPos + 1);
    tmpPos = rest.indexOf('>');
    if (tmpPos == -1) {
      throw new IllegalArgumentException("Invalid symbolic allele specification: " + alt);
    }
    final String[] parts = StringUtils.split(rest.substring(0, tmpPos), ':');
    mType = parts[0];
    mSubTypes = Arrays.asList(parts).subList(1, parts.length);
  }

  /**
   * Construct a symbolic allele directly.
   * @param refSubs the bases of the reference substitution
   * @param type allele ID or structural variant primary type
   * @param subTypes structural variant sub types
   */
  public SymbolicAlt(String refSubs, String type, String... subTypes) {
    mRefSubs = refSubs;
    mType = type;
    mSubTypes = Arrays.asList(subTypes);
  }

  /**
   * @return the bases that substitute for the reference bases
   */
  public String getRefSubstitution() {
    return mRefSubs;
  }

  public String getType() {
    return mType;
  }

  public Collection<String> getSubTypes() {
    return mSubTypes;
  }

  @Override
  public String toString() {
    return mRefSubs + '<' + (mSubTypes.size() > 0 ? mType + ":" + StringUtils.join(';', mSubTypes) : mType) + ">";
  }
}
