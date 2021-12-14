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
package com.rtg.variant.cnv.cnveval;

import java.util.ArrayList;
import java.util.List;

import com.rtg.util.Utils;
import com.rtg.util.intervals.SequenceNameLocusSimple;
import com.rtg.variant.cnv.cnveval.CnaVariant.RegionContext;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.eval.RocSortValueExtractor;
import com.rtg.vcf.eval.VariantSetType;

/**
 * Simple interface for objects generating a column in the evaluation output BED file.
 */
interface BedAnnotation {

  /** @return the annotation name. */
  String name();

  /**
   * @param v the variant
   * @return the annotation value for this variant.
   */
  String value(CnaVariant v);

  static List<BedAnnotation> makeBedAnnotations(final VariantSetType setType, final RocSortValueExtractor extractor, final int sampleCol, final boolean writeNames) {
    final String incorrectTag;
    switch (setType) {
    case BASELINE:
      incorrectTag = "FN";
      break;
    case CALLS:
      incorrectTag = "FP";
      break;
    default:
      throw new RuntimeException("Unknown variant set type: " + setType);
    }
    final List<BedAnnotation> annots = new ArrayList<>();

    annots.add(new BedAnnotation() {
      @Override
      public String name() {
        return "status";
      }

      @Override
      public String value(CnaVariant v) {
        return v.context() != RegionContext.NORMAL ? "IGN" : v.isCorrect() ? "TP" : incorrectTag;
      }
    });

    annots.add(new BedAnnotation() {
      @Override
      public String name() {
        return "svtype";
      }

      @Override
      public String value(CnaVariant v) {
        return v.cnaType().name();
      }
    });

    annots.add(new BedAnnotation() {
      @Override
      public String name() {
        return "span";
      }

      @Override
      public String value(CnaVariant v) {
        return v.spanType().name();
      }
    });

    annots.add(new BedAnnotation() {
      @Override
      public String name() {
        return "context";
      }

      @Override
      public String value(CnaVariant v) {
        return v.context().name();
      }
    });

    if (writeNames) {
      annots.add(new BedAnnotation() {
        @Override
        public String name() {
          return "name";
        }

        @Override
        public String value(CnaVariant v) {
          return v.names();
        }
      });
    }

    annots.add(new BedAnnotation() {
      @Override
      public String name() {
        return "original_pos";
      }

      @Override
      public String value(CnaVariant v) {
        final SequenceNameLocusSimple originalSpan = new SequenceNameLocusSimple(v.record().getSequenceName(),
            v.record().getStart(), VcfUtils.getEnd(v.record()));
        return originalSpan.toString();
      }
    });

    if (extractor != null && extractor != RocSortValueExtractor.NULL_EXTRACTOR) {
      annots.add(new BedAnnotation() {
        @Override
        public String name() {
          return extractor.toString();
        }

        @Override
        public String value(CnaVariant v) {
          return Utils.realFormat(extractor.getSortValue(v.record(), sampleCol), 4);
        }
      });
    }

    return annots;
  }
}
