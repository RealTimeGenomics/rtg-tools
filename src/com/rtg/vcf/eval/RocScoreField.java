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

import com.rtg.util.MathUtils;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.annotation.AbstractDerivedAnnotation;
import com.rtg.vcf.annotation.DerivedAnnotations;

/**
 * Enumeration of possible ROC score fields
 */
public enum RocScoreField {

  /** use <code>QUAL</code> VCF field to sort */
  QUAL {
    @Override
    RocSortValueExtractor getExtractor(final String fieldName, final RocSortOrder order) {
      return new RocSortValueExtractor() {
        @Override
        boolean requiresSample() {
          return false;
        }
        @Override
        RocSortOrder getSortOrder() {
          return order;
        }
        @Override
        double getSortValue(VcfRecord rec, int sampleNo) {
          final String qualStr = rec.getQuality();
          if (VcfRecord.MISSING.equals(qualStr)) {
            return Double.NaN;
          }
          try {
            return Double.parseDouble(qualStr);
          } catch (NumberFormatException e) {
            throw new NoTalkbackSlimException("Invalid QUAL value = " + rec.getQuality());
          }
        }
        @Override
        public String toString() {
          return "QUAL";
        }
      };
    }
  },
  /** use VCF info field to provide sort value */
  INFO {
    @Override
    RocSortValueExtractor getExtractor(final String fieldName, final RocSortOrder order) {
      return new RocSortValueExtractor() {
        @Override
        boolean requiresSample() {
          return false;
        }
        @Override
        RocSortOrder getSortOrder() {
          return order;
        }
        @Override
        double getSortValue(VcfRecord rec, int sampleNo) {
          final double val = VcfUtils.getDoubleInfoFieldFromRecord(rec, fieldName);
          if (MathUtils.approxEquals(val, 0, 0.00000001)) {
            return 0;
          }
          return val;
        }
        @Override
        public String toString() {
          return fieldName + " (INFO)";
        }
      };
    }
  },
  /** use VCF format field to provide sort value */
  FORMAT {
    @Override
    RocSortValueExtractor getExtractor(final String fieldName, final RocSortOrder order) {
      return new RocSortValueExtractor() {
        @Override
        boolean requiresSample() {
          return true;
        }
        @Override
        RocSortOrder getSortOrder() {
          return order;
        }
        @Override
        double getSortValue(VcfRecord rec, int sampleNo) {
          final double val = VcfUtils.getDoubleFormatFieldFromRecord(rec, sampleNo, fieldName);
          if (MathUtils.approxEquals(val, 0, 0.00000001)) {
            return 0;
          }
          return val;
        }
        @Override
        public String toString() {
          return fieldName + " (FORMAT)";
        }
      };
    }
  },
  /** use derived field to provide sort value */
  DERIVED {
    @Override
    RocSortValueExtractor getExtractor(final String fieldName, final RocSortOrder order) {
      final DerivedAnnotations derived;
      final String field = fieldName.toUpperCase(Locale.getDefault());
      try {
        derived = DerivedAnnotations.valueOf(field);
      } catch (IllegalArgumentException e) {
        throw new NoTalkbackSlimException("Unrecognized derived annotation \"" + field + "\", must be one of " + Arrays.toString(DerivedAnnotations.values()));
      }
      final AbstractDerivedAnnotation<?> anno = derived.getAnnotation(); // XXX we need to provide the ability to call ADA.checkHeader(), or some annotations won't work.
      return new RocSortValueExtractor() {
        @Override
        boolean requiresSample() {
          return true;
        }
        @Override
        RocSortOrder getSortOrder() {
          return order;
        }
        @Override
        double getSortValue(VcfRecord rec, int sampleNo) {
          final Double val = (Double) anno.getValue(rec, sampleNo);
          if (val == null) {
            return Double.NaN;
          }
          if (MathUtils.approxEquals(val, 0, 0.00000001)) {
            return 0;
          }
          return val;
        }
        @Override
        public String toString() {
          return fieldName + " (derived)";
        }
      };
    }
  };

  abstract RocSortValueExtractor getExtractor(String fieldName, RocSortOrder order);

}
