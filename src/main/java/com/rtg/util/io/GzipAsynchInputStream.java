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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.rtg.util.gzip.GzipUtils;


/**
 * An input stream that does the GZIP deflation in a separate thread.
 * It also does buffering, before and after the Gzip deflation and
 * within the pipe used to connect the GZIP thread to this thread.
 *
 * This class is final, because the helper thread starts in the
 * constructor.
 *
 */
public final class GzipAsynchInputStream extends AsynchInputStream {

  /** Modern disks have about 64 Kb blocks, so make it that size? */
  public static final int DEFAULT_GZIP_BUFFER_SIZE = 64 * 1024;

  // This is so we can do some parameter checking before calling super()
  private static InputStream makeInputStream(File file, int gzipSize) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null");
    }
    final FileInputStream is = new FileInputStream(file);
    try {
      return GzipUtils.createGzipInputStream(is, gzipSize);
    } catch (IOException e) {
      is.close();
      throw e;
    }
  }

  /**
   * Create an asynchronous GZIP input stream to read the given file.
   *
   * @param file the input file
   * @param pipeSize the size of the buffer between the threads.  At least 1 Kb.
   * @param gzipSize the buffer size of the decompression object.
   * @throws IOException on IO error.
   */
  public GzipAsynchInputStream(File file, int pipeSize, int gzipSize) throws IOException {
    super(makeInputStream(file, gzipSize), pipeSize, gzipSize);
  }

  /**
   * Create an asynchronous GZIP input stream with <code>DEFAULT_GZIP_BUFFER_SIZE</code>.
   *
   * @param file the input file.
   * @throws IOException on IO error.
   */
  public GzipAsynchInputStream(File file) throws IOException {
    this(file, DEFAULT_PIPE_SIZE, DEFAULT_GZIP_BUFFER_SIZE);
  }

  /**
   * Create an asynchronous GZIP input stream with <code>DEFAULT_GZIP_BUFFER_SIZE</code>.
   * @param is the input stream.
   * @throws IOException on IO error.
   */
  public GzipAsynchInputStream(InputStream is) throws IOException {
    super(GzipUtils.createGzipInputStream(is));
  }

}

