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

package com.rtg.util.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.rtg.util.Resources;
import com.rtg.util.io.FileUtils;

import htsjdk.samtools.util.BlockCompressedOutputStream;

/**
 */
public final class BgzipFileHelper {

  private BgzipFileHelper() { }

  /**
   * Convenience method for turning a resource into a block compressed GZIP file that can be used as input.
   * @param resource name (classpath) of the resource.
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the string content (same as <code>file</code>).
   * @throws IOException if an error occurs
   */
  public static File resourceToBgzipFile(final String resource, final File file) throws IOException {
    try (InputStream stream = Resources.getResourceAsStream(resource)) {
      return streamToBgzipFile(stream, file);
    }
  }
  /**
   * Writes the contents of the given input <code>stream</code> to the given block compressed
   * gzipped file.
   *
   * @param stream an <code>InputStream</code>
   * @param file a <code>File</code> to write to
   * @return a <code>File</code> containing the contents of the stream
   * @exception IOException if an error occurs.
   * @exception NullPointerException if the stream is null
   */
  public static File streamToBgzipFile(final InputStream stream, final File file) throws IOException {
    if (stream == null) {
      throw new NullPointerException("null stream given");
    }
    if (file == null) {
      throw new NullPointerException("null file given");
    }
    try (OutputStream out = new BlockCompressedOutputStream(file)) {
      final byte[] b = new byte[FileUtils.BUFFER_SIZE];
      int len = stream.read(b);
      while (len > 0) {
        out.write(b, 0, len);
        len = stream.read(b);
      }
    }
    return file;
  }
  /**
   * Create a BGZIP file
   * @param data data to write
   * @param f file to write to
   * @return file written to
   * @throws IOException if an IO error occurs
   */
  public static File bytesToBgzipFile(byte[] data, File f) throws IOException {
    try (BlockCompressedOutputStream out = new BlockCompressedOutputStream(f)) {
      out.write(data);
    }
    return f;
  }
}
