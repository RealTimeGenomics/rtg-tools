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

import com.rtg.sam.SamUtils;

/**
 * Class for presenting SAM/BAM format files to indexer
 */
class SamPositionReader extends AbstractPositionReader {

  private static final int NUM_COLUMNS = 10;
  private static final int REF_NAME_COLUMN = 2;
  private static final int START_POS_COLUMN = 3;
  private static final int CIGAR_COLUMN = 5;
  private static final int SEQ_COLUMN = 9;

  /**
   * Constructor
   * @param reader source of SAM file
   * @param skip number of lines to skip at start of file
   */
  SamPositionReader(BlockCompressedLineReader reader, int skip) {
    super(reader, NUM_COLUMNS, '@', skip);
  }

  @Override
  protected void setReferenceName() throws IOException {
    mReferenceName = getColumn(REF_NAME_COLUMN);
  }

  @Override
  protected void setStartAndLength() throws IOException {
    mStartPosition = getIntColumn(START_POS_COLUMN) - 1;
    if (mStartPosition < 0) {
      mStartPosition = 0;
    }
    final String cigar = getColumn(CIGAR_COLUMN);
    if ("*".equals(cigar)) {
      // For unmapped reads use length of sequence
      mLengthOnReference = getColumn(SEQ_COLUMN).length();
    } else {
      mLengthOnReference = SamUtils.cigarRefLength(cigar);
    }
  }


}
