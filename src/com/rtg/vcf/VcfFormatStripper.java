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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Removes unwanted Format field entries from a VCF record
 */
public class VcfFormatStripper implements VcfAnnotator {

  private final boolean mKeepMode;
  private final Set<String> mFormats;

  private boolean mKeepRecord = true;

  /**
   * Keep or remove a selected set of format fields from header and records
   * @param formatList the list of format field ids
   * @param keep true to keep values in the list, false to remove them
   */
  VcfFormatStripper(Set<String> formatList, boolean keep) {
    mKeepMode = keep;
    mFormats = formatList;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mFormats == null || mFormats.size() == 0) {
      return;
    }
    final Iterator<FormatField> it = header.getFormatLines().iterator();
    while (it.hasNext()) {
      final FormatField format = it.next();
      if (mKeepMode ^ mFormats.contains(format.getId())) {
        it.remove();
      }
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    if (mFormats == null || mFormats.size() == 0) {
      return;
    }
    final Iterator<Map.Entry<String, ArrayList<String>>> it = rec.getFormatAndSample().entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<String, ArrayList<String>> e = it.next();
      if (mKeepMode ^ mFormats.contains(e.getKey())) {
        it.remove();
      }
    }
    if (rec.getFormatAndSample().size() == 0) {
      mKeepRecord = false;
    }
  }

  boolean keepRecord() {
    return mKeepRecord;
  }
}
