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
package com.rtg.tabix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Used as basis for reading files for <code>TABIX</code> indexing
 */
@TestClass("com.rtg.tabix.SamPositionReaderTest")
abstract class AbstractPositionReader implements BlockCompressedPositionReader {

  private final int[] mTabs;
  private int mTabsUsed;
  private String mCurrentLine;
  private final BlockCompressedLineReader mReader;
  private final char mMeta;
  private String mNextLine;
  private long mNextVirtualOffset;
  private long mCurrentVirtualOffset;

  protected int mRefId;
  protected String mLastReferenceName;
  protected String mReferenceName;
  protected int mStartPosition;
  protected int mLengthOnReference;
  protected int mBin;
  protected HashMap<String, Integer> mSequenceNames;
  protected ArrayList<String> mNamesList;

  /**
   * Constructor
   * @param reader source of file
   * @param numColumns number of columns in file
   * @param meta character signifying start of header line
   * @param skip number of lines to skip at beginning of file
   */
  AbstractPositionReader(BlockCompressedLineReader reader, int numColumns, char meta, int skip) {
    mReader = reader;
    mTabs = new int[numColumns + 1];
    mTabs[0] = -1;
    mTabsUsed = 1;
    mRefId = -1;
    mMeta = meta;
    mSequenceNames = new HashMap<>();
    mNamesList = new ArrayList<>();
    for (int i = 0; i < skip; ++i) {
      mReader.readLine();
    }
    populateNext();
  }

  @Override
  public String getRecord() {
    return mCurrentLine;
  }

  @Override
  public void seek(long virtualOffset) throws IOException {
    mReader.seek(virtualOffset);
    mCurrentLine = null;
    mCurrentVirtualOffset = 0;
    mNextVirtualOffset = 0;
    mStartPosition = 0;
    mLengthOnReference = 0;
    mBin = 0;
    mReferenceName = null;
    mLastReferenceName = null;
    mRefId = Integer.MIN_VALUE;
    mTabsUsed = 1;
    populateNext();
  }

  /**
   * implementors should set the {@link AbstractPositionReader#mReferenceName} field to
   * the name of the reference sequence that applies to the current record
   * @throws java.io.IOException if there is an I/O problem
   */
  protected abstract void setReferenceName() throws IOException;

  /**
   * implementors should set the {@link AbstractPositionReader#mStartPosition} field to
   * the start position (0-based) on the reference of the current record and the {@link AbstractPositionReader#mLengthOnReference}
   * field to the length of the region the current record applies to
   * @throws java.io.IOException if there is an I/O problem
   */
  protected abstract void setStartAndLength() throws IOException;

  @Override
  public void next() throws IOException {

    mCurrentLine = mNextLine;
    mCurrentVirtualOffset = mNextVirtualOffset;
    mTabsUsed = 1;
    populateNext();
    mLastReferenceName = mReferenceName;
    setReferenceName();
    setStartAndLength();
    mBin = TabixIndexer.reg2bin(mStartPosition, mStartPosition + mLengthOnReference);
    if (mLastReferenceName == null || !mLastReferenceName.equals(mReferenceName)) {
      final Integer pos = mSequenceNames.get(mReferenceName);
      if (pos != null) {
        mRefId = pos;
      } else {
        mRefId = mSequenceNames.size();
        mSequenceNames.put(mReferenceName, mSequenceNames.size());
        mNamesList.add(mReferenceName);
      }
    }
  }

  @Override
  public int getLengthOnReference() {
    return mLengthOnReference;
  }

  @Override
  public int getStartPosition() {
    return mStartPosition;
  }

  @Override
  public String getReferenceName() {
    return mReferenceName;
  }

  @Override
  public int getReferenceId() {
    return mRefId;
  }

  @Override
  public boolean hasReference() {
    return true;
  }

  @Override
  public int getBinNum() {
    return mBin;
  }

  @Override
  public boolean hasCoordinates() {
    return true;
  }

  @Override
  public boolean isUnmapped() {
    return false;
  }

  @Override
  public List<String> getSequenceNames() {
    return mNamesList;
  }

  @Override
  public void close() throws IOException {
    mReader.close();
  }

  @Override
  public boolean hasNext() {
    return mNextLine != null;
  }

  @Override
  public long getVirtualOffset() {
    return mCurrentVirtualOffset;
  }

  @Override
  public long getNextVirtualOffset() {
    return mNextVirtualOffset;
  }

  /**
   * Get the value from the given column of the current record
   * @param col zero base column
   * @return the value
   * @throws java.io.IOException if there is an I/O problem
   */
  protected String getColumn(int col) throws IOException {
    populateTabs(col);
    final int pos1 = mTabs[col];
    final int pos2 = mTabs[col + 1];
    if (pos1 >= mCurrentLine.length()) {
      throw new IOException("Data file did not contain expected column " + (col + 1) + " on line: " + mCurrentLine);
    }
    return mCurrentLine.substring(pos1 + 1, pos2);
  }

  /**
   * Get the value from the given column of the current record as an integer
   * @param col zero base column
   * @return the value
   * @throws java.io.IOException if there is an I/O problem
   */
  protected int getIntColumn(int col) throws IOException {
    try {
      return Integer.parseInt(getColumn(col));
    } catch (final IllegalArgumentException e) {
      //illegal argument == badly formed column
      throw new IOException("Data file did not contain an integer in column " + (col + 1) + " on line: " + mCurrentLine, e);
    }
  }

  private void populateTabs(int colNo) {
    while (mTabsUsed <= colNo + 1) {
      int pos = mTabs[mTabsUsed - 1] + 1;
      while (pos < mCurrentLine.length() && mCurrentLine.charAt(pos) != '\t') {
        ++pos;
      }
      mTabs[mTabsUsed++] = pos;
    }
  }

  private void populateNext() {
    do {
      mNextVirtualOffset = mReader.getFilePointer();
      mNextLine = mReader.readLine();
    } while (mNextLine != null && (mNextLine.length() == 0 || mNextLine.charAt(0) == mMeta));
  }
}
