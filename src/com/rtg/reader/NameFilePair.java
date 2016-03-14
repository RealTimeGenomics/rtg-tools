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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.io.BufferedOutputStreamFix;
import com.rtg.util.io.FileUtils;


/**
 * Tracks pairs of data and pointers
 */
class NameFilePair {
  private final OutputStream mNameData;
  private final DataOutputStream mPointers;
  private final long mLimit;
  private int mDataSize;

  /**
   * Creates an instance
   * @param names Destination file for names
   * @param pointers Destination file for pointers
   * @param limit Max file size in bytes (currently ignored)
   * @throws IOException When IO errors occur
   */
  NameFilePair(final File names, final File pointers, final long limit) throws IOException {
    mNameData = new BufferedOutputStreamFix(new FileOutputStream(names), FileUtils.BUFFERED_STREAM_SIZE);
    mPointers = new DataOutputStream(new BufferedOutputStreamFix(new FileOutputStream(pointers), FileUtils.BUFFERED_STREAM_SIZE));
    mLimit = limit;
  }

  /**
   * Checks whether writing name will break file size limit
   * @param length length of the name
   * @return true
   */
  public boolean canWriteName(final int length) {
    return mDataSize + length + 1 <= mLimit
      && mPointers.size() + 4 <= mLimit;
  }

  /**
   * Writes a name to files.
   * @param name Name to write
   * @throws IOException When an IO error occurs
   */
  public void writeName(final String name) throws IOException {
    mPointers.writeInt(mDataSize);
    final byte[] rawChars = name.getBytes();
    mNameData.write(rawChars);
    mNameData.write(0); //it worked for c (kinda)
    mDataSize += rawChars.length + 1;
  }

  public String forceWriteName(final String name) throws IOException {
    final long remaining = mLimit - mDataSize;
    if (remaining > name.length() + 1) {
      writeName(name);
      return name;
    } else {
      final String truncated = name.substring(0, (int) (remaining - 1));
      writeName(truncated);
      return truncated;
    }
  }

  /**
   * Closes internal OutputStreams
   * @throws IOException When IO errors occur
   */
  public void close() throws IOException {
    mNameData.close();
    mPointers.close();
  }

  /**
   * @return number of ASCII values written including null characters
   */
  public long valuesWritten() {
    return mDataSize;
  }
}
