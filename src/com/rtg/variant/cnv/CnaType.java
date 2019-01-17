/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
package com.rtg.variant.cnv;

import static com.rtg.vcf.VcfUtils.INFO_SVTYPE;

import java.util.ArrayList;

import com.rtg.vcf.VcfRecord;

/** Simple classification of copy number alteration types */
public enum CnaType {

  /** Deletion / loss */
  DEL,
  /** Duplication / gain */
  DUP,
  /** Not a copy number alteration */
  NONE;

  /**
   * Determines the type of copy number alteration of a VCF record
   * @param rec the record
   * @return the determined copy number alteration type
   */
  public static CnaType valueOf(final VcfRecord rec) {
    final ArrayList<String> svTypes = rec.getInfo().get(INFO_SVTYPE);
    if (svTypes == null || svTypes.size() != 1) {
      return CnaType.NONE;
    }
    switch (svTypes.get(0)) {
      case "DUP":
        return CnaType.DUP;
      case "DEL":
        return CnaType.DEL;
      default:
        return CnaType.NONE;
    }
  }
}
