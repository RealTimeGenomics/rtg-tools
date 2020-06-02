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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.rtg.reader.FastqUtils;
import com.rtg.reader.FastqWriter;
import com.rtg.reader.SdfId;
import com.rtg.reader.SequenceWriter;
import com.rtg.util.io.BaseFile;
import com.rtg.util.io.FileUtils;

/**
 * FASTQ style read simulator output
 */
public class FastqReadWriter implements ReadWriter {

  private final BaseFile mBaseFile;
  private SequenceWriter mAppend;
  private SequenceWriter mAppendLeft;
  private SequenceWriter mAppendRight;
  private int mTotal = 0;
  private boolean mExpectLeft = true;

  /**
   * Constructor
   * @param append destination for output
   * @param paired true if being used for paired end output
   */
  FastqReadWriter(Writer append, boolean paired) {
    mBaseFile = null;
    if (paired) {
      mAppend = null;
      mAppendLeft = new FastqWriter(append, 0, (byte) 0);
      mAppendRight = mAppendLeft;
    } else {
      mAppend = new FastqWriter(append, 0, (byte) 0);
      mAppendLeft = null;
      mAppendRight = null;
    }
  }

  /**
   * Constructor for use with file-based output.
   * @param fastqBaseFileName base name of FASTQ file to output to
   */
  public FastqReadWriter(File fastqBaseFileName) {
    mBaseFile = FastqUtils.baseFile(fastqBaseFileName, true);
  }

  private void initSingleEnd() throws IOException {
    mAppend = getFastqWriter("");
    mAppendLeft = null;
    mAppendRight = null;
  }

  private void initPairedEnd() throws IOException {
    mAppend = null;
    if (FileUtils.isStdio(mBaseFile.getBaseFile())) {
      mAppendLeft = getFastqWriter("");
      mAppendRight = mAppendLeft;
    } else {
      mAppendLeft = getFastqWriter("_1");
      mAppendRight = getFastqWriter("_2");
    }
  }

  private FastqWriter getFastqWriter(String suffix) throws IOException {
    return new FastqWriter(new OutputStreamWriter(FileUtils.createOutputStream(mBaseFile, suffix)), 0, (byte) 0);
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
    if (!mExpectLeft || mAppend != null) {
      throw new IllegalStateException();
    }
    if (mAppendLeft == null) {
      initPairedEnd();
    }
    mAppendLeft.write(mTotal + " " + name + " 1", data, qual, length);
    mExpectLeft = !mExpectLeft;
  }

  @Override
  public void writeRightRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (mExpectLeft || mAppend != null) {
      throw new IllegalStateException();
    }
    if (mAppendLeft == null) {
      initPairedEnd();
    }
    mAppendRight.write(mTotal + " " + name + " 2", data, qual, length);
    mExpectLeft = !mExpectLeft;
    ++mTotal;
  }

  @Override
  public void writeRead(String name, byte[] data, byte[] qual, int length) throws IOException {
    if (mAppendLeft != null || mAppendRight != null) {
      throw new IllegalStateException("Cannot mix single and paired end writing");
    }
    if (mAppend == null) {
      initSingleEnd();
    }
    mAppend.write(mTotal + " " + name, data, qual, length);
    ++mTotal;
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (Closeable ignored = mAppendLeft; Closeable ignored2 = mAppendRight; Closeable ignored3 = mAppend) {
      // we want the sexy closing side effects
    }
  }


  @Override
  public int readsWritten() {
    return mTotal;
  }

}
