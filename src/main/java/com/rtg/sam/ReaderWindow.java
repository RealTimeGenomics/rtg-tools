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

package com.rtg.sam;

import java.io.IOException;
import java.util.Iterator;

/**
 * A cache for records fetched from mapping files for one sequence.
 * Records may be fetched more than once except when flush has specified that they will not be requested again.
 *
 * @param <R> type of record to return
 */
public interface ReaderWindow<R extends ReaderRecord<R>> {

  /**
   * Fetch all records whose extent overlaps the specified region. The extent is
   * determined by unrolling the cigar. It is permissible to include records that don't
   * actually overlap if this makes the internal calculations simpler. However, it is expected
   * that these will be kept to a minimum.
   * Callers may receive over-coverage records that need to be dealt with appropriately.
   * @param start first position in region selected (0 based, inclusive).
   * @param end last position in region selected (0 based, exclusive).
   * @return all records selected in any order.
   * @throws IOException if an IO error occurs
   */
  Iterator<R> recordsOverlap(int start, int end) throws IOException;

  /**
   * Explicitly request that records be processed up to the specified point. Ordinarily this happens
   * automatically due to calls to <code>recordsOverlap</code>. But sometimes we want to skip over regions without
   * looking at the records.
   * @param end the end position
   */
  void advanceBuffer(int end);

  /**
   * Specify that all records whose start position is within the specified region
   * will not be requested again by a <code>recordsAtStart</code> or a <code>recordsOverlap</code> call.
   * @param start first position in region selected (0 based, inclusive).
   * @param end last position in region selected (0 based, exclusive).
   * @throws IOException if an IO error occurs
   */
  void flush(int start, int end) throws IOException;

  /**
   * Returns the start of the earliest chuck that has not had a flush request.
   * @return position of minimum chunk with work outstanding.
   */
  int flushedTo();
}
