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
package com.rtg.tabix;

import java.io.IOException;
import java.util.Arrays;

import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.samtools.util.LineReader;
import htsjdk.samtools.util.RuntimeIOException;

/**
 * This class relies on implementation details of {@link BlockCompressedInputStream} namely
 * that {@link BlockCompressedInputStream#available()} returns the number of bytes remaining
 * in the current block.
 */
public class BlockCompressedLineReader implements LineReader {

  private final BlockCompressedInputStream mStream;

  private byte[] mBuffer = new byte[BlockCompressedStreamConstants.MAX_COMPRESSED_BLOCK_SIZE * 2];
  private byte[] mLineBuffer = new byte[1024];
  private int mBufferUsed;
  private long mBufferFilePointer;
  private int mPos;
  private int mLineBufferUsed;
  private long mLineFilePointer;
  private long mFilePointer;
  private int mLineNumber;
  private boolean mInit;

  /**
   * @param stream create reader from given stream
   */
  public BlockCompressedLineReader(BlockCompressedInputStream stream) {
    mLineNumber = 0;
    mStream = stream;
    mLineFilePointer = 0;
    mFilePointer = 0;
    mPos = 0;
    mBufferUsed = 0;
    mLineBufferUsed = 0;
    mInit = false;
  }

  @Override
  public void close() {
    try {
      mStream.close();
    } catch (IOException e) {
      throw new RuntimeIOException(e.getMessage(), e);
    }
  }

  private void resizeLineBuffer() {
    final long newSize = (long) mLineBuffer.length * 3L / 2L;
    if (newSize > Integer.MAX_VALUE) {
      throw new ArrayIndexOutOfBoundsException("Cannot create buffer big enough for line. Lines must be less than: " + Integer.MAX_VALUE + " long");
    }
    mLineBuffer = Arrays.copyOf(mLineBuffer, (int) newSize);
  }
  private void resizeBuffer() {
    final long newSize = (long) mBuffer.length * 3L / 2L;
    if (newSize > Integer.MAX_VALUE) {
      throw new ArrayIndexOutOfBoundsException("Cannot create buffer big enough for block. Blocks must be less than: " + Integer.MAX_VALUE + " long");
    }
    mBuffer = Arrays.copyOf(mBuffer, (int) newSize);
  }

  /**
   * @return file pointer for the start of the current line (the one next read by
   * {@link LineReader#readLine()})
   */
  public long getFilePointer() {
    return mFilePointer;
  }

  /**
   * @return file pointer for the start of the previous line (the one just read by
   * {@link LineReader#readLine()})
   */
  public long getLineFilePointer() {
    return mLineFilePointer;
  }

  @Override
  public int getLineNumber() {
    return mLineNumber;
  }

  @Override
  public int peek() {
    if (mPos >= mBufferUsed) {
      try {
        fillBuffer();
      } catch (IOException e) {
        throw new RuntimeIOException(e.getMessage(), e);
      }
    }
    if (mPos < mBufferUsed) {
      return mBuffer[mPos] & 0xff;
    }
    return -1;
  }

  private void fillBuffer() throws IOException {
    final int start;
    if (mPos >= 0) {
      start = mPos;
    } else {
      start = 0;
    }
    final int stash = mBufferUsed - start;
    copyToLineBuffer(start, stash);
    mPos = 0 - mLineBufferUsed;
    mBufferFilePointer = mInit ? mStream.getFilePointer() : 0;
    mInit = true;
    try {
      final int avail = mStream.available();
      while (avail > mBuffer.length) {
        resizeBuffer();
      }
      mBufferUsed = mStream.read(mBuffer, 0, avail);
    } catch (final NullPointerException e) {
      // NPE can come from deep in htsjdk.samtools.util.BlockCompressedInputStream
      // when the input file is truncated.
      throw new IOException("Probable truncation or corruption of input file", e);
    }
  }

  private void copyToLineBuffer(int startPos, int length) {
    while (mLineBufferUsed + length > mLineBuffer.length) {
      resizeLineBuffer();
    }
    System.arraycopy(mBuffer, startPos, mLineBuffer, mLineBufferUsed, length);
    mLineBufferUsed += length;
  }

  @Override
  public String readLine() {
    do {
      int locPos = mPos >= 0 ? mPos : 0;
      while (locPos < mBufferUsed) {
        if (mBuffer[locPos++] == '\n') {
          final int endPos = (locPos > 1 && mBuffer[locPos - 2] == '\r') ? locPos - 2 : locPos - 1;
          final String ret;
          if (mPos < 0) {
            copyToLineBuffer(0, endPos);
            ret = new String(mLineBuffer, 0, mLineBufferUsed);
          } else {
            ret = new String(mBuffer, mPos, endPos - mPos);
          }
          mLineBufferUsed = 0;
          mPos = locPos;
          mLineFilePointer = mFilePointer;
          mFilePointer = mPos == mBufferUsed ? mStream.getFilePointer() : mBufferFilePointer + mPos;
          //return successful
          ++mLineNumber;
          return ret;
        }
      }
      try {
        fillBuffer();
      } catch (IOException e) {
        throw new RuntimeIOException(e.getMessage(), e);
      }
    } while (mBufferUsed > 0);
    return null;
  }

  /**
   * block compressed seek using a file pointer as described in a <code>tabix</code> index.
   * @param filePointer the file pointer value
   * @throws IOException if an IO error occurs
   */
  public void seek(long filePointer) throws IOException {
    mStream.seek(filePointer);
    mBufferFilePointer = filePointer;
    mFilePointer = filePointer;
    mPos = 0;
    mBufferUsed = 0;
    mLineBufferUsed = 0;
    mInit = true;
  }

}
