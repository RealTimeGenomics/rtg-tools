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
package com.rtg.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

import com.rtg.mode.SequenceType;

/**
 * FASTQ writing
 */
public class FastqWriter implements SequenceWriter {

  protected final int mLineLength; // Maximum residues per line -- 0 denotes infinite line length
  protected final byte[] mCodeToBytes;

  private final Writer mWriter;
  private final char mDefaultQual;
  private final boolean mRedundantName;

  private byte[] mBuff = new byte[0];
  private char[] mQBuff = new char[0];

  /**
   * Create a FASTQ writer for DNA
   * @param writer the destination
   */
  public FastqWriter(Writer writer) {
    this(writer, 0, (byte) 0);
  }

  /**
   * Create a FASTQ writer for DNA
   * @param writer the destination
   * @param lineLength the maximum line length, 0 means no bound.
   * @param defaultQual default quality value, 0 to 63 scale.
   */
  public FastqWriter(Writer writer, int lineLength, byte defaultQual) {
    this(writer, lineLength, defaultQual, false);
  }
    /**
     * Create a FASTQ writer for DNA
     * @param writer the destination
     * @param lineLength the maximum line length, 0 means no bound.
     * @param defaultQual default quality value, 0 to 63 scale.
     * @param redundantName if true, include the sequence name also in the quality section prefix
     */
  public FastqWriter(Writer writer, int lineLength, byte defaultQual, boolean redundantName) {
    mLineLength = lineLength;
    mWriter = writer;
    mDefaultQual = FastaUtils.rawToAsciiQuality(defaultQual);
    mCodeToBytes = SdfSubseq.getByteMapping(SequenceType.DNA, false);
    mRedundantName = redundantName;
  }

  @Override
  public void write(String name, byte[] data, byte[] quality, int length) throws IOException {
    if (mBuff.length < length) {
      mBuff = new byte[length];
      mQBuff = new char[length];
    }
    for (int i = 0; i < length; ++i) {
      mBuff[i] = mCodeToBytes[data[i]];
      mQBuff[i] = quality == null ? mDefaultQual : FastaUtils.rawToAsciiQuality(quality[i]);
    }

    mWriter.append("@").append(name).append('\n');
    if (mLineLength == 0) {
      mWriter.append(new String(mBuff, 0, length)).append('\n');
      mWriter.append("+");
      if (mRedundantName) {
        mWriter.append(name);
      }
      mWriter.append('\n');
      mWriter.append(new String(mQBuff, 0, length)).append('\n');
    } else {
      for (long k = 0; k < length; k += mLineLength) {
        mWriter.append(new String(mBuff, (int) k, Math.min(mLineLength, length - (int) k))).append('\n');
      }
      mWriter.append("+");
      if (mRedundantName) {
        mWriter.append(name);
      }
      mWriter.append('\n');
      for (long i = 0; i < length; i += mLineLength) {
        mWriter.append(new String(mQBuff, (int) i, Math.min(mLineLength, length - (int) i))).append('\n');
      }
    }
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws IOException {
    try (Closeable ignored = mWriter) {
      // ignored
    }
  }
}

