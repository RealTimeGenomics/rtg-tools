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

package com.rtg.bed;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import com.rtg.util.ByteUtils;


/**
 * Writer to write BED records out into a <code>.bed</code> output stream.
 *
 */
public class BedWriter implements Closeable {

  /** The character that indicates that the line is a comment */
  public static final byte COMMENT_CHAR = '#';

  private final OutputStream mOut;

  /**
   * create a new BED writer
   *
   * @param out stream to write to
   */
  public BedWriter(OutputStream out) {
    if (out == null) {
      throw new NullPointerException("output stream cannot be null");
    }
    mOut = out;
  }

  /**
   * Write out a BED record
   *
   * @param record record to write
   * @throws java.io.IOException if error
   */
  public void write(BedRecord record) throws IOException {
    writeLine(record.toString());
  }

  /**
   * Write a line as a comment (will automatically be prepended with comment char)
   * @param line the comment line
   * @throws java.io.IOException if error
   */
  protected void writeComment(String line) throws IOException {
    mOut.write(COMMENT_CHAR);
    writeLine(line);
  }

  /**
   * Write an extra line (e.g. track line) to output stream
   * @param line the line to output
   * @throws java.io.IOException if error
   */
  protected void writeLine(String line) throws IOException {
    mOut.write(line.getBytes());
    ByteUtils.writeLn(mOut);
  }

  @Override
  public void close() throws IOException {
    mOut.close();
  }

}
