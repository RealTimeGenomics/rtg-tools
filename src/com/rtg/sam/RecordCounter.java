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
package com.rtg.sam;

/**
 *
 */
public interface RecordCounter {
  /**
   * Gets the total number of records that were invalid.
   * @return the sum of all invalid counts
   */
  long getInvalidRecordsCount();

  /**
   * Gets the number of records that were ignored due to filtering criteria
   * @return the count of records ignored due to user-filtering
   */
  long getFilteredRecordsCount();

  /**
   * Gets the number of records that were detected as duplicates and ignored
   * @return the number of duplicate records filtered
   */
  long getDuplicateRecordsCount();

  /**
   * Gets the number of records that were ignored due to over-coverage
   * @return the number of records ignored
   */
  long getOverCoverageRecordsCount();

  /**
   * Gets the total number of records that were returned to the caller.
   * @return the count of records returned to the caller.
   */
  long getOutputRecordsCount();

  /**
   * Gets the total number of input records.
   * @return the count of all input records (regardless of validity or filtering status).
   */
  long getTotalRecordsCount();
}
