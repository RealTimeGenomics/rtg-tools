/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.simulation.reads;

import java.io.IOException;
import java.io.Writer;

import com.rtg.reader.FastaWriter;
import com.rtg.reader.SdfId;

/**
 * FASTA style read simulator output
 */
public class FastaReadWriter implements ReadWriter {

  private final FastaWriter mAppend;
  private int mTotal = 0;
  private boolean mExpectLeft = true;

  /**
   * Constructor
   * @param append destination for output
   */
  public FastaReadWriter(Writer append) {
    mAppend = new FastaWriter(append, 0);
  }

  @Override
  public void identifyTemplateSet(SdfId... templateIds) {
    // Ignored
  }

  @Override
  public void identifyOriginalReference(SdfId referenceId) {
    // Ignored
  }

  @Override
  public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    writeSequence(mTotal + " " + name, data, length);
    ++mTotal;
  }

  @Override
  public void writeLeftRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (!mExpectLeft) {
      throw new IllegalStateException();
    }
    writeSequence(mTotal + " " + name + "/Left", data, length);
    mExpectLeft = !mExpectLeft;
  }

  @Override
  public void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (mExpectLeft) {
      throw new IllegalStateException();
    }
    writeSequence(mTotal + " " + name + "/Right", data, length);
    mExpectLeft = !mExpectLeft;
    ++mTotal;
  }

  private void writeSequence(String name, byte[] data, int length) throws IOException {
    mAppend.write(name, data, null, length);
  }

  @Override
  public void close() {
  }


  @Override
  public int readsWritten() {
    return mTotal;
  }

}
