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
package com.rtg.reader;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.rtg.mode.SequenceType;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.InformationType;

/**
 * Concatenates multiple <code>SequenceDataSource</code>s
 *
 * @param <T> <code>SequenceDataSource</code> or a subclass
 */
public class ConcatSequenceDataSource<T extends SequenceDataSource> implements SequenceDataSource {

  private final List<T> mSources;
  private final Iterator<T> mIterator;
  private final List<String> mNames;
  private SequenceDataSource mCurrent;
  private long mWarningCount;
  private long mDustedCount;
  private int mNameIndex;
  private long mMinLength = Long.MAX_VALUE;
  private long mMaxLength = Long.MIN_VALUE;

  /**
   *
   * @param sources <code>SequencesDataSource</code>s to read
   * @param names names of sources to print to user (pass <code>null</code> to not print)
   */
  public ConcatSequenceDataSource(final List<T> sources, final List<String> names) {
    if (sources == null || sources.size() == 0) {
      throw new IllegalArgumentException("Cannot concatenate 0 sources");
    }
    mSources = sources;
    mIterator = mSources.iterator();
    mNames = names;
    mCurrent = mIterator.next();
    mWarningCount = 0;
    mNameIndex = 0;
    mDustedCount = 0;
    printFileNumber();
  }

  @Override
  public SequenceType type() {
    return mCurrent.type();
  }

  @Override
  public boolean nextSequence() throws IOException {
    while (!mCurrent.nextSequence()) {
      if (!mIterator.hasNext()) {
        return false;
      }
      mWarningCount += mCurrent.getWarningCount();
      mDustedCount += mCurrent.getDusted();
      mMinLength = Math.min(mMinLength, mCurrent.getMinLength());
      mMaxLength = Math.max(mMaxLength, mCurrent.getMaxLength());
      mCurrent.close();
      mCurrent = mIterator.next();
      ++mNameIndex;
      printFileNumber();
    }
    return true;
  }

  @Override
  public String name() throws IOException {
    return mCurrent.name();
  }

  @Override
  public byte[] sequenceData() throws IOException {
    return mCurrent.sequenceData();
  }

  @Override
  public byte[] qualityData() throws IOException {
    return mCurrent.qualityData();
  }

  @Override
  public boolean hasQualityData() {
    return mCurrent.hasQualityData();
  }

  @Override
  public int currentLength() throws IOException {
    return mCurrent.currentLength();
  }

  @Override
  public void close() throws IOException {
    mCurrent.close();
    while (mIterator.hasNext()) {
      mIterator.next().close();
    }
  }

  @Override
  public void setDusting(boolean val) {
    for (final SequenceDataSource source : mSources) {
      source.setDusting(val);
    }
  }

  @Override
  public long getWarningCount() {
    return mWarningCount + mCurrent.getWarningCount();
  }

  private void printFileNumber() {
    if (mNames != null) {
      Diagnostic.info(InformationType.PROCESSING_ITEM_N_OF_N, true, "", mNames.get(mNameIndex), Integer.toString(mNameIndex + 1), Integer.toString(mSources.size()));
    }
  }

  @Override
  public long getDusted() {
    return mDustedCount + mCurrent.getDusted();
  }

  @Override
  public long getMaxLength() {
    return Math.max(mMaxLength, mCurrent.getMaxLength());
  }

  @Override
  public long getMinLength() {
    return Math.min(mMinLength, mCurrent.getMinLength());
  }
}
