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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.rtg.util.Pair;
import com.rtg.vcf.VcfIterator;
import com.rtg.vcf.header.VcfHeader;

/**
 * Iterate over the sequences returning sets of called and baseline variants
 */
public interface VariantSet extends Closeable {

  /**
   * @return the variants for the next sequence or null if there are no more.
   * @throws IOException for any problem reading variants from the underlying source
   */
  Pair<String, Map<VariantSetType, List<Variant>>> nextSet() throws IOException;

  /**
   * @return header for baseline files
   */
  VcfHeader baselineHeader();

  /**
   * @return index of sample within baseline or -1 if not selecting a sample
   */
  int baselineSample();

  /**
   * @return header for called files
   */
  VcfHeader calledHeader();

  /**
   * @return index of sample within calls or -1 if not selecting a sample
   */
  int calledSample();

  /**
   * @return the number of baseline variants that were skipped during loading
   */
  int getNumberOfSkippedBaselineVariants();

  /**
   * @return the number of called variants that were skipped during loading
   */
  int getNumberOfSkippedCalledVariants();

  /**
   * Gets the baseline variants on the specified reference sequence
   * @param sequenceName the sequence of interest
   * @return an iterator supplying the baseline variants on the specified sequence
   * @throws IOException when I/O fails
   */
  VcfIterator getBaselineVariants(String sequenceName) throws IOException;

  /**
   * Gets the called variants on the specified reference sequence
   * @param sequenceName the sequence of interest
   * @return an iterator supplying the called variants on the specified sequence
   * @throws IOException when I/O fails
   */
  VcfIterator getCalledVariants(String sequenceName) throws IOException;
}
