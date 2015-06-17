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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Will probably one day become <code>VcfHeaderUtils</code>. At the moment just has a method for
 * merging the headers of 2 <code>VCF</code> files.
 */
public final class VcfHeaderMerge {

  private VcfHeaderMerge() {

  }

  /**
   * Merge 2 headers. If the 2 headers have different versions we replace with the latest version support by this code.
   * Any identical meta lines are kept only once. Filter, Info, and Format meta lines must have compatible entries for the same
   * ID or it will be an error, the merged header will contain the most compatible version. Any unique meta line will be copied to the
   * merged header. Order of items within each group (generic, info, filter, format) will be the items from the first file, then the second
   *
   * The header line itself will contain the first headers samples, followed by the second headers unique samples
   * @param first first header
   * @param second second header
   * @param forceMerge set of header IDs we should force merge, supply empty set to merge none, or null to force merge all.
   * @return merged header
   */
  public static VcfHeader mergeHeaders(VcfHeader first, VcfHeader second, Set<String> forceMerge) {
    final VcfHeader ret = new VcfHeader();
    if (!first.getVersionLine().equals(second.getVersionLine())) {
      //just set to current;
      ret.setVersionValue(VcfHeader.VERSION_VALUE);
    } else {
      ret.setVersionValue(first.getVersionValue());
    }

    final ArrayList<String> errors = new ArrayList<>();
    appendMeta(ret, combineFieldSection(first.getContigLines(), second.getContigLines(), forceMerge, errors));
    appendMeta(ret, combineFieldSection(first.getFilterLines(), second.getFilterLines(), forceMerge, errors));
    appendMeta(ret, combineFieldSection(first.getFormatLines(), second.getFormatLines(), forceMerge, errors));
    appendMeta(ret, combineFieldSection(first.getInfoLines(), second.getInfoLines(), forceMerge, errors));
    appendMeta(ret, combineFieldSection(first.getAltLines(), second.getAltLines(), forceMerge, errors));
    if (errors.size() > 0) {
      final StringBuilder mesg = new StringBuilder();
      for (final String s : errors) {
        mesg.append(s).append(StringUtils.LS);
      }
      throw new NoTalkbackSlimException(mesg.toString());
    }

    for (SampleField p : first.getSampleLines()) {
      ret.addMetaInformationLine(p.toString());
    }
    for (SampleField p : second.getSampleLines()) {
      if (!first.getSampleLines().contains(p)) {
        ret.addMetaInformationLine(p.toString());
      }
    }

    for (PedigreeField p : first.getPedigreeLines()) {
      ret.addMetaInformationLine(p.toString());
    }
    for (PedigreeField p : second.getPedigreeLines()) {
      if (!first.getPedigreeLines().contains(p)) {
        ret.addMetaInformationLine(p.toString());
      }
    }

    // Add in generic meta lines. Specific meta lines should be handled separately
    for (String s : first.getGenericMetaInformationLines()) {
      ret.addMetaInformationLine(s);
    }
    for (String s : second.getGenericMetaInformationLines()) {
      if (!first.getGenericMetaInformationLines().contains(s)) {
        ret.addMetaInformationLine(s);
      }
    }

    final List<String> s = first.getSampleNames();
    for (String sample : s) {
      ret.addSampleName(sample);
    }
    final List<String> s2 = second.getSampleNames();
    for (String sample : s2) {
      if (!s.contains(sample)) {
        ret.addSampleName(sample);
      }
    }
    return ret;
  }

  private static <T extends IdField<T>> List<T> combineFieldSection(List<T> firstList, List<T> secondList, Set<String> forceMerge, List<String> errors) {
    final ArrayList<T> ret = new ArrayList<>();
    final Map<String, T> secondMap = getFieldMap(secondList);
    for (T t : firstList) {
      final T secondVal = secondMap.get(t.getId());
      if (secondVal != null) {
        final T superSet = t.superSet(secondVal);
        if (superSet == null) {
          if (forceMerge != null && !forceMerge.contains(t.getId())) {
            errors.add("Header line: " + StringUtils.LS + t.toString() + StringUtils.LS + "is incompatible with" + StringUtils.LS + secondVal.toString());
          } else {
            ret.add(t);
          }
          continue;
        }
        ret.add(superSet);
      } else {
        ret.add(t);
      }
    }
    //first depleted
    final Map<String, T> firstMap = getFieldMap(firstList);
    for (T t : secondList) {
      final T firstVal = firstMap.get(t.getId());
      if (firstVal == null) {
        ret.add(t);
      } //else already added
    }
    return ret;
  }

  private static <T extends IdField<T>> void appendMeta(VcfHeader header, List<T> fields) {
    for (T t : fields) {
      header.addMetaInformationLine(t.toString());
    }
  }

  private static <T extends IdField<T>> HashMap<String, T> getFieldMap(List<T> fields) {
    final HashMap<String, T> ret = new HashMap<>();
    for (T t : fields) {
      ret.put(t.getId(), t);
    }
    return ret;
  }

}
