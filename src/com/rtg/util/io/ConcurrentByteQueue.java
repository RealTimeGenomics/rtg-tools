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
import java.io.OutputStream;

import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;

/**
 * A simple circular buffer of bytes, to be shared between a concurrent reader and writer.
 *
 */
public class ConcurrentByteQueue implements Integrity {

  private final int[] mReadHist = new int[10];
  private final int[] mWriteHist = new int[10];


  private static final int TIMEOUT = 100000;

  /**
   * Circular buffer.
   * Valid bytes are <code>mBuf[mStart .. mStart + mSize - 1]</code> but modulo size.
   */
  private final byte[] mBuf;
  private int mStart, mSize;

  /** Set by the writer to indicate that no more writes will be done. */
  private boolean mClosed;

  ConcurrentByteQueue(int bufSize) {
    assert bufSize > 0;
    mBuf = new byte[bufSize];
    mStart = 0;
    mSize = 0;
    mClosed = false;
    assert globalIntegrity();
  }

  /**
   * @return the maximum capacity of this queue.
   */
  public synchronized int maxSize() {
    return mBuf.length;
  }

  /**
   * @return the number of bytes currently available for reading.
   */
  public synchronized int available() {
    return mSize;
  }

  /**
   * The write can call this to close the buffer.
   * It must not do any more writes after calling close.
   */
  public synchronized void close() {
//    Diagnostic.developerLog("=====" + StringUtils.LS + "CBQ Writer full notifies: " + mWriteFullNotify + StringUtils.LS
//            + "CBQ Writer was empty notifies: " + mWriteWasEmptyNotify + StringUtils.LS
//            + "CBQ Reader was full notifies: " + mReadWasFullNotify);
    mClosed = true;
    notifyAll();
//    for (int i = 0; i < mReadHist.length; i++) {
//      Diagnostic.developerLog("CBQ hist: " + (i * 1000) + " read " + mReadHist[i] + " write " + mWriteHist[i]);
//    }
  }

  /**
   * Add all the bytes in <code>buf[start .. start+count-1]</code> into the queue.
   *
   * Pre: <code>close()</code> has not been called yet.
   *
   * @param buf non-null array of bytes.
   * @param off the first byte to copy.
   * @param len how many bytes to copy.
   * @throws InterruptedException if one end closed early.
   */
  public synchronized void write(byte[] buf, int off, int len) throws InterruptedException {

    if (len / 1000 < mWriteHist.length - 1) {
      mWriteHist[len / 1000]++;
    } else {
      mWriteHist[mWriteHist.length - 1]++;
    }

    assert buf != null;
    assert 0 <= off;
    assert 0 <= len;
    assert off + len <= buf.length;
    //assert !mClosed; // This is possible if an exception has happened
    boolean wasEmpty = mSize == 0;
    int from = off;
    int todo = len;
    while (todo > 0 && !mClosed) { // To allow reading thread to kill when exception occurs in underlying writing
      final int freeSpaceStart = (mStart + mSize) % mBuf.length;
      final int freeSize = mBuf.length - mSize;
      final int spaceUntilEnd = mBuf.length - freeSpaceStart;
      final int copied = Math.min(todo, Math.min(freeSize, spaceUntilEnd));
      if (copied > 0) {
        System.arraycopy(buf, from, mBuf, freeSpaceStart, copied);
        mSize += copied;
        todo -= copied;
        from += copied;
      } else {
        // wait until the reader has cleared some space
        notifyAll();
        wait(TIMEOUT);
        wasEmpty = mSize == 0;
      }
      assert globalIntegrity();
    }
    if (wasEmpty) {
      // readers may be waiting
      notifyAll();
    }
  }

  /**
   * Copy some bytes out of the queue into <code>buf[start .. start+count-1]</code>.
   * This may return less bytes that you asked for, but will never return 0
   * (unless you ask for 0 bytes!).  It returns -1 on end of file or if close has been called.
   *
   * @param buf the destination buffer
   * @param start the position to start copying to.
   * @param count how many bytes you would like to get.
   * @return the actual number of bytes you got, or -1 on end of file or if close has been called.
   * @throws InterruptedException if one ends closes early.
   */
  public synchronized int read(byte[] buf, int start, int count) throws InterruptedException {
    while (mSize == 0 && !mClosed) {
      wait(TIMEOUT);
    }
    if (mSize == 0 && mClosed) {
      return -1;
    } else {
      final boolean wasFull = mSize == mBuf.length;
      final int spaceUntilEnd = mBuf.length - mStart;
      final int copied = Math.min(count, Math.min(mSize, spaceUntilEnd));
      System.arraycopy(mBuf, mStart, buf, start, copied);
      mStart += copied;
      mSize -= copied;
      if (mStart == mBuf.length) {
        mStart = 0;
      }
      if (wasFull) {
        notifyAll(); // let the writer continue
      }
      assert globalIntegrity();
      if (copied / 1000 < mReadHist.length - 1) {
        mReadHist[copied / 1000]++;
      } else {
        mReadHist[mReadHist.length - 1]++;
      }
      return copied;
    }
  }

  /**
   * Write anything currently in the queue to an output stream
   * @param out output stream to write to.
   * @return number of bytes written -1 in the event of completion.
   * @throws InterruptedException if interrupted
   * @throws IOException when the write fails
   */
  public synchronized int writeToStream(OutputStream out) throws InterruptedException, IOException {
    while (mSize == 0 && !mClosed) {
      wait(TIMEOUT);
    }
    if (mSize == 0 && mClosed) {
      return -1;
    } else {
      final boolean wasFull = mSize == mBuf.length;
      final int copied = Math.min(mSize, mBuf.length - mStart);
      out.write(mBuf, mStart, copied);
      mStart += copied;
      mSize -= copied;
      if (mStart == mBuf.length) {
        mStart = 0;
      }
      if (wasFull) {
        notifyAll(); // let the writer continue
      }
      assert globalIntegrity();
      if (copied / 1000 < mReadHist.length - 1) {
        mReadHist[copied / 1000]++;
      } else {
        mReadHist[mReadHist.length - 1]++;
      }
      return copied;
    }

  }

  @Override
  public synchronized boolean globalIntegrity() {
    integrity();
    return true;
  }

  @Override
  public synchronized boolean integrity() {
    Exam.assertTrue(0 <= mStart);
    Exam.assertTrue(mStart < mBuf.length);
    Exam.assertTrue(0 <= mSize);
    Exam.assertTrue(mSize <= mBuf.length);
    return true;
  }
}
