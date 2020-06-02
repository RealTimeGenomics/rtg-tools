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
package com.rtg.util.gzip;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import com.rtg.util.io.IOUtils;

/**
 * Contains utility methods for handling creation of <code>GZIP</code> input streams.
 */
public final class GzipUtils {

  private GzipUtils() {

  }

  static final byte[] GZIP_FILE = {
    (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0xff, (byte) 0x06, (byte) 0x00, (byte) 0x42, (byte) 0x43, (byte) 0x02, (byte) 0x00,
    (byte) 0x20, (byte) 0x00, (byte) 0x2b, (byte) 0x49, (byte) 0x2d, (byte) 0x2e, (byte) 0xe1, (byte) 0x02,
    (byte) 0x00, (byte) 0xc6, (byte) 0x35, (byte) 0xb9, (byte) 0x3b, (byte) 0x05, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x06, (byte) 0x00, (byte) 0x42, (byte) 0x43, (byte) 0x02,
    (byte) 0x00, (byte) 0x1b, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1f, (byte) 0x8b, (byte) 0x08,
    (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0x06,
    (byte) 0x00, (byte) 0x42, (byte) 0x43, (byte) 0x02, (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x4b,
    (byte) 0xce, (byte) 0xcf, (byte) 0x4b, (byte) 0x4e, (byte) 0x2c, (byte) 0xe1, (byte) 0x02, (byte) 0x00,
    (byte) 0xb3, (byte) 0x51, (byte) 0x92, (byte) 0x33, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0xff, (byte) 0x06, (byte) 0x00, (byte) 0x42, (byte) 0x43, (byte) 0x02, (byte) 0x00,
    (byte) 0x1b, (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

  static final byte[] EXPECTED_UNZIPPED = {(byte) 0x74, (byte) 0x65,  (byte) 0x73, (byte) 0x74,
    (byte) 0x0a, (byte) 0x63,  (byte) 0x6f, (byte) 0x6e,
    (byte) 0x63, (byte) 0x61,  (byte) 0x74, (byte) 0x0a};

  /** True if we are overriding built in GZIP class */
  private static boolean sOverrideGzip = shouldOverrideGzip(); //System.getProperty(GZIP_OVERRIDE_FLAG) == null ? shouldOverrideGzip() : Boolean.valueOf(System.getProperty(GZIP_OVERRIDE_FLAG));

  public static boolean getOverrideGzip() {
    return sOverrideGzip;
  }

  public static void setOverrideGzip(boolean value) {
    sOverrideGzip = value;
  }

  static boolean shouldOverrideGzip() {
    try {
      final byte[] input = IOUtils.readData(new GZIPInputStream(new BufferedInputStream(new FakeBlockingInputStream(GZIP_FILE), 16)));
      return !Arrays.equals(EXPECTED_UNZIPPED, input);
    } catch (final IOException e) {
      return true;
    }
  }

  /**
   * Creates a <code>GZIPInputStream</code> the exact class it uses depends on the capabilities of the JVM supplied one.
   * <p>
   * The stream will be able to handle multiple members and extra headers.
   *
   * @param in see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream)}
   * @return see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream)}
   * @throws IOException see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream)}
   */
  public static InputStream createGzipInputStream(InputStream in) throws IOException {
    return sOverrideGzip ? new WorkingGzipInputStream(in) : new GZIPInputStream(in);
  }

  /**
   * Creates a <code>GZIPInputStream</code> the exact class it uses depends on the capabilities of the JVM supplied one.
   * <p>
   * The stream will be able to handle multiple members and extra headers.
   *
   * @param in see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream, int)}
   * @param size see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream, int)}
   * @return see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream, int)}
   * @throws IOException see {@link GZIPInputStream#GZIPInputStream(java.io.InputStream, int)}
   */
  public static InputStream createGzipInputStream(InputStream in, int size) throws IOException {
    return sOverrideGzip ? new WorkingGzipInputStream(in, size) : new GZIPInputStream(in, size);
  }


  static class FakeBlockingInputStream extends ByteArrayInputStream {

    FakeBlockingInputStream(byte[] input) {
      super(input);
    }

    @Override
    public synchronized int available() {
      return 0;
    }

  }

}
