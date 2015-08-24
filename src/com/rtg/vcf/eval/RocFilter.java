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

import java.util.Locale;

import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;

/**
 * Interface defining ROC filters
 */
public enum RocFilter {


  /** accepts everything **/
  ALL {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return true;
    }

    @Override
    String filename() {
      return "weighted_roc.tsv";
    }
  },
  /** all homozygous **/
  HOMOZYGOUS {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return VcfUtils.isHomozygousAlt(rec, sample);
    }
  },
  /** all heterozygous **/
  HETEROZYGOUS {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return VcfUtils.isHeterozygous(rec, sample);
    }
  },
  /** all complex calls **/
  COMPLEX {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return VcfUtils.isComplexScored(rec);
    }
  },
  /** all simple (non complex) calls **/
  SIMPLE {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return !VcfUtils.isComplexScored(rec);
    }
  },
  /** homozygous complex calls **/
  HOMOZYGOUS_COMPLEX {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return VcfUtils.isComplexScored(rec) && VcfUtils.isHomozygousAlt(rec, sample);
    }
  },
  /** homozygous simple (non complex) calls **/
  HOMOZYGOUS_SIMPLE {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return !VcfUtils.isComplexScored(rec) && VcfUtils.isHomozygousAlt(rec, sample);
    }
  },
  /** heterozygous complex calls **/
  HETEROZYGOUS_COMPLEX {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return VcfUtils.isComplexScored(rec) && VcfUtils.isHeterozygous(rec, sample);
    }
  },
  /** heterozygous simple (non complex) calls **/
  HETEROZYGOUS_SIMPLE {
    @Override
    boolean accept(VcfRecord rec, int sample) {
      return !VcfUtils.isComplexScored(rec) && VcfUtils.isHeterozygous(rec, sample);
    }
  };

  static final String ROC_EXT = "_roc.tsv";

  /**
   * Tests specified record
   * @param rec record to be tested
   * @param sample sample number
   * @return if accepted returns true, false otherwise
   */
  abstract boolean accept(VcfRecord rec, int sample);

  /**
   * Get the name of the default output file for this filter
   * @return the output file name
   */
  String filename() {
    return name().toLowerCase(Locale.ROOT) + ROC_EXT;
  }
}
