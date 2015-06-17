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
 * Position reader for <code>VCF</code> files, used for TABIX index support
 */
class VcfPositionReader extends AbstractPositionReader {

  private static final int NUM_COLUMNS = 4;
  private static final int REF_NAME_COLUMN = 0;
  private static final int START_POS_COLUMN = 1;
  private static final int REF_COLUMN = 3;

  /**
   * Constructor
   * @param reader source of SAM file
   * @param skip number of lines to skip at beginning of file
   * @throws IOException if an IO error occurs.
   */
  VcfPositionReader(BlockCompressedLineReader reader, int skip) throws IOException {
    super(reader, NUM_COLUMNS, '#', skip);
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
    final String ref = getColumn(REF_COLUMN);

    mLengthOnReference = ref.length();
  }

}
