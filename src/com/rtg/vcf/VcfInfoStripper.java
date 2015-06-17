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

import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.VcfHeader;

/**
 * Removes unwanted Info field entries from a VCF record
 */
public class VcfInfoStripper implements VcfAnnotator {

  private final boolean mRemoveAll;
  private final boolean mKeepMode;
  private final Set<String> mInfos;

  /**
   * Remove all info fields from header and records
   * @param removeAll false if you don't actually want to do it for some reason.
   */
  VcfInfoStripper(boolean removeAll) {
    mRemoveAll = removeAll;
    mKeepMode = false;
    mInfos = null;
  }

  /**
   * Keep or remove a selected set of info fields from header and records
   * @param infoList the list of info field ids
   * @param keep true to keep values in the list, false to remove them
   */
  VcfInfoStripper(Set<String> infoList, boolean keep) {
    mRemoveAll = false;
    mKeepMode = keep;
    mInfos = infoList;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mRemoveAll) {
      header.getInfoLines().clear();
      return;
    } else if (mInfos == null || mInfos.size() == 0) {
      return;
    }
    final Iterator<InfoField> it = header.getInfoLines().iterator();
    while (it.hasNext()) {
      final InfoField info = it.next();
      if (mKeepMode ^ mInfos.contains(info.getId())) {
        it.remove();
      }
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    if (mRemoveAll) {
      rec.getInfo().clear();
      return;
    } else if (mInfos == null || mInfos.size() == 0) {
      return;
    }
    final Iterator<Map.Entry<String, ArrayList<String>>> it = rec.getInfo().entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<String, ArrayList<String>> e = it.next();
      if (mKeepMode ^ mInfos.contains(e.getKey())) {
        it.remove();
      }
    }
  }
}
