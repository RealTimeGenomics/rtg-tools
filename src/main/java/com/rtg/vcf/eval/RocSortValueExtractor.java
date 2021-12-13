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

package com.rtg.vcf.eval;

import java.util.Arrays;
import java.util.Locale;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.StringUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Class template for value extractor.
 */
@TestClass("com.rtg.vcf.eval.RocScoreFieldTest")
public abstract class RocSortValueExtractor {

  /**
   * Creates an appropriate sort value extractor for the specified field
   * @param scoreField the field to extract
   * @param sortOrder the sort order
   * @return the extractor
   */
  public static RocSortValueExtractor getRocSortValueExtractor(String scoreField, RocSortOrder sortOrder) {
    final RocScoreField fieldType;
    final String fieldName;
    if (scoreField == null) {
      return RocSortValueExtractor.NULL_EXTRACTOR;
    } else {
      final String[] splitScore = StringUtils.split(scoreField, '.', 2);
      if (splitScore.length > 1) {
        final String fieldTypeName = splitScore[0].toUpperCase(Locale.getDefault());
        try {
          fieldType = RocScoreField.valueOf(fieldTypeName);
        } catch (IllegalArgumentException e) {
          throw new NoTalkbackSlimException("Unrecognized field type \"" + fieldTypeName + "\", must be one of " + Arrays.toString(RocScoreField.values()));
        }
        fieldName = splitScore[1];
      } else if (VcfUtils.QUAL.equals(scoreField)) {
        fieldType = RocScoreField.QUAL;
        fieldName = "UNUSED";
      } else {
        fieldType = RocScoreField.FORMAT;
        fieldName = scoreField;
      }
    }
    return fieldType.getExtractor(fieldName, sortOrder);
  }

  /**
   * @return true if the extractor requires a sample column
   */
  public abstract boolean requiresSample();

  /**
   * Gets the score value from a record
   * @param rec the VCF record
   * @param sampleNo the index of the sample of interest (or -1 for a sample-free record)
   * @return the score value
   */
  public abstract double getSortValue(VcfRecord rec, int sampleNo);

  /**
   * @return the order in which the scores should be sorted such that "good" scores are first.
   */
  public abstract RocSortOrder getSortOrder();

  /**
   * Lets the extractor see the header in order to get any required information.
   * @param header a VCF header
   */
  public void setHeader(VcfHeader header) { }

  /** Dummy null extractor for testing purposes */
  public static final RocSortValueExtractor NULL_EXTRACTOR = new RocSortValueExtractor() {

    @Override
    public boolean requiresSample() {
      return false;
    }

    @Override
    public double getSortValue(VcfRecord rec, int sampleNo) {
      return 0;
    }

    @Override
    public RocSortOrder getSortOrder() {
      return RocSortOrder.ASCENDING;
    }

    @Override
    public String toString() {
      return "NONE";
    }
  };
}
