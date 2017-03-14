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
import java.util.Collection;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.bed.NamedBedRangeLoader;
import com.rtg.util.intervals.RangeList;
import com.rtg.util.intervals.ReferenceRanges;
import com.rtg.vcf.header.InfoField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Annotates a VCF records that match BED regions with the text from the matching BED region.
 * Note: Only uses the values from the name BED column (column 4).
 */
@TestClass("com.rtg.vcf.VcfAnnotatorCliTest")
public class BedVcfAnnotator implements VcfAnnotator {


  /** Per chromosome annotations. */
  private final ReferenceRanges<String> mAnnotations;

  private final String mInfoId;
  private final String mInfoDescription;

  /**
   * Constructor
   * @param infoId if non-null, BED annotations will be added as an INFO field with this ID, otherwise add to VCF id column.
   * @param description if non-null, use this description for the INFO field header, if it doesn't already exist.
   * @param bedFiles BED files containing annotations
   * @throws IOException if the BED file could not be loaded.
   */
  public BedVcfAnnotator(String infoId, String description, Collection<File> bedFiles) throws IOException {
    mInfoId = infoId;
    mInfoDescription = description;
    final NamedBedRangeLoader bedLoader = new NamedBedRangeLoader();
    bedLoader.loadRanges(bedFiles);
    mAnnotations = bedLoader.getReferenceRanges();
  }

  private List<String> annotate(String chr, int loc) {
    List<String> anno = null;
    final RangeList<String> chrRanges = mAnnotations.get(chr);
    if (chrRanges != null) {
      anno = chrRanges.find(loc);
    }
    return anno;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    if (mInfoId != null) {
      header.ensureContains(new InfoField(mInfoId, MetaType.STRING, new VcfNumber("."), mInfoDescription));
    }
  }

  @Override
  public void annotate(VcfRecord rec) {
    final List<String> annotation = annotate(rec.getSequenceName(), rec.getStart());
    if (annotation != null) {
      if (annotation.size() > 0) {
        if (mInfoId == null) {
          rec.setId(annotation.toArray(new String[annotation.size()]));
        } else {
          rec.addInfo(mInfoId, annotation.toArray(new String[annotation.size()]));
        }
      }
    }
  }
}
