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
import java.io.RandomAccessFile;

/**
 * SeekableStream based on random access file
 */
public class RandomAccessFileStream extends SeekableStream {

  private final RandomAccessFile mRaf;

  /**
   * Constructor
   * @param raf base random access file
   */
  public RandomAccessFileStream(RandomAccessFile raf) {
    mRaf = raf;
  }

  @Override
  public long getPosition() throws IOException {
    return mRaf.getFilePointer();
  }

  @Override
  public long length() throws IOException {
    return mRaf.length();
  }

  @Override
  public void seek(long pos) throws IOException {
    mRaf.seek(pos);
  }

  @Override
  public int read() throws IOException {
    return mRaf.read();
  }

  @Override
  public int available() throws IOException {
    final long ret = mRaf.length() - mRaf.getFilePointer();
    if (ret > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) ret;
  }
  @Override
  public void close() throws IOException {
    mRaf.close();
  }

  @Override
  public synchronized void mark(int readlimit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return mRaf.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return mRaf.read(b, off, len);
  }

  @Override
  public synchronized void reset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long skip(long n) throws IOException {
    final long skip = n > available() ? (long) available() : n;
    mRaf.seek(mRaf.getFilePointer() + skip);
    return skip;
  }

}
