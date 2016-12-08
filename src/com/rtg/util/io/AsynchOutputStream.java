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

import htsjdk.samtools.util.RuntimeIOException;

/**
 * An output stream that does the writing in a separate thread.  It
 * also does buffering, during the output and also within the pipe
 * used to connect the output thread to this thread.
 *
 * WARNING: Because the helper thread starts in the constructor,
 * subclasses can not contain any state themselves (as these will not
 * have been initialized before the thread starts)
 *
 */
public class AsynchOutputStream extends OutputStream {

  /**
   * The size of the buffer/pipe between the decompression thread and the
   * main thread.  Benchmarking showed it is best to make this fairly
   * large, probably because it allows the decompression thread to get ahead.
   */
  public static final int DEFAULT_PIPE_SIZE = 1024 * 1024; // 1m buffer

  /** Modern disks have about 64 Kb blocks, so make it that size? */
  public static final int DEFAULT_OUTPUT_BUFFER_SIZE = 65536;

  /** This is package-level protection, just for testing purposes. */
  final ConcurrentByteQueue mQueue;

  private final AsynchOutput mAsynchOutput;

  /** This is package-level protection, just for testing purposes. */
  final Thread mThread;


  /**
   * Create an asynchronous output stream with a pipe size of
   * <code>DEFAULT_PIPE_SIZE</code> and a output buffer size of
   * <code>DEFAULT_OUTPUT_BUFFER_SIZE</code>.
   *
   * @param stream the output stream.
   */
  public AsynchOutputStream(OutputStream stream) {
    this(stream, DEFAULT_PIPE_SIZE);
  }

  /**
   * Create an asynchronous output stream to write to the given stream.
   *  @param stream the output stream
   * @param pipeSize the size of the buffer between the threads.  At least 1 Kb.
   */
  public AsynchOutputStream(OutputStream stream, int pipeSize) {
    //Diagnostic.developerLog("new AsynchOutputStream(" + pipeSize + ", " + bufferSize + ")");
    assert pipeSize >= 1024;
    mQueue = new ConcurrentByteQueue(pipeSize);
    if (stream == null) {
      throw new IllegalArgumentException("Stream cannot be null");
    }
    mAsynchOutput = new AsynchOutput(stream, mQueue);
    mThread = new Thread(mAsynchOutput, "AsynchOutputStream");
    mThread.setDaemon(true);
    mThread.start();
  }

  private final byte[] mBuffer = new byte[FileUtils.BUFFERED_STREAM_SIZE];
  private int mBufferCount = 0;

  /**
   * @return maximum size of the internal pipe-like buffer.
   */
  public int getMaxSize() {
    return mQueue.maxSize();
  }

  /**
   * This also IO errors that happened in the GZIP thread.
   */
  @Override
  public void close() throws IOException {
    if (mBufferCount > 0) {
      try {
        mQueue.write(mBuffer, 0, mBufferCount);
      } catch (InterruptedException e) {
        throw new IOException("GzipAsynchOutputStream interrupted during write/3");
      }
      mBufferCount = 0;
    }
    mQueue.close();
    try {
      mThread.join();
    } catch (InterruptedException e) {
      throw new IOException("AsynchOutputStream interrupted during close");
    } finally {
      super.close();
    }
    checkException();
  }

  private void checkException() throws IOException {
    if (mAsynchOutput.mException != null) {
      try {
        throw mAsynchOutput.mException;
      } finally {
        mAsynchOutput.mException = null;
      }
    }
  }

  /**
   * The flush method of GzipAsynchOutputStream blocks until the internal
   * pipe buffer to the compression subprocess is empty, but does not
   * guarantee that all bytes have been flushed through the compression
   * routines.  So it is really a partial flush.  Call <code>close()</code>
   * to make sure that all bytes are written out and the subprocess has
   * finished.
   */
  @Override
  public void flush() throws IOException {
    if (mBufferCount > 0) {
      try {
        mQueue.write(mBuffer, 0, mBufferCount);
      } catch (InterruptedException e) {
        throw new IOException("GzipAsynchOutputStream interrupted during write/3");
      }
      mBufferCount = 0;
    }
    // TODO, also wait for other thread to flush buffers.
    while (mQueue.available() > 0 && mAsynchOutput.mException == null) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new IOException("GzipAsynchOutputStream interrupted during flush");
      }
    }
    checkException();
  }
  @Override
  public void write(byte[] buf, int off, int len) throws IOException {
    if (mBufferCount + len >= mBuffer.length) {
      checkException();
      // validity checking of buf, off, len is done in mQueue.write.
      try {
        mQueue.write(mBuffer, 0, mBufferCount);
        mQueue.write(buf, off, len);
      } catch (InterruptedException e) {
        throw new IOException("GzipAsynchOutputStream interrupted during write/3");
      }
      mBufferCount = 0;
    } else {
      System.arraycopy(buf, off, mBuffer, mBufferCount, len);
      mBufferCount += len;
    }
  }
  @Override
  public void write(int b) throws IOException {
    // throw new UnsupportedOperationException();
    if (mBufferCount == mBuffer.length) {
      checkException();
      mBufferCount = 0;
      try {
        mQueue.write(mBuffer, 0, mBuffer.length);
      } catch (InterruptedException e) {
        throw new IOException("GzipAsynchOutputStream interrupted during write/1");
      }
    }
    mBuffer[mBufferCount] = (byte) b;
    ++mBufferCount;
  }

  /**
   * This class does the writing and compression of the output file.
   */
  private static class AsynchOutput implements Runnable {

    private final OutputStream mOutput;

    /** the queue/pipe used to send bytes to the parent process */
    private final ConcurrentByteQueue mQueue;

    volatile IOException mException = null; // tell the parent about an error.

    AsynchOutput(OutputStream stream, ConcurrentByteQueue queue) {
      mOutput = stream;
      mQueue = queue;
    }

    @Override
    public void run() {
      try {
        for (;;) {
          final int size = mQueue.writeToStream(mOutput);
          if (size <= 0) {
            break;
          }
        }
      } catch (IOException e) {
        mException = e; // tell the other end of the pipe about this error.
      } catch (RuntimeIOException e) {
        final Throwable cause = e.getCause();
        if (cause != null && e.getCause() instanceof IOException) {
          mException = (IOException) e.getCause();
        } else {
          mException = new IOException(e.getMessage(), e);
        }
      } catch (InterruptedException e) {
        mException = new IOException("GzipAsynchOutputStream interrupted");
      } finally {
        try {
          mOutput.close();
        } catch (IOException e2) {
          if (mException == null) {
            mException = e2;
          }
        } catch (RuntimeIOException e2) {
          if (mException == null) {
            final Throwable cause = e2.getCause();
            if (cause != null && e2.getCause() instanceof IOException) {
              mException = (IOException) e2.getCause();
            } else {
              mException = new IOException(e2.getMessage(), e2);
            }
          }
        }
        mQueue.close(); // In case of failure in mOutput.write
      }
    }
  }
}

