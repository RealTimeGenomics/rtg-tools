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
package com.rtg.util;

import static com.rtg.util.StringUtils.FS;
import static com.rtg.util.StringUtils.LS;

import java.io.IOException;
import java.io.OutputStream;

/**
 * byte and byte[] utilities.
 * Intended to help people writing directly to <code>OutputStream</code> s
 *
 */
public final class ByteUtils {

  /** System dependent line separator. */
  private static final byte[] LS_BYTES = LS.getBytes();
  /**  System dependent file separator. */
  public static final byte FS_BYTE = FS.getBytes()[0];
  /** Tab. */
  public static final byte TAB_BYTE = (byte) '\t';
  /** System independent newline separator */
  private static final byte[] NEWLINE_BYTES = "\n".getBytes();

  /**
   * Write an end of line to out.
   * @param out where to put the output.
   * @throws IOException from out.
   */
  public static void writeLn(final OutputStream out) throws IOException {
    out.write(LS_BYTES);
  }

  /**
   * Write a newline (<code>/n</code>) to out
   * @param out where to write to
   * @throws IOException from out
   */
  public static void writeNewline(final OutputStream out) throws IOException {
    out.write(NEWLINE_BYTES);
  }

  private ByteUtils() { }

}
