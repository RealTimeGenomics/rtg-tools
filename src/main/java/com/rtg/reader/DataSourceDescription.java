/*
 * Copyright (c) 2017. Real Time Genomics Limited.
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
package com.rtg.reader;

/**
 * Hold metadata associated with a data source.
 */
public class DataSourceDescription {

  private final SourceFormat mSourceFormat;
  private final QualityFormat mQualityFormat;
  private final boolean mIsPairedEnd;
  private final boolean mIsInterleaved;
  private final boolean mIsCompleteGenomics;

  /**
   * Construct a description of a data source
   * @param sourceFormat format of data
   * @param qualityFormat quality format of data (if relevant)
   * @param isPairedEnd true if the data is paired-end
   * @param isInterleaved true if the pairs are interleaved
   * @param isCompleteGenomics true if the data is Complete Genomics
   */
  public DataSourceDescription(final SourceFormat sourceFormat, final QualityFormat qualityFormat, boolean isPairedEnd, final boolean isInterleaved, final boolean isCompleteGenomics) {
    mSourceFormat = sourceFormat;
    mQualityFormat = qualityFormat;
    mIsPairedEnd = isPairedEnd;
    mIsInterleaved = isInterleaved;
    mIsCompleteGenomics = isCompleteGenomics;
  }

  public SourceFormat getSourceFormat() {
    return mSourceFormat;
  }

  public QualityFormat getQualityFormat() {
    return mQualityFormat;
  }

  public boolean isPairedEnd() {
    return mIsPairedEnd;
  }

  public boolean isInterleaved() {
    return mIsInterleaved;
  }

  public boolean isCompleteGenomics() {
    return mIsCompleteGenomics;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    if (mSourceFormat != SourceFormat.SAM && isInterleaved()) {
      sb.append("interleaved ");
    }
    if (isPairedEnd()) {
      sb.append("paired-end ");
    }
    sb.append(getSourceFormat());
    if (isCompleteGenomics()) {
      sb.append("-CG");
    }
    if (getQualityFormat() != null && getQualityFormat() != QualityFormat.UNKNOWN && getQualityFormat() != QualityFormat.SANGER) {
      sb.append("-").append(getQualityFormat());
    }
    return sb.toString();
  }
}
