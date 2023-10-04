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
package com.rtg.reader;

import java.io.IOException;


/**
 * Utility for reading... reads... into a byte array
 */
public final class ReadHelper {

  private ReadHelper() { }

  /**
   * Get a read.
   *
   * @param r reader
   * @param readId read identifier
   * @return read
   */
  public static byte[] getRead(SequencesReader r, long readId) {
    if (r == null) {
      return null;
    }
    try {
      final byte[] b = new byte[r.length(readId)];
      r.read(readId, b);
      return b;
    } catch (final IOException ex) {
      // This should not occur in MemorySequencesReader
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Get the quality information for a read.
   *
   * @param r reader
   * @param readId read
   * @return quality
   */
  public static byte[] getQual(SequencesReader r, long readId) {
    try {
      if (r == null || !r.hasQualityData()) {
        return null;
      }
      final byte[] b = new byte[r.length(readId)];
      r.readQuality(readId, b);
      return b;
    } catch (final IOException ex) {
      // This should not occur in MemorySequencesReader
      throw new IllegalStateException(ex);
    }
  }

}
