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

import com.rtg.vcf.VcfFormatException;

/**
 * Encapsulate type in <code>VCF</code> meta lines
 */
public enum MetaType {
  /** if value is integer */
  INTEGER("Integer"),
  /** if value is floating point number */
  FLOAT("Float"),
  /** if value is not present (i.e. existence of ID is sufficient) */
  FLAG("Flag"),
  /** if value is a single character */
  CHARACTER("Character"),
  /** if value is a string */
  STRING("String");

  private static final HashMap<String, MetaType> PARSE_MAP = new HashMap<>(5);
  static {
    for (final MetaType mt : MetaType.values()) {
      PARSE_MAP.put(mt.toString(), mt);
    }
  }
  private final String mToString;

  MetaType(String toString) {
    mToString = toString;
  }

  /**
   * @return value as appears in file
   */
  @Override
  public String toString() {
    return mToString;
  }

  /**
   * @param val value as appears in file
   * @return corresponding instance
   */
  public static MetaType parseValue(String val) {
    if (!PARSE_MAP.containsKey(val)) {
      throw new VcfFormatException("Invalid VCF header field type: '" + val + "'. Must be one of " + PARSE_MAP.keySet());
    }
    return PARSE_MAP.get(val);
  }

  /**
   * find out if type is super set of another. Currently each type is a super set of itself, and {@link MetaType#FLOAT} is also a super set of {@link MetaType#INTEGER}
   * @param other type to compare to
   * @return true if this is a super set
   */
  public boolean isSuperSet(MetaType other) {
    if (this == MetaType.FLOAT) {
      return other == MetaType.FLOAT || other == MetaType.INTEGER;
    }
    return this == other;
  }
}
