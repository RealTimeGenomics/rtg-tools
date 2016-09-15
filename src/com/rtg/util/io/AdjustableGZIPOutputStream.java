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
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import com.rtg.launcher.globals.GlobalFlags;
import com.rtg.launcher.globals.ToolsGlobalFlags;

/**
 * An override of the standard <code>GZIPOutputStream</code> so that
 * we can get access to the deflation compression level.
 *
 *
 */
public class AdjustableGZIPOutputStream extends GZIPOutputStream {

  /** Modern disks have about 64 Kb blocks, so make it that size? */
  public static final int DEFAULT_GZIP_BUFFER_SIZE = 64 * 1024;

  /** Benchmarking shows that 2 gives best CPU to compression tradeoff */
  public static final int DEFAULT_GZIP_LEVEL = GlobalFlags.getIntegerValue(ToolsGlobalFlags.GZIP_LEVEL);


  /**
   * Creates a GZIP output stream with a given compression level.
   * The fastest level seems to be 2 or 3.
   * The compression gets significantly better at 6, but is almost
   * twice as slow as the fastest.
   * The default compression for the standard GZIPOutputStream is -1,
   * which seems to correspond to about level 6.
   *
   * @param out the stream to send the compressed data to.
   * @param gzipSize the size of the GZIP buffers.
   * @param level -1 or 0 to 9 where 0 means no compression and 9 is maximum.
   * @throws IOException on IO error.
   */
  public AdjustableGZIPOutputStream(OutputStream out, int gzipSize, int level) throws IOException {
    super(out, gzipSize);
    //Diagnostic.developerLog("new AdjustableGZIPOutputStream(" + gzipSize + ", " + level + ")");
    assert -1 <= level && level <= 9;
    def = new Deflater(level, true);
  }

  /**
   * Creates a GZIP output stream with default compression level and buffer size.
   *
   * @param out the stream to send the compressed data to.
   * @throws IOException on IO error.
   */
  public AdjustableGZIPOutputStream(OutputStream out) throws IOException {
    this(out, DEFAULT_GZIP_BUFFER_SIZE, DEFAULT_GZIP_LEVEL);
  }
}
