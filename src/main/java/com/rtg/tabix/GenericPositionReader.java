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


/**
 * Reader for providing data for generic files to be <code>TABIX</code> indexed
 */
public class GenericPositionReader extends AbstractPositionReader {
  private final boolean mOneBased;
  private final int mStartCol;
  private final int mSeqCol;
  private final int mEndCol;

  /**
   * @param reader read positions out of this reader
   * @param options the options to use while reading positions
   */
  public GenericPositionReader(BlockCompressedLineReader reader, TabixIndexer.TabixOptions options) {
    super(reader, noColsNeeded(options), (char) options.mMeta, options.mSkip);
    mOneBased = !options.mZeroBased;
    mStartCol = options.mStartCol;
    mEndCol = options.mEndCol;
    mSeqCol = options.mSeqCol;
  }

  private static int noColsNeeded(TabixIndexer.TabixOptions options) {
    //cols are zero based in options class
    return Math.max(Math.max(options.mStartCol, options.mEndCol), options.mSeqCol) + 1;
  }

  @Override
  protected void setReferenceName() throws IOException {
    mReferenceName = getColumn(mSeqCol);
  }

  @Override
  protected void setStartAndLength() throws IOException {
      mStartPosition = getIntColumn(mStartCol) - (mOneBased ? 1 : 0);
      if (mEndCol != mStartCol) {
        mLengthOnReference = getIntColumn(mEndCol) - mStartPosition;
      } else {
        mLengthOnReference = 1;
      }
  }

}
