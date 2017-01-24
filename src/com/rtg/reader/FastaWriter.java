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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

import com.rtg.mode.SequenceType;
import com.rtg.util.StringUtils;

/**
 * FASTA writing
 */
public class FastaWriter implements SequenceWriter {

  protected final int mLineLength; // Maximum residues per line -- 0 denotes infinite line length
  protected final byte[] mCodeToBytes;

  private final Writer mWriter;

  private byte[] mBuff = new byte[0];

  /**
   * Create a FASTA writer for DNA
   * @param writer the destination
   * @param lineLength the maximum line length, 0 means no bound.
   */
  public FastaWriter(Writer writer, int lineLength) {
    this(writer, lineLength, SdfSubseq.getByteMapping(SequenceType.DNA, false));
  }

  /**
   * Create a FASTA writer for DNA or protein
   * @param writer the destination
   * @param lineLength the maximum line length, 0 means no bound.
   * @param encoding contains the byte to ASCII mapping table
   */
  public FastaWriter(Writer writer, int lineLength, byte[] encoding) {
    mCodeToBytes = encoding;
    mLineLength = lineLength;
    mWriter = writer;
  }

  @Override
  public void write(String name, byte[] data, byte[] quality, int length) throws IOException {
    if (mBuff.length < length) {
      mBuff = new byte[length];
    }
    for (int i = 0; i < length; ++i) {
      mBuff[i] = mCodeToBytes[data[i]];
    }
    mWriter.append(">").append(name).append(StringUtils.LS);
    if (mLineLength == 0) {
      mWriter.append(new String(mBuff, 0, length)).append(StringUtils.LS);
    } else {
      for (long k = 0; k < length; k += mLineLength) {
        mWriter.append(new String(mBuff, (int) k, Math.min(mLineLength, length - (int) k))).append(StringUtils.LS);
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

