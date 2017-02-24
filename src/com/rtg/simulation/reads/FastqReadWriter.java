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

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.rtg.reader.FastqWriter;
import com.rtg.reader.SdfId;
import com.rtg.reader.SequenceWriter;

/**
 * FASTQ style read simulator output
 */
public class FastqReadWriter implements ReadWriter {

  private SequenceWriter mAppend;
  private final SequenceWriter mAppendLeft;
  private final SequenceWriter mAppendRight;
  private int mTotal = 0;
  private boolean mExpectLeft = true;

  /**
   * Constructor
   * @param append destination for output
   */
  public FastqReadWriter(Writer append) {
    mAppend = new FastqWriter(append, 0, (byte) 0);
    mAppendLeft = null;
    mAppendRight = null;
  }

  /**
   * Constructor for use with two appendables (e.g. paired end <code>fastq</code> output)
   * @param fastqBaseFileName base name of a paired end <code>fastq</code> file to output to
   * @throws IOException if an exception occurs when instantiating writers
   */
  public FastqReadWriter(File fastqBaseFileName) throws IOException {
    final String fpath = fastqBaseFileName.getPath();
    final String base = fpath.substring(0, fpath.length() - 3);
    mAppendLeft = new FastqWriter(new FileWriter(base + "_1.fq"), 0, (byte) 0);
    mAppendRight = new FastqWriter(new FileWriter(base + "_2.fq"), 0, (byte) 0);
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
  public void writeLeftRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (!mExpectLeft) {
      throw new IllegalStateException();
    }
    if (mAppendLeft != null) {
      mAppend = mAppendLeft;
    }
    writeSequence(mTotal + " " + name + " 1", data, qual, length);
    mExpectLeft = !mExpectLeft;
  }

  @Override
  public void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (mExpectLeft) {
      throw new IllegalStateException();
    }
    if (mAppendRight != null) {
      mAppend = mAppendRight;
    }
    writeSequence(mTotal + " " + name + " 2", data, qual, length);
    mExpectLeft = !mExpectLeft;
    ++mTotal;
  }

  @Override
  public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    writeSequence(mTotal + " " + name, data, qual, length);
    ++mTotal;
  }

  private void writeSequence(String name, byte[] data, byte[] qual, int length) throws IOException {
    mAppend.write(name, data, qual, length);
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (Closeable ignored = mAppendLeft; Closeable ignored2 = mAppendRight) {
      // we want the sexy closing side effects
    }
  }


  @Override
  public int readsWritten() {
    return mTotal;
  }

}
