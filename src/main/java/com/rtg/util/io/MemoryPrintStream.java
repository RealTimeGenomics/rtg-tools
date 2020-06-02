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
package com.rtg.util.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

/**
 * For all those times you need to pass something to a print stream and then
 * dump the contents to a string
 */
public class MemoryPrintStream implements Closeable {

  private final ByteArrayOutputStream mBytes;
  private final PrintStream mPrint;
  private final LineWriter mWriter;

  /**
   * Construct an empty one
   */
  public MemoryPrintStream() {
    mBytes = new ByteArrayOutputStream();
    mPrint = new PrintStream(mBytes);
    mWriter = new LineWriter(new OutputStreamWriter(mBytes));
  }

  /**
   * Close the print stream.
   */
  @Override
  public void close() {
    mPrint.close();
  }

  /**
   * Retrieve the underlying ByteArrayOutputStream
   * @return the raw stream
   */
  public ByteArrayOutputStream outputStream() {
    return mBytes;
  }

  /**
   * Retrieve the PrintStream
   * @return the stream
   */
  public PrintStream printStream() {
    return mPrint;
  }

  /**
   * Retrieve the LineWriter
   * @return the stream
   */
  public LineWriter lineWriter() {
    return mWriter;
  }

  /**
   * Flushes first
   * @see java.io.ByteArrayOutputStream#toString()
   * @return string representation of contents of output
   */
  @Override
  public String toString() {
    mPrint.flush();
    flushWriter();
    return mBytes.toString();
  }

  private void flushWriter() {
    try {
      mWriter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e); // Can't happen
    }
  }

  /**
   * @return contents of stream
   */
  public byte[] toByteArray() {
    mPrint.flush();
    flushWriter();
    return mBytes.toByteArray();
  }

  /**
   * Reset the underlying ByteArrayOutputStream
   * @see java.io.ByteArrayOutputStream#toString()
   */
  public void reset() {
    mBytes.reset();
  }
}
