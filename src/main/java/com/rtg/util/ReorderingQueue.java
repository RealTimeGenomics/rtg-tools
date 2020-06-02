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
package com.rtg.util;


import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * A general purpose reordering and duplicate removal queue. Assumes that records are fed in in
 * approximately coordinate order for each reference. Note that any records that are determined
 * equal according to the configured Comparator will be considered duplicates and only the first
 * will be output.
 * @param <T> the record type.
 */
public abstract class ReorderingQueue<T> implements AutoCloseable {

  private final int mWindowSize;

  protected final TreeSet<T> mRecordSet;

  protected T mLastWrittenRecord = null;

  private String mPreviousReference = null;

  private int mMaxCapacityUsed = 0;

  private int mDuplicates = 0;

  /**
   * Constructor
   * @param windowSize the initial buffer distance
   * @param comparator used to order the records and identify duplicates.
   */
  public ReorderingQueue(int windowSize, Comparator<T> comparator) {
    mWindowSize = windowSize;
    mRecordSet = new TreeSet<>(comparator);
  }

  // We don't have a common interface for getting a position out of things like SAMRecords / VCFRecords / BEDRecords, so do it via abstract methods.
  /**
   * Get the reference sequence name from a record.
   * @param record the input record
   * @return the name of the reference sequence that this record is positioned on
   */
  protected abstract String getReferenceName(T record);

  /**
   * Get the coordinate position of a record. Doesn't matter if it is 0-based or 1-based, as long as it is consistent.
   * @param record the input record
   * @return the coordinate on the reference sequence that this record is positioned on
   */
  protected abstract int getPosition(T record);

  /**
   * This method causes the record to actually leave the building
   * @param rec the record
   * @throws IOException if an IO exception occurs
   */
  protected abstract void flushRecord(T rec) throws IOException;

  /**
   * Called when a record is seen that cannot be output due to reordering failure (buffer size too small).
   * @param rec the record
   */
  protected abstract void reportReorderingFailure(T rec);

  /**
   * Add a record to the buffer. If the record is a duplicate it will
   * be discarded. If the record can not be output due to already flushed
   * records, a call to <code>reportReorderingFailure</code> will be made.
   * @param record the record to add
   * @return true if record was added and will be written.
   * @throws IOException if an IO exception occurs
   */
  public boolean addRecord(T record) throws IOException {
    final boolean ret;
    final String referenceIdStr = getReferenceName(record);
    if (!referenceIdStr.equals(mPreviousReference)) {
      flush();
      mPreviousReference = referenceIdStr;
    }
    if (mLastWrittenRecord == null
        || getPosition(mLastWrittenRecord) < getPosition(record)) { // Must be < not <= here to ensure we can still detect duplicates.
      if (!mRecordSet.add(record)) {
        ++mDuplicates;
        ret = false;
      } else {
        ret = true;
      }
      mMaxCapacityUsed = Math.max(mRecordSet.size(), mMaxCapacityUsed);

      final int lastPos = getPosition(mRecordSet.last());
      while (lastPos - getPosition(mRecordSet.first()) > mWindowSize) {
        final T firstRecord = mRecordSet.first();
        mRecordSet.remove(firstRecord);
        flushRecord(firstRecord);
        mLastWrittenRecord = firstRecord;
      }
    } else {
      reportReorderingFailure(record);
      return false;
    }
    return ret;
  }

  /**
   * Return the maximum capacity used during buffering.
   *
   * @return the largest size reached by the buffer.
   */
  public int getMaxCapacityUsed() {
    return mMaxCapacityUsed;
  }

  /**
   * Return the number of duplicated records.  Only one copy of such records
   * are written.
   *
   * @return duplicated record count
   */
  public int getDuplicateCount() {
    return mDuplicates;
  }

  /**
   * @return the window size in bases
   */
  public int getWindowSize() {
    return mWindowSize;
  }

  protected void flush() throws IOException {
    for (final T rec : mRecordSet) {
      flushRecord(rec);
    }
    mRecordSet.clear();
    mLastWrittenRecord = null;
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect. If you override this method you must call super.close().
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    flush();
  }
}
