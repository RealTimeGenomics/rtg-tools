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

import com.reeltwo.jumble.annotations.TestClass;

/**
 * Class to get around the bizarre lack of reasonable interfaces on <code>java.io.RandomAccessFile</code>
 */
@TestClass("com.rtg.util.io.RandomAccessFileStreamTest")
public abstract class SeekableStream extends InputStream {

  /**
   * Sets the position offset, measured from the beginning of this
   * stream, at which the next read or write occurs.  The offset may be
   * set beyond the end of the stream. Setting the offset beyond the end
   * of the file does not change the underlying structure.  The length will
   * change only by writing after the offset has been set beyond the end
   * of the stream.
   *
   * @param      pos   the offset position, measured in bytes from the
   *                   beginning of the stream, at which to set the position.
   * @exception  IOException  if <code>pos</code> is less than
   *                          <code>0</code> or if an I/O error occurs.
   */
  public abstract void seek(long pos) throws IOException;

  /**
   * Returns the current offset in this stream.
   *
   * @return     the offset from the beginning of the stream, in bytes,
   *             at which the next read or write occurs.
   * @exception  IOException  if an I/O error occurs.
   */
  public abstract long getPosition() throws IOException;

  /**
   * Returns the length of this stream.
   *
   * @return     the length of this stream, measured in bytes.
   * @exception  IOException  if an I/O error occurs.
   */
  public abstract long length() throws IOException;
  @Override
  public void close() throws IOException {
    super.close();
  }
}
