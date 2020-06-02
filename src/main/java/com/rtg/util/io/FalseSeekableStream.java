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

import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps a normal {@link InputStream} in a {@link SeekableStream} but does not
 * support seeking. Should not be passed around, instead only use inside a class
 * aware of its limitations.
 */
public class FalseSeekableStream extends SeekableStream {

  private final InputStream mStream;

  /**
   * Constructor
   * @param stream the stream to wrap
   */
  public FalseSeekableStream(InputStream stream) {
    mStream = stream;
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @return nothing
   */
  @Override
  public long getPosition() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @return nothing
   */
  @Override
  public long length() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @param pos ignored
   */
  @Override
  public void seek(long pos) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int read() throws IOException {
    return mStream.read();
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @return nothing
   */
  @Override
  public int available() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
  @Override
  public void close() throws IOException {
    mStream.close();
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @param readlimit ignored
   */
  @Override
  public synchronized void mark(int readlimit) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @return nothing
   */
  @Override
  public boolean markSupported() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int read(byte[] b) throws IOException {
    return mStream.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return mStream.read(b, off, len);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public synchronized void reset() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * throws {@link UnsupportedOperationException}
   * @param n ignored
   * @return nothing
   */
  @Override
  public long skip(long n) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
