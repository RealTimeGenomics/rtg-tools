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


import com.reeltwo.jumble.annotations.JumbleIgnore;
import com.rtg.util.diagnostic.Diagnostic;

/**
 * Hold counts of SAM records by processing category.
 */
@JumbleIgnore
public class SimpleRecordCounter implements RecordCounter {

  protected long mRecordCount = 0;
  protected long mNumInvalidRecords = 0;
  protected long mNumFilteredRecords = 0;
  protected long mNumDuplicateRecords = 0;
  protected long mNumOverCoverageRecords = 0;

  /**
   * Accumulate statistics from another counter
   * @param rc the other counter
   */
  public void incrementCounts(RecordCounter rc) {
    mRecordCount += rc.getTotalRecordsCount();
    mNumInvalidRecords += rc.getInvalidRecordsCount();
    mNumFilteredRecords += rc.getFilteredRecordsCount();
    mNumDuplicateRecords += rc.getDuplicateRecordsCount();
    mNumOverCoverageRecords += rc.getOverCoverageRecordsCount();
  }

  @Override
  public long getTotalRecordsCount() {
    return mRecordCount;
  }

  @Override
  public long getInvalidRecordsCount() {
    return mNumInvalidRecords;
  }

  @Override
  public long getFilteredRecordsCount() {
    return mNumFilteredRecords;
  }

  @Override
  public long getDuplicateRecordsCount() {
    return mNumDuplicateRecords;
  }

  @Override
  public long getOverCoverageRecordsCount() {
    return mNumOverCoverageRecords;
  }

  @Override
  public long getOutputRecordsCount() {
    return getTotalRecordsCount() - getInvalidRecordsCount() - getDuplicateRecordsCount() - getFilteredRecordsCount() - getOverCoverageRecordsCount();
  }

  /**
   * Report counts to the log
   */
  public void reportCounts() {
    final String invalidRecordsWarning = getInvalidRecordsCount() + " records skipped because of SAM format problems.";
    if (getInvalidRecordsCount() > 0) {
      Diagnostic.warning(invalidRecordsWarning);
    } else {
      Diagnostic.userLog(invalidRecordsWarning);
    }
    if (getFilteredRecordsCount() > 0) {
      Diagnostic.userLog(getFilteredRecordsCount() + " records skipped due to input filtering criteria");
    }
    if (getDuplicateRecordsCount() > 0) {
      Diagnostic.userLog(getDuplicateRecordsCount() + " records skipped due to duplicate detection");
    }
    if (getOverCoverageRecordsCount() > 0) {
      Diagnostic.userLog(getOverCoverageRecordsCount() + " records skipped in extreme coverage regions");
    }
    Diagnostic.userLog(getOutputRecordsCount() + " records processed");
  }
}
