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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.io.BufferedOutputStreamFix;
import com.rtg.util.io.FileUtils;

/**
 * A rolling index.
 *
 */
@TestClass(value = {"com.rtg.reader.SequencesWriterTest"})
class RollingIndex {
  DataOutputStream mOutput;
  private long mCurrent;
  private long mDataSize;
  private final File mFile;

  RollingIndex(final File file) {
    mCurrent = 0;
    mFile = file;
  }

  private void init() throws FileNotFoundException {
    mOutput = new DataOutputStream(new BufferedOutputStreamFix(new FileOutputStream(mFile), FileUtils.BUFFERED_STREAM_SIZE));
  }

  public void incrementCount() {
    ++mCurrent;
  }

  public void incrementSize(long amount) {
    mDataSize += amount;
  }

  /**
   * Writes the count
   * @throws IOException if an I/O error occurs.
   */
  public void writeEntry() throws IOException {
    if (mOutput == null) {
      init();
    }
    mOutput.writeLong(mCurrent);
    mCurrent = 0;
    mOutput.writeLong(mDataSize);
    mDataSize = 0;
  }

  /**
   * Closes the output stream
   * @throws IOException if an I/O error occurs.
   */
  public void close() throws IOException {
    if (mOutput != null) {
      mOutput.close();
    }
  }
}
