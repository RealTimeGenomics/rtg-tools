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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.RangeList.RangeData;
import com.rtg.vcf.header.VcfHeader;

/**
 * Adds VCF ID column based on VCF IDs from another VCF file.
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public class VcfIdAnnotator implements VcfAnnotator {

  private final Map<String, RangeList<String>> mAnnotations;

  /**
   * Constructor
   * @param vcfFiles VCF files containing variant IDs to be added to VCF id column.
   * @throws IOException if the BED file could not be loaded.
   */
  public VcfIdAnnotator(Collection<File> vcfFiles) throws IOException {
    final Map<String, List<RangeData<String>>> annotations = new HashMap<>();

    for (final File vcfFile : vcfFiles) {
      try (final VcfReader reader = VcfReader.openVcfReader(vcfFile)) {
        while (reader.hasNext()) {
          final VcfRecord record = reader.next();
          final String id = record.getId();
          if (!VcfRecord.MISSING.equals(id)) {
            final String[] ids = StringUtils.split(id, ';');
            final List<RangeList.RangeData<String>> annos;
            if (annotations.containsKey(record.getSequenceName())) {
              annos = annotations.get(record.getSequenceName());
            } else {
              annos = new ArrayList<>();
              annotations.put(record.getSequenceName(), annos);
            }
            final int start = record.getStart();
            final int end = start + record.getRefCall().length();
            final RangeList.RangeData<String> range = new RangeData<>(start, end, ids[0]);
            annos.add(range);
            for (int i = 1; i < ids.length; i++) {
              range.addMeta(ids[i]);
            }
          }
        }
      }
    }
    final Map<String, RangeList<String>> annotationSearch = new HashMap<>();
    for (final Map.Entry<String, List<RangeList.RangeData<String>>> me : annotations.entrySet()) {
      annotationSearch.put(me.getKey(), new RangeList<>(me.getValue()));
    }
    mAnnotations = annotationSearch;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    //No header changes necessary, modifying VCF id column
  }

  private List<String> annotate(String chr, int loc) {
    List<String> anno = null;
    if (mAnnotations.containsKey(chr)) {
      anno = mAnnotations.get(chr).find(loc);
    }
    return anno;
  }

  @Override
  public void annotate(VcfRecord rec) {
    final List<String> annotation = annotate(rec.getSequenceName(), rec.getStart());
    if (annotation != null) {
      if (annotation.size() > 0) {
        rec.setId(annotation.toArray(new String[annotation.size()]));
      }
    }
  }

}
