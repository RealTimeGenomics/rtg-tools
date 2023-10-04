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

import com.rtg.util.intervals.SequenceNameLocus;

/**
 * Defines an integer identifier and status flags for a variant.
 */
public interface VariantId extends SequenceNameLocus {

  /** Variant was in a too-hard region */
  byte STATUS_SKIPPED = 0b1;
  /** Variant was included in diploid match */
  byte STATUS_GT_MATCH = 0b10;
  /** Variant was included during haploid matching */
  byte STATUS_ALLELE_MATCH = 0b100;
  /** Variant was excluded */
  byte STATUS_NO_MATCH = 0b1000;
  /** Variant was outside eval regions */
  byte STATUS_OUTSIDE_EVAL = 0b10000;
  /** Variant was able to be matched in any explored path */
  byte STATUS_ANY_MATCH = 0b100000;

  /** @return the ID assigned to this variant */
  int getId();

  /**
   * Sets status flag
   * @param status the status value
   */
  void setStatus(byte status);

  /**
   * @param status the status value
   * @return true if the variant has the status flag set
   */
  boolean hasStatus(byte status);
}
